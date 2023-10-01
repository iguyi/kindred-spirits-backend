DROP TABLE IF EXISTS `user_team`;
CREATE TABLE `user_team`
(
    `id`         bigint(0)   NOT NULL AUTO_INCREMENT COMMENT 'id',
    `userId`     bigint(0)   NOT NULL COMMENT '队伍成员 id',
    `teamId`     bigint(0)   NOT NULL COMMENT '队伍 id',
    `joinTime`   datetime(0) NULL COMMENT '加入时间',
    `createTime` datetime(0) NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `updateTime` datetime(0) NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    `isDelete`   tinyint(0)  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户-队伍关系表'
  ROW_FORMAT = Dynamic;