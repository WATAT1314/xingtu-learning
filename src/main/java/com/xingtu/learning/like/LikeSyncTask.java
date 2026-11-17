package com.xingtu.learning.like;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 点赞数据定时落库：把 Redis 里的计数与关系批量写入数据库
 */
@Component
public class LikeSyncTask {

    private static final Logger log = LoggerFactory.getLogger(LikeSyncTask.class);

    private final LikeService likeService;

    public LikeSyncTask(LikeService likeService) {
        this.likeService = likeService;
    }

    @Scheduled(fixedDelayString = "${xingtu.like.sync-interval-ms:10000}", initialDelay = 10000)
    public void syncLikeData() {
        int processed = likeService.sync();
        if (processed > 0) {
            log.info("点赞数据同步完成，处理业务对象 {} 个", processed);
        }
    }
}
