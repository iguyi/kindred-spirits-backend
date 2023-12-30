DROP TABLE IF EXISTS `chat`;
CREATE TABLE `chat`
(
    `id`          bigint(0)                                                     NOT NULL AUTO_INCREMENT COMMENT '聊天记录 id',
    `senderId`    bigint(0)                                                     NOT NULL COMMENT '消息发送者 id',
    `receiverId`  bigint(0)                                                     NULL     DEFAULT NULL COMMENT '消息接收者 id',
    `teamId`      bigint(0)                                                     NULL     DEFAULT NULL COMMENT '群聊时, 对应队伍的 id',
    `receiverIds` varchar(512)                                                  NULL     DEFAULT NULL COMMENT 'teamId 对应队伍的成员的 id 列表, 格式为 JSON',
    `chatContent` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '聊天内容',
    `chatType`    tinyint(0)                                                    NOT NULL COMMENT '聊天类型 1-私聊 2-群聊',
    `createTime`  datetime                                                      NULL     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`  datetime                                                      NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`    tinyint(0)                                                    NULL     DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天记录表'
  ROW_FORMAT = COMPACT;