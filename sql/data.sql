-- 演示数据
USE xingtu_learning;

INSERT INTO learning_lesson (user_id, course_id, course_name, section_count)
VALUES (1, 1001, '星途 · Java 微服务架构实战', 12),
       (1, 1002, '星途 · Redis 高并发实战', 8);

INSERT INTO coupon (name, type, threshold_amount, discount_amount, discount_rate, per_amount, total_num, remain_num, begin_time, end_time)
VALUES ('星途启航 · 新人券', 1, 0, 10, 10, 0, 100, 100, '2026-01-01 00:00:00', '2027-12-31 23:59:59'),
       ('星途满减 · 满200减30', 2, 200, 30, 10, 0, 100, 100, '2026-01-01 00:00:00', '2027-12-31 23:59:59'),
       ('星途每满 · 每满100减15', 3, 0, 15, 10, 100, 100, 100, '2026-01-01 00:00:00', '2027-12-31 23:59:59'),
       ('星途尊享 · 全场9折', 4, 0, 0, 9, 0, 100, 100, '2026-01-01 00:00:00', '2027-12-31 23:59:59');

-- 成就徽章定义
INSERT INTO badge (code, name, description, icon) VALUES
('first_step',      '启程之星',   '完成第一门课程的第一节课', 'star'),
('lesson_10',       '勤学之星',   '累计学完 10 节课',         'award'),
('streak_3',        '三日之星',   '连续打卡满 3 天',          'fire'),
('streak_7',        '周周之星',   '连续打卡满 7 天',          'flame'),
('sign_10',         '坚持之星',   '累计签到满 10 次',         'ten'),
('coupon_master',   '领券达人',   '累计领取 5 张优惠券',       'ticket');
