package com.xingtu.learning.badge;

import com.xingtu.learning.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 成就徽章接口
 */
@RestController
@RequestMapping("/api/badge")
public class BadgeController {

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    @GetMapping("/my")
    public Result<List<Map<String, Object>>> my(@RequestParam Long userId) {
        return Result.ok(badgeService.myBadges(userId));
    }

    @GetMapping("/recent")
    public Result<List<Map<String, Object>>> recent(@RequestParam Long userId,
                                                    @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(badgeService.recent(userId, limit));
    }
}