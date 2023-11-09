DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`
(
    `id`           bigint(0)                                                      NOT NULL AUTO_INCREMENT COMMENT 'id',
    `userAccount`  varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NOT NULL COMMENT '用户账号',
    `username`     varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NULL     DEFAULT NULL COMMENT '用户昵称',
    `avatarUrl`    varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '用户头像',
    `gender`       tinyint(0)                                                     NULL     DEFAULT NULL COMMENT '性别',
    `userPassword` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NOT NULL COMMENT '密码',
    `userRole`     int(0)                                                         NOT NULL DEFAULT 0 COMMENT '用户角色',
    `tags`         varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL     DEFAULT NULL COMMENT '标签列，json 格式',
    `profile`      varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NULL     DEFAULT NULL COMMENT '个人简介',
    `phone`        varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NULL     DEFAULT NULL COMMENT '电话',
    `email`        varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NULL     DEFAULT NULL COMMENT '邮箱',
    `userStatus`   int(0)                                                         NOT NULL DEFAULT 0 COMMENT '用户状态',
    `isHot`        int(0)                                                         NOT NULL DEFAULT 0 COMMENT '是否为热点用户 0-不是 1-是',
    `createTime`   datetime(0)                                                    NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `updateTime`   datetime(0)                                                    NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    `isDelete`     tinyint(0)                                                     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表'
  ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;