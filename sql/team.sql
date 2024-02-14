SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 创建 team 表
-- ----------------------------
DROP TABLE IF EXISTS `team`;
CREATE TABLE `team`
(
    `id`          bigint(0)                                                      NOT NULL AUTO_INCREMENT COMMENT 'id',
    `name`        varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NOT NULL COMMENT '队伍名称',
    `avatarUrl`   varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NULL     DEFAULT NULL COMMENT '头像',
    `description` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL     DEFAULT NULL COMMENT '队伍描述',
    `teamLink`    varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NULL     DEFAULT NULL COMMENT '队伍邀请链接',
    `maxNum`      int(0)                                                         NOT NULL DEFAULT 3 COMMENT '队伍最大人数',
    `num`         int(0)                                                         NOT NULL DEFAULT 1 COMMENT '队伍已有人数',
    `expireTime`  datetime(0)                                                    NULL     DEFAULT NULL COMMENT '过期时间',
    `userId`      bigint(0)                                                      NOT NULL COMMENT '创建人 id',
    `leaderId`    bigint(0)                                                      NOT NULL COMMENT '队长 id',
    `status`      int(0)                                                         NOT NULL DEFAULT 0 COMMENT '0 - 公开，1 - 私有，2 - 加密',
    `password`    varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NULL     DEFAULT NULL COMMENT '密码',
    `createTime`  datetime(0)                                                    NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `updateTime`  datetime(0)                                                    NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    `isDelete`    tinyint(0)                                                     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `idx_team_link` (`teamLink`) USING BTREE COMMENT '队伍邀请链接索引',
    INDEX `idx_name` (`name`) USING BTREE COMMENT '队伍名称索引'
) ENGINE = InnoDB
  AUTO_INCREMENT = 8
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '队伍表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- updateTime 字段触发器
-- ----------------------------
DROP TRIGGER IF EXISTS `tr_team_table_updateTime`;
delimiter ;;
CREATE TRIGGER `tr_team_table_updateTime`
    BEFORE UPDATE
    ON `team`
    FOR EACH ROW
BEGIN
    SET NEW.updateTime = NOW();
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
