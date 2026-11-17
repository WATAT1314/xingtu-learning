package com.xingtu.learning.note;

import com.xingtu.learning.common.BizException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习笔记
 *
 * 每个用户对小节可记多条笔记；支持私密/公开，公开笔记可被"学习广场"看到。
 */
@Service
public class NoteService {

    private final JdbcTemplate jdbc;

    public NoteService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 新增笔记
     */
    public Map<String, Object> create(Long userId, Long lessonId, Long sectionId, String content, int isPublic) {
        if (content == null || content.isBlank()) {
            throw new BizException("笔记内容不能为空");
        }
        jdbc.update("INSERT INTO study_note(user_id, lesson_id, section_id, content, is_public) VALUES (?,?,?,?,?)",
                userId, lessonId, sectionId, content, isPublic);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("lessonId", lessonId);
        result.put("sectionId", sectionId);
        result.put("content", content);
        result.put("isPublic", isPublic);
        result.put("tip", "笔记已保存");
        return result;
    }

    /**
     * 我的笔记（按课表过滤）
     */
    public List<Map<String, Object>> mine(Long userId, Long lessonId) {
        if (lessonId != null && lessonId > 0) {
            return jdbc.queryForList(
                    "SELECT id, lesson_id, section_id, content, is_public, create_time " +
                            "FROM study_note WHERE user_id = ? AND lesson_id = ? ORDER BY id DESC LIMIT 100",
                    userId, lessonId);
        }
        return jdbc.queryForList(
                "SELECT id, lesson_id, section_id, content, is_public, create_time " +
                        "FROM study_note WHERE user_id = ? ORDER BY id DESC LIMIT 100",
                userId);
    }

    /**
     * 学习广场：最新公开笔记
     */
    public List<Map<String, Object>> publicNotes(int limit) {
        return jdbc.queryForList(
                "SELECT id, user_id, lesson_id, section_id, content, create_time " +
                        "FROM study_note WHERE is_public = 1 ORDER BY id DESC LIMIT ?",
                limit);
    }

    /**
     * 删除笔记（仅本人）
     */
    public void delete(Long userId, Long noteId) {
        int n = jdbc.update("DELETE FROM study_note WHERE id = ? AND user_id = ?", noteId, userId);
        if (n == 0) {
            throw new BizException("笔记不存在或无权删除");
        }
    }

    /**
     * 编辑笔记（仅本人）
     */
    public void update(Long userId, Long noteId, String content, int isPublic) {
        int n = jdbc.update("UPDATE study_note SET content = ?, is_public = ? WHERE id = ? AND user_id = ?",
                content, isPublic, noteId, userId);
        if (n == 0) {
            throw new BizException("笔记不存在或无权编辑");
        }
    }
}