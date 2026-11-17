package com.xingtu.learning.course;

import com.xingtu.learning.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 课程与章节接口
 */
@RestController
@RequestMapping("/api/course")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/catalog")
    public Result<List<Map<String, Object>>> catalog(@RequestParam Long userId) {
        return Result.ok(courseService.catalog(userId));
    }

    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(@RequestParam Long courseId, @RequestParam Long userId) {
        return Result.ok(courseService.courseDetail(courseId, userId));
    }

    @GetMapping("/section")
    public Result<Map<String, Object>> section(@RequestParam Long sectionId, @RequestParam Long userId) {
        return Result.ok(courseService.sectionDetail(sectionId, userId));
    }

    @GetMapping("/outline")
    public Result<List<Map<String, Object>>> outline(@RequestParam Long courseId) {
        return Result.ok(courseService.outline(courseId));
    }
}