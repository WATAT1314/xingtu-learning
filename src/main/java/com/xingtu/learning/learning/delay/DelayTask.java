package com.xingtu.learning.learning.delay;

import java.time.Duration;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 延迟任务：按 deadline 在 DelayQueue 中排序
 */
public class DelayTask<D> implements Delayed {

    private final D data;
    private final long deadlineNanos;

    public DelayTask(D data, Duration delay) {
        this.data = data;
        this.deadlineNanos = System.nanoTime() + delay.toNanos();
    }

    public D getData() {
        return data;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long remain = Math.max(0, deadlineNanos - System.nanoTime());
        return unit.convert(remain, TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        return Long.compare(getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
    }
}
