package com.xingtu.learning.agent;

import com.xingtu.learning.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 星途学习助手 Agent 接口
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final LearningAgentService agentService;

    public AgentController(LearningAgentService agentService) {
        this.agentService = agentService;
    }

    /** 学习诊断 */
    @GetMapping("/diagnose")
    public Result<Map<String, Object>> diagnose(@RequestParam Long userId) {
        return Result.ok(agentService.diagnose(userId));
    }

    /** 下一节推荐 */
    @GetMapping("/next")
    public Result<Map<String, Object>> next(@RequestParam Long userId) {
        return Result.ok(agentService.nextSection(userId));
    }

    /** 关键词答疑 */
    @PostMapping("/ask")
    public Result<Map<String, Object>> ask(@RequestParam Long userId, @RequestBody Map<String, String> body) {
        return Result.ok(agentService.ask(userId, body.get("question")));
    }

    /** 学习规划 */
    @GetMapping("/plan")
    public Result<Map<String, Object>> plan(@RequestParam Long userId,
                                            @RequestParam(defaultValue = "14") int days) {
        return Result.ok(agentService.plan(userId, days));
    }
}