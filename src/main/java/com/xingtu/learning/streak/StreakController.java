package com.xingtu.learning.streak;

import com.xingtu.learning.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 连续打卡接口
 */
@RestController
@RequestMapping("/api/streak")
public class StreakController {

    private final StreakService streakService;

    public StreakController(StreakService streakService) {
        this.streakService = streakService;
    }

    @PostMapping("/checkin")
    public Result<Map<String, Object>> checkIn(@RequestParam Long userId) {
        return Result.ok(streakService.checkIn(userId));
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status(@RequestParam Long userId) {
        return Result.ok(streakService.status(userId));
    }

    @GetMapping("/calendar")
    public Result<List<Map<String, Object>>> calendar(@RequestParam Long userId,
                                                      @RequestParam(defaultValue = "30") int days) {
        return Result.ok(streakService.calendar(userId, days));
    }
}