package com.xingtu.learning.points;

import com.xingtu.learning.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 积分与排行榜接口
 */
@RestController
@RequestMapping("/api/points")
public class PointsController {

    private final PointsService pointsService;

    public PointsController(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    @PostMapping("/sign")
    public Result<Map<String, Object>> sign(@RequestParam Long userId) {
        return Result.ok(pointsService.sign(userId));
    }

    @GetMapping("/score")
    public Result<Integer> score(@RequestParam Long userId) {
        return Result.ok(pointsService.score(userId));
    }

    @GetMapping("/rank")
    public Result<List<Map<String, Object>>> rank(@RequestParam(defaultValue = "10") int topN) {
        return Result.ok(pointsService.rank(topN));
    }

    @GetMapping("/history")
    public Result<List<Map<String, Object>>> history(@RequestParam Long userId) {
        return Result.ok(pointsService.history(userId));
    }
}
