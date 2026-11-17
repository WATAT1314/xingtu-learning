package com.xingtu.learning.learning;

import com.xingtu.learning.badge.BadgeService;
import com.xingtu.learning.common.BizException;
import com.xingtu.learning.learning.delay.LearningRecordDelayTaskHandler;
import com.xingtu.learning.learning.delay.LearningRecordPersister;
import com.xingtu.learning.learning.delay.RecordTaskData;
import com.xingtu.learning.points.PointsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习计划与学习进度
 *
 * 上报 → 合并进 Redis Hash → 延迟任务到点落库 → 学完推进课表进度并加积分
 */
@Service
public class LearningService implements LearningRecordPersister {

    private static final Logger log = LoggerFactory.getLogger(LearningService.class);
    private static final String RECORD_KEY_TEMPLATE = "xt:learning:record:%d";

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final LearningRecordDelayTaskHandler delayTaskHandler;
    private final PointsService pointsService;
    private final BadgeService badgeService;
    private final int finishPoints;

    public LearningService(JdbcTemplate jdbc,
                           StringRedisTemplate redis,
                           LearningRecordDelayTaskHandler delayTaskHandler,
                           PointsService pointsService,
                           BadgeService badgeService,
                           @Value("${xingtu.learning.finish-points:2}") int finishPoints) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.delayTaskHandler = delayTaskHandler;
        this.pointsService = pointsService;
        this.badgeService = badgeService;
        this.finishPoints = finishPoints;
    }

    /**
     * 报名课程 / 加入课表
     */
    public void join(Long userId, Long courseId, String courseName, int sectionCount) {
        jdbc.update("INSERT IGNORE INTO learning_lesson(user_id, course_id, course_name, section_count) VALUES (?,?,?,?)",
                userId, courseId, courseName, sectionCount);
    }

    /**
     * 我的课表
     */
    public List<Map<String, Object>> lessons(Long userId) {
        return jdbc.queryForList(
                "SELECT id, course_id, course_name, section_count, learned_section_count, plan_status, create_time " +
                        "FROM learning_lesson WHERE user_id = ? ORDER BY id", userId);
    }

    /**
     * 上报学习进度：只写 Redis，合并后由延迟任务落库
     */
    public Map<String, Object> report(Long userId, Long lessonId, Long sectionId,
                                      int sectionType, int moment, boolean finished) {
        Integer owned = jdbc.queryForObject(
                "SELECT COUNT(1) FROM learning_lesson WHERE id = ? AND user_id = ?", Integer.class, lessonId, userId);
        if (owned == null || owned == 0) {
            throw new BizException("课表不存在或不属于当前用户: " + lessonId);
        }
        String key = String.format(RECORD_KEY_TEMPLATE, lessonId);
        String field = userId + ":" + sectionId;
        String value = moment + "," + (finished ? 1 : 0) + "," + sectionType;
        // 同一用户同一小节多次上报，后写覆盖前写，天然完成合并
        redis.opsForHash().put(key, field, value);
        delayTaskHandler.addTask(new RecordTaskData(userId, lessonId, sectionId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lessonId", lessonId);
        result.put("sectionId", sectionId);
        result.put("mergedValue", value);
        result.put("pendingTasks", delayTaskHandler.pendingTasks());
        result.put("tip", "已写入 Redis 合并缓冲区，等延迟任务到点后统一落库");
        return result;
    }

    /**
     * 查看 Redis 中尚未落库的合并结果
     */
    public Map<String, Object> pending(Long lessonId) {
        String key = String.format(RECORD_KEY_TEMPLATE, lessonId);
        Map<Object, Object> entries = redis.opsForHash().entries(key);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("redisKey", key);
        result.put("size", entries.size());
        result.put("buffered", entries);
        result.put("pendingTasks", delayTaskHandler.pendingTasks());
        return result;
    }

    /**
     * 我的学习记录
     */
    public List<Map<String, Object>> records(Long userId, Long lessonId) {
        return jdbc.queryForList(
                "SELECT lesson_id, section_id, section_type, moment, finished, update_time " +
                        "FROM learning_record WHERE user_id = ? AND lesson_id = ? ORDER BY section_id",
                userId, lessonId);
    }

    /**
     * 延迟任务到点：从 Redis 取出合并结果并落库
     */
    @Override
    @Transactional
    public void persist(RecordTaskData data) {
        String key = String.format(RECORD_KEY_TEMPLATE, data.lessonId());
        String field = data.userId() + ":" + data.sectionId();
        Object cached = redis.opsForHash().get(key, field);
        if (cached == null) {
            return;
        }
        redis.opsForHash().delete(key, field);

        String[] parts = cached.toString().split(",");
        int moment = Integer.parseInt(parts[0]);
        int finished = Integer.parseInt(parts[1]);
        int sectionType = Integer.parseInt(parts[2]);

        Integer previous = jdbc.query(
                "SELECT finished FROM learning_record WHERE user_id = ? AND lesson_id = ? AND section_id = ?",
                rs -> rs.next() ? rs.getInt(1) : null,
                data.userId(), data.lessonId(), data.sectionId());

        jdbc.update("INSERT INTO learning_record(user_id, lesson_id, section_id, section_type, moment, finished) " +
                        "VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE moment = VALUES(moment), " +
                        "section_type = VALUES(section_type), finished = VALUES(finished)",
                data.userId(), data.lessonId(), data.sectionId(), sectionType, moment, finished);

        boolean justFinished = finished == 1 && (previous == null || previous == 0);
        if (justFinished) {
            refreshProgress(data.userId(), data.lessonId());
            pointsService.addPoints(data.userId(), finishPoints, 2, "学完一个小节");
            // 成就徽章：启程之星（第一节）/ 勤学之星（满 10 节）
            badgeService.checkAndAward(data.userId(), "first_step");
            badgeService.checkAndAward(data.userId(), "lesson_10");
        }
        log.info("学习记录落库完成 userId={} lessonId={} sectionId={} finished={}",
                data.userId(), data.lessonId(), data.sectionId(), finished);
    }

    /**
     * 重新统计课表进度：小节全部学完则标记为已学完
     */
    private void refreshProgress(Long userId, Long lessonId) {
        Integer learned = jdbc.queryForObject(
                "SELECT COUNT(1) FROM learning_record WHERE user_id = ? AND lesson_id = ? AND finished = 1",
                Integer.class, userId, lessonId);
        jdbc.update("UPDATE learning_lesson SET learned_section_count = ? WHERE id = ? AND user_id = ?",
                learned == null ? 0 : learned, lessonId, userId);

        Integer total = jdbc.queryForObject("SELECT section_count FROM learning_lesson WHERE id = ?",
                Integer.class, lessonId);
        if (total != null && learned != null && learned >= total) {
            jdbc.update("UPDATE learning_lesson SET plan_status = 1 WHERE id = ?", lessonId);
        }
    }

    /**
     * 课表概况（演示用）
     */
    public List<Map<String, Object>> summary(Long userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> lesson : lessons(userId)) {
            Map<String, Object> row = new LinkedHashMap<>(lesson);
            int total = ((Number) lesson.get("section_count")).intValue();
            int learned = ((Number) lesson.get("learned_section_count")).intValue();
            row.put("progress", total == 0 ? "0%" : (learned * 100 / total) + "%");
            list.add(row);
        }
        return list;
    }
}
