package com.xingtu.learning.coupon.discount;

import com.xingtu.learning.coupon.domain.Coupon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 折扣策略注册表
 *
 * 用 Map 按券类型查策略，替代 if-else 分支；新增券类型只需在 static 块里注册。
 */
public final class DiscountFactory {

    private static final Map<Integer, Discount> REGISTRY = new HashMap<>();

    static {
        register(new NoThresholdDiscount());
        register(new PriceDiscount());
        register(new PerPriceDiscount());
        register(new RateDiscount());
    }

    private DiscountFactory() {
    }

    private static void register(Discount discount) {
        REGISTRY.put(discount.type(), discount);
    }

    public static Discount get(int type) {
        Discount discount = REGISTRY.get(type);
        if (discount == null) {
            throw new IllegalArgumentException("不支持的优惠券类型: " + type);
        }
        return discount;
    }

    /**
     * 优惠金额兜底：不允许负数，且不超过订单金额
     */
    private static BigDecimal cap(BigDecimal discount, BigDecimal amount) {
        if (discount == null || discount.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return discount.min(amount);
    }

    /**
     * 1. 无门槛立减
     */
    static final class NoThresholdDiscount implements Discount {

        @Override
        public int type() {
            return Coupon.TYPE_NO_THRESHOLD;
        }

        @Override
        public BigDecimal discount(Coupon coupon, BigDecimal amount) {
            return cap(coupon.discountAmount(), amount);
        }
    }

    /**
     * 2. 满减：达到门槛才可用
     */
    static final class PriceDiscount implements Discount {

        @Override
        public int type() {
            return Coupon.TYPE_PRICE_DISCOUNT;
        }

        @Override
        public BigDecimal discount(Coupon coupon, BigDecimal amount) {
            if (amount.compareTo(coupon.thresholdAmount()) < 0) {
                return BigDecimal.ZERO;
            }
            return cap(coupon.discountAmount(), amount);
        }
    }

    /**
     * 3. 每满减：每满 X 元减 Y 元，可叠加多次
     */
    static final class PerPriceDiscount implements Discount {

        @Override
        public int type() {
            return Coupon.TYPE_PER_PRICE_DISCOUNT;
        }

        @Override
        public BigDecimal discount(Coupon coupon, BigDecimal amount) {
            BigDecimal per = coupon.perAmount();
            if (per == null || per.signum() <= 0) {
                return BigDecimal.ZERO;
            }
            BigDecimal times = amount.divide(per, 0, RoundingMode.DOWN);
            return cap(coupon.discountAmount().multiply(times), amount);
        }
    }

    /**
     * 4. 折扣率：discount_rate 存的是折数（9 表示 9 折）
     */
    static final class RateDiscount implements Discount {

        @Override
        public int type() {
            return Coupon.TYPE_RATE_DISCOUNT;
        }

        @Override
        public BigDecimal discount(Coupon coupon, BigDecimal amount) {
            if (coupon.thresholdAmount() != null && amount.compareTo(coupon.thresholdAmount()) < 0) {
                return BigDecimal.ZERO;
            }
            BigDecimal rate = coupon.discountRate();
            if (rate == null || rate.signum() <= 0) {
                return BigDecimal.ZERO;
            }
            BigDecimal off = amount
                    .multiply(BigDecimal.TEN.subtract(rate))
                    .divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
            return cap(off, amount);
        }
    }
}
