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
-- 相关数据
-- ----------------------------
INSERT INTO `tag` VALUES (1, '方向', 1, 1, NULL, 100000, 100000, '2023-11-11 18:57:08', '2023-11-11 18:57:08', 0);
INSERT INTO `tag` VALUES (2, 'Java', 1, 0, 1, 10000, 110000, '2023-11-11 18:58:42', '2023-11-11 18:58:42', 0);
INSERT INTO `tag` VALUES (3, 'Python', 1, 0, 1, 20000, 120000, '2023-11-11 18:58:42', '2023-11-11 18:58:42', 0);
INSERT INTO `tag` VALUES (4, 'C++', 1, 0, 1, 30000, 130000, '2023-11-11 18:58:42', '2023-11-11 18:58:42', 0);
INSERT INTO `tag` VALUES (5, '身份', 1, 1, NULL, 90000, 90000, '2023-11-11 19:00:09', '2023-11-11 19:00:09', 0);
INSERT INTO `tag` VALUES (6, '大四', 1, 0, 5, 1000, 91000, '2023-11-11 19:02:45', '2023-11-11 19:02:45', 0);
INSERT INTO `tag` VALUES (7, '大三', 1, 0, 5, 2000, 92000, '2023-11-11 19:02:45', '2023-11-11 19:02:45', 0);
INSERT INTO `tag` VALUES (8, '大二', 1, 0, 5, 3000, 93000, '2023-11-11 19:02:45', '2023-11-11 19:02:45', 0);
INSERT INTO `tag` VALUES (9, '爱好', 1, 1, NULL, 50000, 50000, '2023-11-11 19:03:44', '2023-11-11 19:03:44', 0);
INSERT INTO `tag` VALUES (10, '乒乓球', 1, 0, 9, 1000, 51000, '2023-11-11 19:05:21', '2023-11-11 19:05:21', 0);
INSERT INTO `tag` VALUES (11, '篮球', 1, 0, 9, 2000, 52000, '2023-11-11 19:05:21', '2023-11-11 19:05:21', 0);
INSERT INTO `tag` VALUES (12, '排球', 1, 0, 9, 3000, 53000, '2023-11-11 19:05:21', '2023-11-11 19:05:21', 0);
INSERT INTO `tag` VALUES (13, '状态', 1, 1, NULL, 80000, 80000, '2023-11-11 19:06:13', '2023-11-11 19:06:13', 0);
INSERT INTO `tag` VALUES (14, '乐观', 1, 0, 13, 6000, 86000, '2023-11-11 19:09:07', '2023-11-11 19:09:07', 0);
INSERT INTO `tag` VALUES (15, '冷静', 1, 0, 13, 6000, 86000, '2023-11-11 19:09:07', '2023-11-11 19:09:07', 0);
INSERT INTO `tag` VALUES (16, '自信', 1, 0, 13, 6000, 86000, '2023-11-11 19:09:07', '2023-11-11 19:09:07', 0);
INSERT INTO `tag` VALUES (17, '沮丧', 1, 0, 13, 2000, 82000, '2023-11-11 19:09:07', '2023-11-11 19:09:07', 0);
INSERT INTO `tag` VALUES (18, 'C', 1, 0, 1, 40000, 140000, '2024-02-15 20:56:13', '2024-02-15 20:56:13', 0);
INSERT INTO `tag` VALUES (19, 'PHP', 1, 0, 1, 50000, 150000, '2024-02-15 20:56:13', '2024-02-15 20:56:13', 0);
INSERT INTO `tag` VALUES (20, 'GO', 1, 0, 1, 60000, 160000, '2024-02-15 20:57:39', '2024-02-15 20:57:39', 0);
INSERT INTO `tag` VALUES (21, 'C#', 1, 0, 1, 70000, 170000, '2024-02-15 20:57:39', '2024-02-15 20:57:39', 0);
INSERT INTO `tag` VALUES (22, 'HTML', 1, 0, 1, 0, 100000, '2024-02-15 20:57:39', '2024-02-15 20:57:39', 0);
INSERT INTO `tag` VALUES (23, 'JavaScript', 1, 0, 1, 90000, 190000, '2024-02-15 20:57:39', '2024-02-15 20:57:39', 0);
INSERT INTO `tag` VALUES (24, 'TypeScript', 1, 0, 1, 95000, 195000, '2024-02-15 20:57:39', '2024-02-15 20:57:39', 0);
INSERT INTO `tag` VALUES (25, 'SQL', 1, 0, 1, 0, 100000, '2024-02-15 20:57:39', '2024-02-15 20:57:39', 0);
INSERT INTO `tag` VALUES (26, 'Shell', 1, 0, 1, 0, 100000, '2024-02-15 20:57:39', '2024-02-15 20:57:39', 0);
INSERT INTO `tag` VALUES (27, '网络安全', 1, 0, 1, 100000, 200000, '2024-02-15 20:57:39', '2024-02-15 20:57:39', 0);
INSERT INTO `tag` VALUES (28, '人工智能', 1, 0, 1, 120000, 220000, '2024-02-15 20:57:39', '2024-02-15 20:57:39', 0);
INSERT INTO `tag` VALUES (29, '大一', 1, 0, 5, 4000, 94000, '2024-02-15 20:57:39', '2024-02-15 20:57:39', 0);
INSERT INTO `tag` VALUES (30, '高中', 1, 0, 5, 5000, 95000, '2024-02-15 20:57:39', '2024-02-15 20:57:39', 0);
INSERT INTO `tag` VALUES (31, '初中', 1, 0, 5, 6000, 96000, '2024-02-15 20:57:39', '2024-02-15 20:57:39', 0);
INSERT INTO `tag` VALUES (32, '小学', 1, 0, 5, 7000, 97000, '2024-02-15 20:57:39', '2024-02-15 20:57:39', 0);
INSERT INTO `tag` VALUES (33, '在职', 1, 0, 5, 8000, 98000, '2024-02-15 20:57:39', '2024-02-15 20:57:39', 0);
INSERT INTO `tag` VALUES (34, '自由人', 1, 0, 5, 9000, 99000, '2024-02-15 20:57:39', '2024-02-15 20:57:39', 0);
INSERT INTO `tag` VALUES (35, '编程', 1, 0, 9, 4000, 54000, '2024-02-15 20:57:39', '2024-02-15 20:57:39', 0);
INSERT INTO `tag` VALUES (36, '跑步', 1, 0, 9, 5000, 55000, '2024-02-15 20:57:39', '2024-02-15 20:57:39', 0);
INSERT INTO `tag` VALUES (37, '书法', 1, 0, 9, 6000, 56000, '2024-02-15 20:57:39', '2024-02-15 20:57:39', 0);
INSERT INTO `tag` VALUES (38, '绘画', 1, 0, 9, 7000, 57000, '2024-02-15 20:57:40', '2024-02-15 20:57:40', 0);
INSERT INTO `tag` VALUES (39, '玩游戏', 1, 0, 9, 8000, 58000, '2024-02-15 20:57:40', '2024-02-15 20:57:40', 0);
INSERT INTO `tag` VALUES (40, '看小说', 1, 0, 9, 9000, 59000, '2024-02-15 20:57:40', '2024-02-15 20:57:40', 0);
INSERT INTO `tag` VALUES (41, '找工作', 1, 0, 13, 4000, 84000, '2024-02-15 20:57:40', '2024-02-15 20:57:40', 0);

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
