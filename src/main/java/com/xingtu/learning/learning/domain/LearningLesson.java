package com.xingtu.learning.domain;

import java.time.LocalDateTime;

/**
 * 我的课表
 */
public record LearningLesson(Long id,
                             Long userId,
                             Long courseId,
                             String courseName,
                             int sectionCount,
                             int learnedSectionCount,
                             int planStatus,
                             LocalDateTime createTime) {

    public static final int PLAN_LEARNING = 0;
    public static final int PLAN_FINISHED = 1;
}
