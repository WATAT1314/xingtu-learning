package com.xingtu.learning.learning.delay;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

/**
 * 学习记录延迟任务处理器
 *
 * 播放进度每隔几秒就上报一次，如果每次都写库会造成大量无效 IO。
 * 这里先把上报合并进 Redis Hash，再用 DelayQueue 承载延迟任务，
 * 到点一次性落库，做到「合并写 + 异步落库」。
 */
@Component
public class LearningRecordDelayTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(LearningRecordDelayTaskHandler.class);

    private final DelayQueue<DelayTask<RecordTaskData>> queue = new DelayQueue<>();
    private final LearningRecordPersister persister;
    private final long delaySeconds;
    private volatile boolean running = true;

    public LearningRecordDelayTaskHandler(@Lazy LearningRecordPersister persister,
                                          @Value("${xingtu.learning.record-delay-seconds:5}") long delaySeconds) {
        this.persister = persister;
        this.delaySeconds = delaySeconds;
    }

    @PostConstruct
    public void init() {
        Thread worker = new Thread(this::consume, "learning-record-delay-task");
        worker.setDaemon(true);
        worker.start();
        log.info("学习记录延迟任务处理器已启动，延迟 {} 秒落库", delaySeconds);
    }

    @PreDestroy
    public void destroy() {
        running = false;
        log.info("学习记录延迟任务处理器已停止");
    }

    /**
     * 提交一个延迟任务
     */
    public void addTask(RecordTaskData data) {
        queue.offer(new DelayTask<>(data, Duration.ofSeconds(delaySeconds)));
    }

    /**
     * 当前排队中的任务数（用于演示观察合并效果）
     */
    public int pendingTasks() {
        return queue.size();
    }

    private void consume() {
        while (running) {
            try {
                DelayTask<RecordTaskData> task = queue.poll(1, TimeUnit.SECONDS);
                if (task == null) {
                    continue;
                }
                persister.persist(task.getData());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("处理学习记录延迟任务失败", e);
            }
        }
    }
}
