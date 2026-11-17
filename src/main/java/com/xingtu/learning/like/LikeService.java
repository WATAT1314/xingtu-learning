package com.xingtu.learning.like;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 点赞
 *
 * 关系与计数都先落 Redis：Set 保证一人一赞，String 做计数，
 * dirty 集合标记「有变化待落库」的业务对象，由定时任务批量同步到数据库。
 */
@Service
public class LikeService {

    private static final String USER_KEY = "xt:like:users:%d:%d";
    private static final String COUNT_KEY = "xt:like:count:%d:%d";
    private static final String DIRTY_KEY = "xt:like:dirty";

    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbc;

    public LikeService(StringRedisTemplate redis, JdbcTemplate jdbc) {
        this.redis = redis;
        this.jdbc = jdbc;
    }

    /**
     * 点赞
     */
    public Map<String, Object> like(Long userId, int bizType, Long bizId) {
        Long added = redis.opsForSet().add(userKey(bizType, bizId), String.valueOf(userId));
        if (added != null && added > 0) {
            warmUp(bizType, bizId);
            redis.opsForValue().increment(countKey(bizType, bizId));
            markDirty(bizType, bizId);
        }
        return state(userId, bizType, bizId);
    }

    /**
     * 取消点赞
     */
    public Map<String, Object> unlike(Long userId, int bizType, Long bizId) {
        Long removed = redis.opsForSet().remove(userKey(bizType, bizId), String.valueOf(userId));
        if (removed != null && removed > 0) {
            warmUp(bizType, bizId);
            Long count = redis.opsForValue().decrement(countKey(bizType, bizId));
            if (count != null && count < 0) {
                redis.opsForValue().set(countKey(bizType, bizId), "0");
            }
            markDirty(bizType, bizId);
        }
        return state(userId, bizType, bizId);
    }

    public Map<String, Object> state(Long userId, int bizType, Long bizId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bizType", bizType);
        result.put("bizId", bizId);
        result.put("count", count(bizType, bizId));
        result.put("liked", liked(userId, bizType, bizId));
        return result;
    }

    public long count(int bizType, Long bizId) {
        warmUp(bizType, bizId);
        String value = redis.opsForValue().get(countKey(bizType, bizId));
        return value == null ? 0 : Long.parseLong(value);
    }

    public boolean liked(Long userId, int bizType, Long bizId) {
        return Boolean.TRUE.equals(redis.opsForSet().isMember(userKey(bizType, bizId), String.valueOf(userId)));
    }

    /**
     * 待同步的业务对象数量（演示观察用）
     */
    public long dirtySize() {
        Long size = redis.opsForSet().size(DIRTY_KEY);
        return size == null ? 0 : size;
    }

    /**
     * 把 Redis 中的点赞关系与计数批量同步到数据库
     */
    public int sync() {
        Set<String> dirty = redis.opsForSet().members(DIRTY_KEY);
        if (dirty == null || dirty.isEmpty()) {
            return 0;
        }
        int processed = 0;
        for (String item : dirty) {
            String[] parts = item.split(":");
            int bizType = Integer.parseInt(parts[0]);
            Long bizId = Long.parseLong(parts[1]);
            long count = count(bizType, bizId);

            jdbc.update("INSERT INTO like_count(biz_type, biz_id, liked_times) VALUES (?,?,?) " +
                            "ON DUPLICATE KEY UPDATE liked_times = VALUES(liked_times)",
                    bizType, bizId, count);

            Set<String> users = redis.opsForSet().members(userKey(bizType, bizId));
            if (users != null) {
                for (String userId : users) {
                    jdbc.update("INSERT IGNORE INTO like_record(user_id, biz_type, biz_id) VALUES (?,?,?)",
                            Long.parseLong(userId), bizType, bizId);
                }
            }
            redis.opsForSet().remove(DIRTY_KEY, item);
            processed++;
        }
        return processed;
    }

    /**
     * 计数缓存缺失时用数据库值预热，避免从 0 开始累加
     */
    private void warmUp(int bizType, Long bizId) {
        if (Boolean.TRUE.equals(redis.hasKey(countKey(bizType, bizId)))) {
            return;
        }
        Integer count = jdbc.query("SELECT liked_times FROM like_count WHERE biz_type = ? AND biz_id = ?",
                rs -> rs.next() ? rs.getInt(1) : 0, bizType, bizId);
        redis.opsForValue().set(countKey(bizType, bizId), String.valueOf(count == null ? 0 : count));
    }

    private void markDirty(int bizType, Long bizId) {
        redis.opsForSet().add(DIRTY_KEY, bizType + ":" + bizId);
    }

    private String userKey(int bizType, Long bizId) {
        return String.format(USER_KEY, bizType, bizId);
    }

    private String countKey(int bizType, Long bizId) {
        return String.format(COUNT_KEY, bizType, bizId);
    }
}
