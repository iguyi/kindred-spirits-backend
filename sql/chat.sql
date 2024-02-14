SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 创建 'chat' 表
-- ----------------------------
DROP TABLE IF EXISTS `chat`;
CREATE TABLE `chat`
(
    `id`          bigint(0)                                                     NOT NULL AUTO_INCREMENT COMMENT '聊天记录 id',
    `senderId`    bigint(0)                                                     NOT NULL COMMENT '消息发送者 id',
    `receiverId`  bigint(0)                                                     NULL     DEFAULT NULL COMMENT '消息接收者 id',
    `teamId`      bigint(0)                                                     NULL     DEFAULT NULL COMMENT '群聊时, 对应队伍的 id',
    `receiverIds` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL     DEFAULT NULL COMMENT 'teamId 对应队伍的成员的 id 列表, 格式为 JSON',
    `chatContent` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '聊天内容',
    `chatType`    tinyint(0)                                                    NOT NULL COMMENT '聊天类型 1-私聊 2-群聊',
    `createTime`  datetime(0)                                                   NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `updateTime`  datetime(0)                                                   NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    `isDelete`    tinyint(0)                                                    NULL     DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_senderId_receiverId` (`senderId`, `receiverId`) USING BTREE COMMENT '私聊聊天记录索引',
    INDEX `idx_teamId` (`teamId`) USING BTREE COMMENT '队伍聊天记录索引'
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天记录表'
  ROW_FORMAT = Compact;

-- ----------------------------
-- updateTime 字段的触发器
-- ----------------------------
DROP TRIGGER IF EXISTS `tr_chat_table_updateTime`;
delimiter ;;
CREATE TRIGGER `tr_chat_table_updateTime`
    BEFORE UPDATE
    ON `chat`
    FOR EACH ROW
BEGIN
    SET NEW.updateTime = NOW();
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
