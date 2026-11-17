package com.xingtu.learning.streak;

import com.xingtu.learning.badge.BadgeService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 连续打卡
 *
 * 用 last_date 与当天比较判定连续：
 * 昨天打过 → 连续天数 +1；更早 → 重置为 1；今天已打 → 视为重复打卡。
 * 达成 3 天 / 7 天时自动颁发对应徽章。
 */
@Service
public class StreakService {

    private final JdbcTemplate jdbc;
    private final BadgeService badgeService;

    public StreakService(JdbcTemplate jdbc, BadgeService badgeService) {
        this.jdbc = jdbc;
        this.badgeService = badgeService;
    }

    /**
     * 打卡：返回当前连续天数与本次是否延续
     */
    public Map<String, Object> checkIn(Long userId) {
        LocalDate today = LocalDate.now();
        Map<String, Object> row = jdbc.query(
                "SELECT current_streak, best_streak, last_date FROM streak WHERE user_id = ?",
                rs -> rs.next()
                        ? Map.of("current", rs.getInt("current_streak"),
                        "best", rs.getInt("best_streak"),
                        "last", rs.getDate("last_date").toLocalDate())
                        : null,
                userId);

        int current;
        int best;
        boolean continued;
        if (row == null) {
            current = 1;
            best = 1;
            continued = false;
            jdbc.update("INSERT INTO streak(user_id, current_streak, best_streak, last_date) VALUES (?,?,?,?)",
                    userId, current, best, today);
        } else {
            LocalDate last = (LocalDate) row.get("last");
            int oldCurrent = (Integer) row.get("current");
            int oldBest = (Integer) row.get("best");
            if (last.equals(today)) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("userId", userId);
                result.put("currentStreak", oldCurrent);
                result.put("bestStreak", oldBest);
                result.put("todayChecked", true);
                result.put("tip", "今天已经打过卡了");
                return result;
            }
            continued = last.plusDays(1).equals(today);
            current = continued ? oldCurrent + 1 : 1;
            best = Math.max(oldBest, current);
            jdbc.update("UPDATE streak SET current_streak = ?, best_streak = ?, last_date = ? WHERE user_id = ?",
                    current, best, today, userId);
        }

        // 达成条件后自动颁发徽章
        if (current >= 3) {
            badgeService.checkAndAward(userId, "streak_3");
        }
        if (current >= 7) {
            badgeService.checkAndAward(userId, "streak_7");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("currentStreak", current);
        result.put("bestStreak", best);
        result.put("todayChecked", true);
        result.put("continued", continued);
        result.put("tip", continued ? "连续打卡 +1 天" : "打卡成功，开启新的连续记录");
        return result;
    }

    /**
     * 打卡概况
     */
    public Map<String, Object> status(Long userId) {
        Map<String, Object> row = jdbc.query(
                "SELECT current_streak, best_streak, last_date FROM streak WHERE user_id = ?",
                rs -> rs.next()
                        ? Map.of("current", rs.getInt("current_streak"),
                        "best", rs.getInt("best_streak"),
                        "last", rs.getDate("last_date").toLocalDate().toString())
                        : null,
                userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("currentStreak", row == null ? 0 : row.get("current"));
        result.put("bestStreak", row == null ? 0 : row.get("best"));
        result.put("lastDate", row == null ? null : row.get("last"));
        result.put("todayChecked", row != null && LocalDate.now().toString().equals(row.get("last")));
        return result;
    }

    /**
     * 打卡日历：最近 N 天是否打卡（用 points_board 的签到流水推导）
     */
    public List<Map<String, Object>> calendar(Long userId, int days) {
        return jdbc.queryForList(
                "SELECT DATE(create_time) AS d FROM points_board " +
                        "WHERE user_id = ? AND type = 1 AND create_time >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                        "GROUP BY DATE(create_time) ORDER BY d",
                userId, days);
    }
}