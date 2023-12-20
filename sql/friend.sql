DROP TABLE IF EXISTS `friend`;
CREATE TABLE `friend`
(
    `id`             bigint(0)   NOT NULL AUTO_INCREMENT COMMENT 'id',
    `activeUserId`   bigint(0)   NOT NULL COMMENT 'activeUser 向 passiveUser 发出好友申请',
    `passiveUserId`  bigint(0)   NOT NULL COMMENT 'passiveUser 同意 activeUser 的好友申请',
    `relationStatus` int(0)      NOT NULL COMMENT '关系状态:
            0-正常好友
            1-activeUserId 删除了 passiveUserId
            2-passiveUserId 删除了 activeUserId
            3 - activeUserId 拉黑 passiveUserId
            4 - passiveUserId 拉黑 activeUserId
        说明：状态 3 可以变为 状态1; 状态 4 可以变为 状态 3',
    `createTime`     datetime(0) NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `updateTime`     datetime(0) NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    `isDelete`       tinyint(0)  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '好友表'
  ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;