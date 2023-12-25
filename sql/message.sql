CREATE TABLE `message`
(
    `id`          bigint(0)   NOT NULL AUTO_INCREMENT COMMENT 'id',
    `senderId`    bigint(0)   NOT NULL COMMENT '消息发送者 id',
    `receiverId`  bigint(0)   NOT NULL COMMENT '消息接受者 id',
    `messageType` int(0)      NOT NULL DEFAULT 0 COMMENT ' 消息类型:
        0 - 系统消息(好友申请通过、入队申请通过等)
        1 - 验证消息(比如好友申请、入队申请等)
        2 - 消息通知(需要系统处理的消息)',
    `messageBody` varchar(255)         DEFAULT '' COMMENT '消息主体(内容)',
    `processed`   tinyint(0) COMMENT '消息是否已处理: 0-未处理, 1-已处理(不需要处理的消息，值为 NaN)',
    `createTime`  datetime(0) NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `updateTime`  datetime(0) NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    `isDelete`    tinyint(0)  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '消息表(区别与聊天记录表)'
  ROW_FORMAT = Dynamic;
DROP TABLE IF EXISTS `message`;

SET FOREIGN_KEY_CHECKS = 1;

DELIMITER \$\$
CREATE TRIGGER tr_message_table_updateTime
    BEFORE UPDATE
    ON message
    FOR EACH ROW
BEGIN
    SET NEW.updateTime = NOW();
END\$\$
DELIMITER ;