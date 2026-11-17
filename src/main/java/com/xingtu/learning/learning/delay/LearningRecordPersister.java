package com.xingtu.learning.learning.delay;

/**
 * 延迟任务到点后的落库动作，由业务层实现，避免 handler 与 service 强耦合
 */
public interface LearningRecordPersister {

    void persist(RecordTaskData data);
}
