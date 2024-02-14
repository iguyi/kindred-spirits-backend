SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 创建 'unread_message_num' 表
-- ----------------------------
DROP TABLE IF EXISTS `unread_message_num`;
CREATE TABLE `unread_message_num`
(
    `id`              bigint(0)                                                    NOT NULL AUTO_INCREMENT COMMENT 'id',
    `userId`          bigint(0)                                                    NOT NULL COMMENT '用户 id',
    `chatSessionName` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '聊天会话名称: 会话类型-用户id-好友/队伍id',
    `unreadNum`       int(0)                                                       NULL     DEFAULT 0 COMMENT '未读消息数',
    `createTime`      datetime(0)                                                  NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `updateTime`      datetime(0)                                                  NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    `isDelete`        tinyint(0)                                                   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `chatSessionName` (`chatSessionName`) USING BTREE COMMENT '聊天会话名称具有唯一性'
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '未读聊天记录统计表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- updateTime 字段的触发器
-- ----------------------------
DROP TRIGGER IF EXISTS `tr_unreadMessageNum_table_updateTime`;
delimiter ;;
CREATE TRIGGER `tr_unreadMessageNum_table_updateTime`
    BEFORE UPDATE
    ON `unread_message_num`
    FOR EACH ROW
BEGIN
    SET NEW.updateTime = NOW();
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
