package com.xingtu.learning.coupon;

import com.xingtu.learning.badge.BadgeService;
import com.xingtu.learning.common.BizException;
import com.xingtu.learning.coupon.discount.Discount;
import com.xingtu.learning.coupon.discount.DiscountFactory;
import com.xingtu.learning.coupon.domain.Coupon;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 优惠券
 *
 * 领取：Redis + Lua 原子完成「一人一张校验 + 库存扣减」，避免并发超发；
 * 使用：策略模式算单券优惠，再对可用券组合求最优解。
 */
@Service
public class CouponService {

    private static final String STOCK_KEY_TEMPLATE = "xt:coupon:stock:%d";
    private static final String USER_KEY_TEMPLATE = "xt:coupon:users:%d";
    private static final int MAX_COMBINATION_SIZE = 10;

    /** 返回值：1 领取成功；0 已抢光；-1 已领过 */
    private static final DefaultRedisScript<Long> RECEIVE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
                return -1
            end
            local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
            if stock <= 0 then
                return 0
            end
            redis.call('DECR', KEYS[1])
            redis.call('SADD', KEYS[2], ARGV[1])
            return 1
            """, Long.class);

    private static final RowMapper<Coupon> COUPON_MAPPER = (rs, rowNum) -> new Coupon(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getInt("type"),
            rs.getBigDecimal("threshold_amount"),
            rs.getBigDecimal("discount_amount"),
            rs.getBigDecimal("discount_rate"),
            rs.getBigDecimal("per_amount"),
            rs.getInt("total_num"),
            rs.getInt("remain_num"),
            rs.getTimestamp("begin_time").toLocalDateTime(),
            rs.getTimestamp("end_time").toLocalDateTime());

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final BadgeService badgeService;

    public CouponService(JdbcTemplate jdbc, StringRedisTemplate redis, BadgeService badgeService) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.badgeService = badgeService;
    }

    /**
     * 领取优惠券（Lua 保证原子性）
     */
    public Map<String, Object> receive(Long userId, Long couponId) {
        Coupon coupon = load(couponId);
        warmUp(coupon);

        Long result = redis.execute(RECEIVE_SCRIPT,
                List.of(stockKey(couponId), userKey(couponId)), String.valueOf(userId));
        if (result == null) {
            throw new BizException("领取失败，请稍后重试");
        }
        if (result == -1L) {
            throw new BizException("您已经领取过这张券了");
        }
        if (result == 0L) {
            throw new BizException("优惠券已被抢光");
        }

        jdbc.update("INSERT IGNORE INTO user_coupon(user_id, coupon_id) VALUES (?,?)", userId, couponId);
        jdbc.update("UPDATE coupon SET remain_num = remain_num - 1 WHERE id = ? AND remain_num > 0", couponId);

        // 领取满 5 张 → 领券达人徽章（幂等）
        badgeService.checkAndAward(userId, "coupon_master");

        String remain = redis.opsForValue().get(stockKey(couponId));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("couponId", couponId);
        data.put("name", coupon.name());
        data.put("remainNum", remain == null ? 0 : Integer.parseInt(remain));
        data.put("tip", "领取成功，库存扣减与一人一张校验由 Lua 脚本原子完成");
        return data;
    }

    /**
     * 我持有的可用券
     */
    public List<Coupon> available(Long userId) {
        return jdbc.query("SELECT c.* FROM user_coupon uc JOIN coupon c ON uc.coupon_id = c.id " +
                        "WHERE uc.user_id = ? AND uc.status = 0 AND NOW() BETWEEN c.begin_time AND c.end_time",
                COUPON_MAPPER, userId);
    }

    /**
     * 计算最优优惠：对可用券做组合枚举，取优惠最大的一组
     */
    public Map<String, Object> best(Long userId, BigDecimal orderAmount) {
        if (orderAmount == null || orderAmount.signum() <= 0) {
            throw new BizException("订单金额必须大于 0");
        }
        List<Coupon> coupons = available(userId);
        if (coupons.size() > MAX_COMBINATION_SIZE) {
            coupons = coupons.subList(0, MAX_COMBINATION_SIZE);
        }

        BigDecimal bestDiscount = BigDecimal.ZERO;
        List<Coupon> bestCombo = new ArrayList<>();
        int size = coupons.size();
        for (int mask = 1; mask < (1 << size); mask++) {
            BigDecimal total = BigDecimal.ZERO;
            List<Coupon> combo = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                if ((mask & (1 << i)) == 0) {
                    continue;
                }
                Coupon coupon = coupons.get(i);
                Discount discount = DiscountFactory.get(coupon.type());
                total = total.add(discount.discount(coupon, orderAmount));
                combo.add(coupon);
            }
            if (total.compareTo(orderAmount) > 0) {
                total = orderAmount;
            }
            if (total.compareTo(bestDiscount) > 0) {
                bestDiscount = total;
                bestCombo = combo;
            }
        }

        List<Map<String, Object>> comboDetail = new ArrayList<>();
        for (Coupon coupon : bestCombo) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("couponId", coupon.id());
            row.put("name", coupon.name());
            row.put("type", coupon.type());
            row.put("discount", DiscountFactory.get(coupon.type()).discount(coupon, orderAmount));
            comboDetail.add(row);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderAmount", orderAmount);
        data.put("availableCoupons", coupons.size());
        data.put("bestDiscount", bestDiscount);
        data.put("payableAmount", orderAmount.subtract(bestDiscount));
        data.put("bestCombination", comboDetail);
        return data;
    }

    private Coupon load(Long couponId) {
        List<Coupon> list = jdbc.query("SELECT * FROM coupon WHERE id = ?", COUPON_MAPPER, couponId);
        if (list.isEmpty()) {
            throw new BizException("优惠券不存在: " + couponId);
        }
        return list.get(0);
    }

    /**
     * 库存预热：Redis 没有库存键时用数据库剩余量初始化
     */
    private void warmUp(Coupon coupon) {
        String stockKey = stockKey(coupon.id());
        if (Boolean.TRUE.equals(redis.hasKey(stockKey))) {
            return;
        }
        redis.opsForValue().set(stockKey, String.valueOf(Math.max(coupon.remainNum(), 0)));
    }

    private String stockKey(Long couponId) {
        return String.format(STOCK_KEY_TEMPLATE, couponId);
    }

    private String userKey(Long couponId) {
        return String.format(USER_KEY_TEMPLATE, couponId);
    }
}
