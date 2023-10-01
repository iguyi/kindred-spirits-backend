DROP TABLE IF EXISTS `team`;
CREATE TABLE `team`
(
    `id`          bigint(0)                                                      NOT NULL AUTO_INCREMENT COMMENT 'id',
    `name`        varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NOT NULL COMMENT '队伍名称',
    `description` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '队伍描述',
    `maxNum `     int(0)                                                         NOT NULL DEFAULT 1 COMMENT '队伍最大人数',
    `expireTime`  datetime(0)                                                    NULL COMMENT '过期时间',
    `userId`      bigint(0)                                                      NOT NULL COMMENT '创建人 id',
    `leaderId`    bigint(0)                                                      NOT NULL COMMENT '队长 id',
    `status`      int(0)                                                         NOT NULL DEFAULT 0 COMMENT '0 - 公开，1 - 私有，2 - 加密',
    `password`    varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NULL COMMENT '密码',
    `createTime`  datetime(0)                                                    NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `updateTime`  datetime(0)                                                    NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    `isDelete`    tinyint(0)                                                     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '队伍表'
  ROW_FORMAT = Dynamic;