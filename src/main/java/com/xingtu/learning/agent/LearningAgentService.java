package com.xingtu.learning.agent;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 星途学习助手 Agent
 *
 * 数据驱动的规则智能体：
 * 1. 学习诊断：分析课表/记录/打卡/积分数据，给出画像与建议
 * 2. 章节推荐：根据进度推荐下一节该学的内容
 * 3. 关键词答疑：基于课程章节内容的知识检索问答
 * 4. 学习规划：根据剩余章节与历史节奏生成计划
 */
@Service
public class LearningAgentService {

    private final JdbcTemplate jdbc;

    /** 答疑知识库关键词表（从课程章节正文中检索） */
    private static final Map<String, List<String>> KNOWLEDGE = buildKnowledge();

    private static Map<String, List<String>> buildKnowledge() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("锁", List.of("分布式锁", "SETNX", "Redisson", "看门狗"));
        m.put("缓存", List.of("穿透", "击穿", "雪崩", "布隆"));
        m.put("lua", List.of("Lua", "原子", "秒杀"));
        m.put("事务", List.of("TCC", "本地消息表", "Saga", "最终一致"));
        m.put("网关", List.of("网关", "路由", "限流"));
        m.put("注册", List.of("Nacos", "注册中心", "心跳", "发现"));
        m.put("持久化", List.of("RDB", "AOF", "哨兵", "Cluster"));
        m.put("链路", List.of("TraceID", "链路追踪", "Span"));
        m.put("微服务", List.of("微服务", "拆分", "单体", "边界"));
        m.put("zset", List.of("ZSet", "排行榜", "延迟队列"));
        m.put("监控", List.of("监控", "P99", "黄金指标", "告警"));
        m.put("灰度", List.of("灰度", "金丝雀", "蓝绿", "发布"));
        return m;
    }

    public LearningAgentService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 学习诊断：汇总各维度数据，给出画像和建议
     */
    public Map<String, Object> diagnose(Long userId) {
        Map<String, Object> report = new LinkedHashMap<>();

        // 1. 课程进度
        List<Map<String, Object>> lessons = jdbc.queryForList(
                "SELECT course_name, section_count, learned_section_count FROM learning_lesson WHERE user_id = ?", userId);
        int totalSections = 0;
        int learnedSections = 0;
        for (Map<String, Object> l : lessons) {
            totalSections += ((Number) l.get("section_count")).intValue();
            learnedSections += ((Number) l.get("learned_section_count")).intValue();
        }
        int progressPct = totalSections == 0 ? 0 : learnedSections * 100 / totalSections;

        // 2. 打卡
        Map<String, Object> streak = jdbc.query(
                "SELECT current_streak, best_streak, last_date FROM streak WHERE user_id = ?",
                rs -> rs.next()
                        ? Map.of("current", rs.getInt("current_streak"), "best", rs.getInt("best_streak"))
                        : Map.of("current", 0, "best", 0),
                userId);

        // 3. 积分与笔记
        Integer points = jdbc.queryForObject(
                "SELECT IFNULL(SUM(points),0) FROM points_board WHERE user_id = ?", Integer.class, userId);
        Integer notes = jdbc.queryForObject(
                "SELECT COUNT(1) FROM study_note WHERE user_id = ?", Integer.class, userId);
        Integer finished = jdbc.queryForObject(
                "SELECT COUNT(1) FROM learning_record WHERE user_id = ? AND finished = 1",
                Integer.class, userId);

        // 4. 学习节奏（最近 7 天学完的节数）
        Integer recent = jdbc.queryForObject(
                "SELECT COUNT(1) FROM learning_record WHERE user_id = ? AND finished = 1 " +
                        "AND update_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)", Integer.class, userId);

        // 画像
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("enrolledCourses", lessons.size());
        profile.put("totalSections", totalSections);
        profile.put("learnedSections", learnedSections);
        profile.put("progressPct", progressPct);
        profile.put("finishedSections", finished == null ? 0 : finished);
        profile.put("streakCurrent", ((Number) streak.get("current")).intValue());
        profile.put("streakBest", ((Number) streak.get("best")).intValue());
        profile.put("points", points == null ? 0 : points);
        profile.put("noteCount", notes == null ? 0 : notes);
        profile.put("recent7dSections", recent == null ? 0 : recent);
        report.put("profile", profile);

        // 诊断结论
        List<String> findings = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        if (lessons.isEmpty()) {
            findings.add("你还没有加入任何课程");
            suggestions.add("先去「课程」页选一门课加入课表，推荐从「星途 · Java 微服务架构实战」开始");
        } else if (progressPct == 0) {
            findings.add("已加入 " + lessons.size() + " 门课但还没有完成任何章节");
            suggestions.add("从第一章开始，每章 25~40 分钟，先完成「为什么需要微服务」建立信心");
        } else if (progressPct < 30) {
            findings.add("整体进度 " + progressPct + "%，刚起步阶段");
            suggestions.add("保持每天 1 章的节奏，两周可以完成一门课");
        } else if (progressPct < 70) {
            findings.add("整体进度 " + progressPct + "%，处于中段爬坡期");
            suggestions.add("中段容易松懈，建议配合连续打卡保持节奏");
        } else {
            findings.add("整体进度 " + progressPct + "%，接近完成，干得漂亮！");
            suggestions.add("剩余章节收尾后，可以开始下一门课");
        }

        int streakDays = ((Number) streak.get("current")).intValue();
        if (streakDays == 0) {
            findings.add("还没有打卡记录");
            suggestions.add("今天先打一次卡，连续 3 天可以解锁「三日之星」徽章");
        } else if (streakDays < 3) {
            findings.add("已连续打卡 " + streakDays + " 天");
            suggestions.add("再坚持 " + (3 - streakDays) + " 天解锁「三日之星」徽章");
        } else {
            findings.add("已连续打卡 " + streakDays + " 天，习惯不错");
        }

        if (notes == null || notes == 0) {
            findings.add("还没有写过笔记");
            suggestions.add("学完一章后写一篇笔记，哪怕只有三句话，复习效率会翻倍");
        } else {
            findings.add("已积累 " + notes + " 篇笔记");
        }

        if (recent != null && recent == 0 && progressPct > 0) {
            findings.add("最近 7 天没有新的学习记录");
            suggestions.add("似乎停滞了，建议今天重启学习，先复习上一篇笔记热身");
        }

        report.put("findings", findings);
        report.put("suggestions", suggestions);
        return report;
    }

    private int progressct(int pct) {
        return pct;
    }

    /**
     * 下一节推荐：找到当前课程第一个未完成的章节
     */
    public Map<String, Object> nextSection(Long userId) {
        // 取用户第一门未完成的课
        Map<String, Object> lesson = jdbc.query(
                "SELECT l.id AS lesson_id, l.course_id, l.course_name, l.section_count, l.learned_section_count " +
                        "FROM learning_lesson l WHERE l.user_id = ? AND l.plan_status = 0 ORDER BY l.id LIMIT 1",
                rs -> rs.next() ? Map.of(
                        "lessonId", rs.getLong("lesson_id"),
                        "courseId", rs.getLong("course_id"),
                        "courseName", rs.getString("course_name"),
                        "sectionCount", rs.getInt("section_count"),
                        "learned", rs.getInt("learned_section_count")) : null,
                userId);
        if (lesson == null) {
            return Map.of("tip", "所有课程都已完成，可以开始新课程了");
        }
        long courseId = ((Number) lesson.get("courseId")).longValue();
        List<Integer> doneIdx = jdbc.query(
                "SELECT r.section_id FROM learning_record r JOIN learning_lesson l ON l.id = r.lesson_id " +
                        "WHERE l.user_id = ? AND l.course_id = ? AND r.finished = 1",
                (rs, i) -> rs.getInt(1), userId, courseId);

        List<Map<String, Object>> sections = jdbc.queryForList(
                "SELECT id, idx, title, minutes FROM section WHERE course_id = ? ORDER BY idx", courseId);
        for (Map<String, Object> s : sections) {
            int idx = ((Number) s.get("idx")).intValue();
            if (!doneIdx.contains(idx)) {
                Map<String, Object> result = new LinkedHashMap<>(lesson);
                result.put("sectionId", s.get("id"));
                result.put("sectionIdx", idx);
                result.put("sectionTitle", s.get("title"));
                result.put("minutes", s.get("minutes"));
                result.put("tip", "建议先学「第 " + idx + " 章 · " + s.get("title") + "」，预计 " + s.get("minutes") + " 分钟");
                return result;
            }
        }
        return Map.of("tip", "当前课程已全部学完");
    }

    /**
     * 关键词答疑：从课程章节内容检索最相关章节
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> ask(Long userId, String question) {
        String q = question == null ? "" : question.toLowerCase();
        List<String> keywords = null;
        for (Map.Entry<String, List<String>> e : KNOWLEDGE.entrySet()) {
            if (q.contains(e.getKey())) {
                keywords = e.getValue();
                break;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        if (keywords == null) {
            result.put("answer", "我先基于你的学习数据给出建议，关于具体知识点，试试问我这些主题：" +
                    "分布式锁、缓存穿透击穿雪崩、Lua 秒杀、分布式事务、网关、注册中心、持久化、链路追踪、排行榜、监控、灰度发布。");
            result.put("sections", List.of());
            return result;
        }

        // 在章节正文里检索命中关键词最多的章节
        String bestSectionId = null;
        String bestTitle = null;
        int bestHits = 0;
        List<Map<String, Object>> allSections = jdbc.queryForList(
                "SELECT id, course_id, idx, title, content FROM section");
        for (Map<String, Object> s : allSections) {
            String content = String.valueOf(s.get("content")).toLowerCase();
            int hits = 0;
            for (String kw : keywords) {
                if (content.contains(kw.toLowerCase())) {
                    hits++;
                }
            }
            if (hits > bestHits) {
                bestHits = hits;
                bestSectionId = String.valueOf(s.get("id"));
                bestTitle = "第 " + s.get("idx") + " 章 · " + s.get("title") + "（课程 " + s.get("course_id") + "）";
            }
        }

        if (bestSectionId == null) {
            result.put("answer", "暂未在课程内容中找到与「" + question + "」直接相关的章节，可以换个关键词试试。");
            result.put("sections", List.of());
            return result;
        }

        result.put("answer", "关于「" + question + "」，最相关的章节是：" + bestTitle +
                "，命中 " + bestHits + " 个关键词。建议去阅读该章节的完整内容。");
        result.put("sectionId", bestSectionId);
        result.put("sectionTitle", bestTitle);
        result.put("matchedKeywords", keywords);
        return result;
    }

    /**
     * 学习规划：根据剩余章节和历史节奏生成 N 天计划
     */
    public Map<String, Object> plan(Long userId, int days) {
        Map<String, Object> next = nextSection(userId);
        // 统计剩余章节总数
        Integer remaining = jdbc.queryForObject(
                "SELECT IFNULL(SUM(l.section_count - l.learned_section_count),0) " +
                        "FROM learning_lesson l WHERE l.user_id = ?", Integer.class, userId);
        int remain = remaining == null ? 0 : remaining;
        int perDay = Math.max(1, (int) Math.ceil((double) remain / Math.max(1, days)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("remainingSections", remain);
        result.put("planDays", days);
        result.put("sectionsPerDay", perDay);
        result.put("next", next);
        result.put("tip", remain == 0
                ? "所有课程已完成，太棒了！"
                : "按每天 " + perDay + " 章的节奏，" + days + " 天可以完成全部剩余 " + remain + " 章。");
        return result;
    }
}