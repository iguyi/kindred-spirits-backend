package com.guyi.kindredspirits.service;

import com.guyi.kindredspirits.model.domain.User;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户服务测试
 */
@SpringBootTest
class UserServiceTest {

    @Resource
    private UserService userService;

    /**
     * 测试：根据标签搜索用户 -- SQL 查询
     */
    @Test
    void testSqlSearchUsersByTags() {
        List<String> tagNameList = Arrays.asList("Java", "Python");
        long timeMillis1 = System.currentTimeMillis();
        List<User> userList = userService.searchUsersByTagsBySQL(tagNameList);
        long timeMillis2 = System.currentTimeMillis();
        System.out.println("耗时: " + (timeMillis2 - timeMillis1));
        assertNotNull(userList);
    }

    /**
     * 测试：根据标签搜索用户 -- 内存查询
     */
    @Test
    void testMemorySearchUsersByTags() {
        List<String> tagNameList = Arrays.asList("Java", "Python");
        long timeMillis1 = System.currentTimeMillis();
        List<User> userList = userService.searchUsersByTags(tagNameList);
        long timeMillis2 = System.currentTimeMillis();
        System.out.println("耗时: " + (timeMillis2 - timeMillis1));
        assertNotNull(userList);
    }
}