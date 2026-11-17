package com.xingtu.learning.points;

import com.xingtu.learning.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 积分与排行榜
 *
 * 积分流水落库（points_board），累计积分与排名放 Redis ZSet，
 * 避免每次查榜都做聚合并发查询。
 */
@Service
public class PointsService {

    public static final String RANK_KEY = "xt:points:rank";
    private static final String SIGN_KEY_TEMPLATE = "xt:points:sign:%d:%s";

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final int signPoints;

    public PointsService(JdbcTemplate jdbc,
                         StringRedisTemplate redis,
                         @Value("${xingtu.points.sign-points:2}") int signPoints) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.signPoints = signPoints;
    }

    /**
     * 增加积分：先写流水，再累加 ZSet 排行榜
     */
    public void addPoints(Long userId, int points, int type, String description) {
        jdbc.update("INSERT INTO points_board(user_id, points, type, description) VALUES (?,?,?,?)",
                userId, points, type, description);
        redis.opsForZSet().incrementScore(RANK_KEY, String.valueOf(userId), points);
    }

    /**
     * 签到：当天只能签一次，用 Redis SETNX 做幂等
     */
    public Map<String, Object> sign(Long userId) {
        String key = String.format(SIGN_KEY_TEMPLATE, userId,
                LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
        Boolean first = redis.opsForValue().setIfAbsent(key, "1", Duration.ofDays(2));
        if (!Boolean.TRUE.equals(first)) {
            throw new BizException("今天已经签到过了");
        }
        addPoints(userId, signPoints, 1, "每日签到");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("points", signPoints);
        result.put("total", score(userId));
        return result;
    }

    /**
     * 排行榜 TOP N
     */
    public List<Map<String, Object>> rank(int topN) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redis.opsForZSet().reverseRangeWithScores(RANK_KEY, 0, topN - 1L);
        List<Map<String, Object>> list = new ArrayList<>();
        int rank = 1;
        if (tuples != null) {
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("rank", rank++);
                row.put("userId", Long.parseLong(tuple.getValue()));
                row.put("points", tuple.getScore() == null ? 0 : tuple.getScore().intValue());
                list.add(row);
            }
        }
        return list;
    }

    /**
     * 个人累计积分（Redis 优先，缺失时回源数据库）
     */
    public int score(Long userId) {
        Double score = redis.opsForZSet().score(RANK_KEY, String.valueOf(userId));
        if (score != null) {
            return score.intValue();
        }
        Integer sum = jdbc.queryForObject("SELECT IFNULL(SUM(points),0) FROM points_board WHERE user_id = ?",
                Integer.class, userId);
        int total = sum == null ? 0 : sum;
        if (total > 0) {
            redis.opsForZSet().add(RANK_KEY, String.valueOf(userId), total);
        }
        return total;
    }

    /**
     * 积分流水
     */
    public List<Map<String, Object>> history(Long userId) {
        return jdbc.queryForList(
                "SELECT points, type, description, create_time FROM points_board WHERE user_id = ? ORDER BY id DESC LIMIT 20",
                userId);
    }
}
