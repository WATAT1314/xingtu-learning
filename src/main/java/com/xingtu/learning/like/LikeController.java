package com.xingtu.learning.like;

import com.xingtu.learning.common.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 点赞接口
 */
@RestController
@RequestMapping("/api/like")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping
    public Result<Map<String, Object>> like(@RequestBody LikeForm form) {
        return Result.ok(likeService.like(form.userId(), form.bizType(), form.bizId()));
    }

    @DeleteMapping
    public Result<Map<String, Object>> unlike(@RequestBody LikeForm form) {
        return Result.ok(likeService.unlike(form.userId(), form.bizType(), form.bizId()));
    }

    @GetMapping("/count")
    public Result<Long> count(@RequestParam int bizType, @RequestParam Long bizId) {
        return Result.ok(likeService.count(bizType, bizId));
    }

    /**
     * 手动触发同步（生产中由定时任务执行）
     */
    @PostMapping("/sync")
    public Result<Map<String, Object>> sync() {
        int processed = likeService.sync();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("syncedBizCount", processed);
        result.put("dirtyLeft", likeService.dirtySize());
        return Result.ok(result);
    }

    @GetMapping("/pending")
    public Result<Long> pending() {
        return Result.ok(likeService.dirtySize());
    }

    public record LikeForm(Long userId, int bizType, Long bizId) {
    }
}
