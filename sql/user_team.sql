SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 创建 user_team 表
-- ----------------------------
DROP TABLE IF EXISTS `user_team`;
CREATE TABLE `user_team`
(
    `id`         bigint(0)   NOT NULL AUTO_INCREMENT COMMENT 'id',
    `userId`     bigint(0)   NOT NULL COMMENT '队伍成员 id',
    `teamId`     bigint(0)   NOT NULL COMMENT '队伍 id',
    `joinTime`   datetime(0) NULL     DEFAULT NULL COMMENT '加入时间',
    `createTime` datetime(0) NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `updateTime` datetime(0) NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    `isDelete`   tinyint(0)  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_userId_teamId` (`userId`, `teamId`) USING BTREE COMMENT '队伍成员与队伍关系索引'
) ENGINE = InnoDB
  AUTO_INCREMENT = 18
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户-队伍关系表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- updateTime 字段的触发器
-- ----------------------------
DROP TRIGGER IF EXISTS `tr_user_team_table_updateTime`;
delimiter ;;
CREATE TRIGGER `tr_user_team_table_updateTime`
    BEFORE UPDATE
    ON `user_team`
    FOR EACH ROW
BEGIN
    SET NEW.updateTime = NOW();
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
