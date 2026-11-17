package com.xingtu.learning.learning.delay;

/**
 * 延迟任务负载：一次学习进度上报的定位信息
 */
public record RecordTaskData(Long userId, Long lessonId, Long sectionId) {
}
