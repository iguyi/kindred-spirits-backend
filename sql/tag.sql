SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 创建 tag 表
-- ----------------------------
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag`
(
    `id`         bigint(0)                                                    NOT NULL AUTO_INCREMENT COMMENT 'id',
    `tagName`    varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL     DEFAULT NULL COMMENT '标签名称',
    `userId`     bigint(0)                                                    NULL     DEFAULT NULL COMMENT '标签创建者 id',
    `isParent`   tinyint(0)                                                   NULL     DEFAULT NULL COMMENT '父标签?: 0-不是 1-是',
    `parentId`   bigint(0)                                                    NULL     DEFAULT NULL COMMENT '父标签 id',
    `baseWeight` int(0)                                                       NULL     DEFAULT NULL COMMENT '基本权值',
    `weights`    int(0)                                                       NULL     DEFAULT NULL COMMENT '父标签权值+自己的基本权值',
    `createTime` datetime(0)                                                  NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `updateTime` datetime(0)                                                  NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    `isDelete`   tinyint(0)                                                   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX ```unique_tagName``` (`tagName`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 18
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '标签表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- updateTime 字段的触发器
-- ----------------------------
DROP TRIGGER IF EXISTS `tr_my_table_updateTime`;
delimiter ;;
CREATE TRIGGER `tr_my_table_updateTime`
    BEFORE UPDATE
    ON `tag`
    FOR EACH ROW
BEGIN
    SET NEW.updateTime = NOW();
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;

# todo 补充数据