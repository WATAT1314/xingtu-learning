-- =========================================================
-- 星途学堂核心模块表结构（学习进度 / 点赞 / 积分排行榜 / 优惠券 / 学习笔记 / 连续打卡 / 成就徽章）
-- 执行方式: mysql -uroot -p < sql/schema.sql
-- =========================================================
CREATE DATABASE IF NOT EXISTS xingtu_learning DEFAULT CHARACTER SET utf8mb4;
USE xingtu_learning;

DROP TABLE IF EXISTS learning_lesson;
CREATE TABLE learning_lesson (
    id                   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '课表id',
    user_id              BIGINT      NOT NULL COMMENT '用户id',
    course_id            BIGINT      NOT NULL COMMENT '课程id',
    course_name          VARCHAR(64) NOT NULL COMMENT '课程名称',
    section_count        INT         NOT NULL DEFAULT 0 COMMENT '小节总数',
    learned_section_count INT        NOT NULL DEFAULT 0 COMMENT '已学完小节数',
    plan_status          TINYINT     NOT NULL DEFAULT 0 COMMENT '0学习中 1已学完 2已过期',
    create_time          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_course (user_id, course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='我的课表';

DROP TABLE IF EXISTS learning_record;
CREATE TABLE learning_record (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    user_id      BIGINT   NOT NULL COMMENT '用户id',
    lesson_id    BIGINT   NOT NULL COMMENT '课表id',
    section_id   BIGINT   NOT NULL COMMENT '小节id',
    section_type TINYINT  NOT NULL DEFAULT 1 COMMENT '1视频 2考试 3文档',
    moment       INT      NOT NULL DEFAULT 0 COMMENT '已观看时长(秒)',
    finished     TINYINT  NOT NULL DEFAULT 0 COMMENT '0未学完 1已学完',
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_lesson_section (user_id, lesson_id, section_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='学习记录';

DROP TABLE IF EXISTS like_record;
CREATE TABLE like_record (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL COMMENT '点赞人',
    biz_type    TINYINT  NOT NULL COMMENT '1课程 2问答 3评论',
    biz_id      BIGINT   NOT NULL COMMENT '业务id',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_biz (user_id, biz_type, biz_id),
    KEY idx_biz (biz_type, biz_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='点赞记录';

DROP TABLE IF EXISTS like_count;
CREATE TABLE like_count (
    id       BIGINT  NOT NULL AUTO_INCREMENT,
    biz_type TINYINT NOT NULL COMMENT '业务类型',
    biz_id   BIGINT  NOT NULL COMMENT '业务id',
    liked_times INT  NOT NULL DEFAULT 0 COMMENT '点赞数',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz (biz_type, biz_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='点赞计数（由 Redis 批量落库）';

DROP TABLE IF EXISTS points_board;
CREATE TABLE points_board (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL COMMENT '用户id',
    points      INT      NOT NULL COMMENT '本次积分',
    type        TINYINT  NOT NULL COMMENT '1签到 2学习 3问答',
    description VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_time (user_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='积分流水';

DROP TABLE IF EXISTS coupon;
CREATE TABLE coupon (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    name             VARCHAR(64) NOT NULL COMMENT '券名称',
    type             TINYINT     NOT NULL COMMENT '1无门槛 2满减 3每满减 4折扣',
    threshold_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '使用门槛',
    discount_amount  DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '减免金额/每满减金额',
    discount_rate    DECIMAL(4, 2)  NOT NULL DEFAULT 10 COMMENT '折扣率(如9.5)',
    per_amount       DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '每满多少',
    total_num        INT         NOT NULL DEFAULT 0 COMMENT '发行量',
    remain_num       INT         NOT NULL DEFAULT 0 COMMENT '剩余量',
    begin_time       DATETIME    NOT NULL,
    end_time         DATETIME    NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='优惠券模板';

DROP TABLE IF EXISTS user_coupon;
CREATE TABLE user_coupon (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL,
    coupon_id   BIGINT   NOT NULL,
    status      TINYINT  NOT NULL DEFAULT 0 COMMENT '0未使用 1已使用 2已过期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_coupon (user_id, coupon_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户优惠券';

-- =========================================================
-- 新增模块：学习笔记 / 连续打卡 / 成就徽章
-- =========================================================

DROP TABLE IF EXISTS study_note;
CREATE TABLE study_note (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL COMMENT '用户',
    lesson_id    BIGINT       NOT NULL COMMENT '课表id',
    section_id   BIGINT       NOT NULL COMMENT '小节id',
    content      VARCHAR(1000) NOT NULL COMMENT '笔记内容',
    is_public    TINYINT      NOT NULL DEFAULT 0 COMMENT '0私密 1公开',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_public_time (is_public, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='学习笔记';

DROP TABLE IF EXISTS streak;
CREATE TABLE streak (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    user_id      BIGINT   NOT NULL,
    current_streak INT    NOT NULL DEFAULT 0 COMMENT '当前连续天数',
    best_streak  INT      NOT NULL DEFAULT 0 COMMENT '历史最长连续',
    last_date    DATE     NOT NULL COMMENT '最近打卡日期',
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='连续打卡';

DROP TABLE IF EXISTS badge;
CREATE TABLE badge (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(32)  NOT NULL COMMENT '徽章编码',
    name        VARCHAR(64)  NOT NULL COMMENT '徽章名称',
    description VARCHAR(255) NOT NULL COMMENT '获得条件',
    icon        VARCHAR(32)  NOT NULL DEFAULT 'star',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='徽章定义';

DROP TABLE IF EXISTS user_badge;
CREATE TABLE user_badge (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    user_id      BIGINT   NOT NULL,
    badge_id     BIGINT   NOT NULL,
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_badge (user_id, badge_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户已获得徽章';
