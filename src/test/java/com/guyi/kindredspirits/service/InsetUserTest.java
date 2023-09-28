package com.guyi.kindredspirits.service;

import com.guyi.kindredspirits.mapper.UserMapper;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;

@SpringBootTest
public class InsetUserTest {
    @Resource
    private UserMapper userMapper;

    /**
     * 批量插入用户
     */
    @Test
    public void doInsertUsers() {
        StopWatch stopWatch = new StopWatch();  // 计时工具
        stopWatch.start();
        final int INSERT_NUMBER = 1000;
        for (int i = 0; i < INSERT_NUMBER; i++) {}
        stopWatch.stop();
        long totalTimeMillis = stopWatch.getTotalTimeMillis();
        System.out.println("耗时: " + totalTimeMillis);
    }
}
