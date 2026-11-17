package com.xingtu.learning.badge;

import com.xingtu.learning.common.BizException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 成就徽章
 *
 * 徽章定义表 badge 存「获得条件」，用户获得记录存 user_badge。
 * checkAndAward 幂等：已获得的徽章不会重复颁发。
 */
@Service
public class BadgeService {

    private final JdbcTemplate jdbc;

    public BadgeService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 校验条件并颁发徽章（幂等）
     *
     * @return true 表示本次新获得
     */
    public boolean checkAndAward(Long userId, String code) {
        Long badgeId = jdbc.query("SELECT id FROM badge WHERE code = ? ORDER BY id LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null, code);
        if (badgeId == null) {
            return false;
        }
        boolean satisfied = satisfied(userId, code);
        if (!satisfied) {
            return false;
        }
        int updated = jdbc.update(
                "INSERT IGNORE INTO user_badge(user_id, badge_id) VALUES (?,?)", userId, badgeId);
        return updated > 0;
    }

    /**
     * 各徽章的达成条件
     */
    private boolean satisfied(Long userId, String code) {
        switch (code) {
            case "first_step": {
                Integer n = jdbc.queryForObject(
                        "SELECT COUNT(1) FROM learning_record WHERE user_id = ? AND finished = 1",
                        Integer.class, userId);
                return n != null && n >= 1;
            }
            case "lesson_10": {
                Integer n = jdbc.queryForObject(
                        "SELECT COUNT(1) FROM learning_record WHERE user_id = ? AND finished = 1",
                        Integer.class, userId);
                return n != null && n >= 10;
            }
            case "streak_3": {
                Integer n = jdbc.queryForObject(
                        "SELECT current_streak FROM streak WHERE user_id = ?",
                        Integer.class, userId);
                return n != null && n >= 3;
            }
            case "streak_7": {
                Integer n = jdbc.queryForObject(
                        "SELECT current_streak FROM streak WHERE user_id = ?",
                        Integer.class, userId);
                return n != null && n >= 7;
            }
            case "sign_10": {
                Integer n = jdbc.queryForObject(
                        "SELECT COUNT(1) FROM points_board WHERE user_id = ? AND type = 1",
                        Integer.class, userId);
                return n != null && n >= 10;
            }
            case "coupon_master": {
                Integer n = jdbc.queryForObject(
                        "SELECT COUNT(1) FROM user_coupon WHERE user_id = ?",
                        Integer.class, userId);
                return n != null && n >= 5;
            }
            default:
                return false;
        }
    }

    /**
     * 我的徽章（已获得 + 未获得）
     */
    public List<Map<String, Object>> myBadges(Long userId) {
        List<Map<String, Object>> all = jdbc.queryForList(
                "SELECT b.id, b.code, b.name, b.description, b.icon FROM badge b ORDER BY b.id");
        List<Long> owned = jdbc.query("SELECT badge_id FROM user_badge WHERE user_id = ?",
                (rs, i) -> rs.getLong(1), userId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> badge : all) {
            Map<String, Object> row = new LinkedHashMap<>(badge);
            row.put("owned", owned.contains(((Number) badge.get("id")).longValue()));
            list.add(row);
        }
        return list;
    }

    /**
     * 最近获得徽章流水
     */
    public List<Map<String, Object>> recent(Long userId, int limit) {
        return jdbc.queryForList(
                "SELECT u.create_time, b.name, b.description, b.icon FROM user_badge u " +
                        "JOIN badge b ON b.id = u.badge_id WHERE u.user_id = ? ORDER BY u.id DESC LIMIT ?",
                userId, limit);
    }

    /**
     * 标记获得（供内部直接使用，例如新用户引导）
     */
    public void ensureBadge(Long userId, Long badgeId) {
        jdbc.update("INSERT IGNORE INTO user_badge(user_id, badge_id) VALUES (?,?)", userId, badgeId);
    }

    public void requireUser(Long userId) {
        if (userId == null) {
            throw new BizException("userId 不能为空");
        }
    }
}