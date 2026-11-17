package com.xingtu.learning.learning.domain;

import java.time.LocalDateTime;

/**
 * 学习记录（按 用户 + 课表 + 小节 唯一）
 */
public record LearningRecord(Long id,
                             Long userId,
                             Long lessonId,
                             Long sectionId,
                             int sectionType,
                             int moment,
                             int finished,
                             LocalDateTime updateTime) {
}
