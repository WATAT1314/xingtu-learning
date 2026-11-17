package com.xingtu.learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 星途学堂核心模块（学习进度 / 点赞 / 积分排行榜 / 优惠券）
 */
@EnableScheduling
@SpringBootApplication
public class XingtuLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(XingtuLearningApplication.class, args);
    }
}
