package com.xingtu.learning.learning;

import com.xingtu.learning.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 学习计划与学习进度接口
 */
@RestController
@RequestMapping("/api/learning")
public class LearningController {

    private final LearningService learningService;

    public LearningController(LearningService learningService) {
        this.learningService = learningService;
    }

    /** 报名课程（加入课表） */
    @PostMapping("/join")
    public Result<Void> join(@RequestParam Long userId,
                             @RequestParam Long courseId,
                             @RequestParam(required = false) String courseName,
                             @RequestParam(defaultValue = "10") int sectionCount) {
        learningService.join(userId, courseId,
                courseName == null ? "课程-" + courseId : courseName, sectionCount);
        return Result.ok();
    }

    /** 我的课表 */
    @GetMapping("/lessons")
    public Result<List<Map<String, Object>>> lessons(@RequestParam Long userId) {
        return Result.ok(learningService.lessons(userId));
    }

    /** 课表进度概况 */
    @GetMapping("/summary")
    public Result<List<Map<String, Object>>> summary(@RequestParam Long userId) {
        return Result.ok(learningService.summary(userId));
    }

    /** 上报学习进度（写 Redis 合并缓冲区） */
    @PostMapping("/records")
    public Result<Map<String, Object>> report(@RequestBody ReportForm form) {
        return Result.ok(learningService.report(form.userId(), form.lessonId(), form.sectionId(),
                form.sectionType(), form.moment(), form.finished()));
    }

    /** 查看尚未落库的合并结果 */
    @GetMapping("/pending")
    public Result<Map<String, Object>> pending(@RequestParam Long lessonId) {
        return Result.ok(learningService.pending(lessonId));
    }

    /** 已落库的学习记录 */
    @GetMapping("/records")
    public Result<List<Map<String, Object>>> records(@RequestParam Long userId,
                                                     @RequestParam Long lessonId) {
        return Result.ok(learningService.records(userId, lessonId));
    }

    public record ReportForm(Long userId,
                             Long lessonId,
                             Long sectionId,
                             int sectionType,
                             int moment,
                             boolean finished) {
    }
}
