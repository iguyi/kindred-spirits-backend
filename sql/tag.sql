DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag`
(
    `id`         bigint(0)                                                     NOT NULL AUTO_INCREMENT COMMENT 'id',
    `tagName`    varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL     DEFAULT NULL COMMENT '标签名称',
    `userId`     bigint(0)                                                     NULL     DEFAULT NULL COMMENT '用户 id',
    `parentId`   bigint(0)                                                     NULL     DEFAULT NULL COMMENT '父标签 id',
    `isPatent`   tinyint(0)                                                    NULL     DEFAULT NULL COMMENT '父标签?: 0-不是 1-是',
    `createTime` datetime(0)                                                   NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `updateTime` datetime(0)                                                   NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    `isDelete`   tinyint(0)                                                    NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX ```unique_tagName``` (`tagName`) USING BTREE,
    INDEX ```index_userId``` (`userId`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '标签表'
  ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;