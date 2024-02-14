SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 创建 friend 表
-- ----------------------------
DROP TABLE IF EXISTS `friend`;
CREATE TABLE `friend`
(
    `id`             bigint(0)   NOT NULL AUTO_INCREMENT COMMENT 'id',
    `activeUserId`   bigint(0)   NOT NULL COMMENT 'activeUser 向 passiveUser 发出好友申请',
    `passiveUserId`  bigint(0)   NOT NULL COMMENT 'passiveUser 同意 activeUser 的好友申请',
    `relationStatus` int(0)      NOT NULL DEFAULT 0 COMMENT
        '关系状态:
            0 - 正常好友
            1 - activeUserId 删除了 passiveUserId
            2 - passiveUserId 删除了 activeUserId
            3 - activeUserId 拉黑 passiveUserId
            4 - passiveUserId 拉黑 activeUserId
        说明：状态3 可以变为 状态1; 状态4 可以变为 状态3',
    `createTime`     datetime(0) NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `updateTime`     datetime(0) NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    `isDelete`       tinyint(0)  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_activeUserId_passiveUserId` (`activeUserId`, `passiveUserId`) USING BTREE COMMENT '好友关系索引'
) ENGINE = InnoDB
  AUTO_INCREMENT = 10
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '好友表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- updateTime 字段的触发器
-- ----------------------------
DROP TRIGGER IF EXISTS `tr_friend_table_updateTime`;
delimiter ;;
CREATE TRIGGER `tr_friend_table_updateTime`
    BEFORE UPDATE
    ON `friend`
    FOR EACH ROW
BEGIN
    SET NEW.updateTime = NOW();
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
