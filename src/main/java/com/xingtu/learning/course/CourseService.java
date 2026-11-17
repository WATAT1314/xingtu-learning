package com.xingtu.learning.course;

import com.xingtu.learning.common.BizException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程与章节内容
 *
 * 课程列表、章节大纲、章节正文、随堂小测。
 */
@Service
public class CourseService {

    private final JdbcTemplate jdbc;

    public CourseService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 全部课程（带章节数与我的学习进度）
     */
    public List<Map<String, Object>> catalog(Long userId) {
        List<Map<String, Object>> courses = jdbc.queryForList(
                "SELECT c.id, c.title, c.cover, c.summary, c.level, c.duration_h, c.tags, " +
                        "(SELECT COUNT(1) FROM section s WHERE s.course_id = c.id) AS section_count " +
                        "FROM course c ORDER BY c.id");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> c : courses) {
            Map<String, Object> row = new LinkedHashMap<>(c);
            long courseId = ((Number) c.get("id")).longValue();
            Integer learned = jdbc.queryForObject(
                    "SELECT COUNT(1) FROM learning_record r JOIN learning_lesson l ON l.id = r.lesson_id " +
                            "WHERE l.user_id = ? AND l.course_id = ? AND r.finished = 1",
                    Integer.class, userId, courseId);
            int total = ((Number) c.get("section_count")).intValue();
            int done = learned == null ? 0 : learned;
            row.put("learned", done);
            row.put("progress", total == 0 ? 0 : (done * 100 / total));
            result.add(row);
        }
        return result;
    }

    /**
     * 章节大纲（不含正文，节省流量）
     */
    public List<Map<String, Object>> outline(Long courseId) {
        return jdbc.queryForList(
                "SELECT id, course_id, idx, title, minutes, quiz FROM section " +
                        "WHERE course_id = ? ORDER BY idx", courseId);
    }

    /**
     * 章节详情（含正文与小测）
     */
    public Map<String, Object> sectionDetail(Long sectionId, Long userId) {
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT s.id, s.course_id, s.idx, s.title, s.minutes, s.content, s.quiz, c.title AS course_title " +
                        "FROM section s JOIN course c ON c.id = s.course_id WHERE s.id = ?", sectionId);
        if (list.isEmpty()) {
            throw new BizException("章节不存在: " + sectionId);
        }
        Map<String, Object> row = new LinkedHashMap<>(list.get(0));
        // 查询该用户在本章节的完成状态
        Integer finished = jdbc.query(
                "SELECT r.finished FROM learning_record r JOIN learning_lesson l ON l.id = r.lesson_id " +
                        "WHERE l.user_id = ? AND l.course_id = ? AND r.section_id = ?",
                rs -> rs.next() ? rs.getInt(1) : 0,
                userId, row.get("course_id"), row.get("idx"));
        row.put("finished", finished != null && finished == 1);
        return row;
    }

    /**
     * 课程详情（课程信息 + 大纲 + 逐章完成状态）
     */
    public Map<String, Object> courseDetail(Long courseId, Long userId) {
        List<Map<String, Object>> courses = jdbc.queryForList(
                "SELECT id, title, cover, summary, level, duration_h, tags FROM course WHERE id = ?", courseId);
        if (courses.isEmpty()) {
            throw new BizException("课程不存在: " + courseId);
        }
        Map<String, Object> course = new LinkedHashMap<>(courses.get(0));
        List<Map<String, Object>> sections = outline(courseId);

        // 该用户在这门课已完成的章节序号
        List<Integer> doneIdx = jdbc.query(
                "SELECT r.section_id FROM learning_record r JOIN learning_lesson l ON l.id = r.lesson_id " +
                        "WHERE l.user_id = ? AND l.course_id = ? AND r.finished = 1",
                (rs, i) -> rs.getInt(1), userId, courseId);

        for (Map<String, Object> s : sections) {
            int idx = ((Number) s.get("idx")).intValue();
            s.put("finished", doneIdx.contains(idx));
            s.remove("quiz");
        }
        course.put("sections", sections);
        course.put("sectionCount", sections.size());
        course.put("learnedCount", doneIdx.size());
        course.put("progress", sections.isEmpty() ? 0 : doneIdx.size() * 100 / sections.size());
        return course;
    }
}