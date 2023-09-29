package com.guyi.kindredspirits.service;

import com.guyi.kindredspirits.mapper.UserMapper;
import com.guyi.kindredspirits.model.domain.User;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@SpringBootTest
public class InsetUserTest {
    @Resource
    private UserMapper userMapper;

    @Resource
    private UserService userService;

    /**
     * 自定义线程池
     * 线程存活时间: 1 分钟
     * 任务处理策略: 默认策略, 拒绝溢出的任务
     */
    private final ExecutorService executorService = new ThreadPoolExecutor(24,
            1000,
            10000,
            TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(10000));

    /**
     * 批量插入用户
     */
    @Test
    public void doInsertUsers() {
        StopWatch stopWatch = new StopWatch();  // 计时工具
        stopWatch.start();
        final int INSERT_NUMBER = 1000;
        for (int i = 0; i < INSERT_NUMBER; i++) {
        }
        stopWatch.stop();
        long totalTimeMillis = stopWatch.getTotalTimeMillis();
        System.out.println("耗时: " + totalTimeMillis);
    }

    /**
     * 多线程异步、批量插入
     */
    @Test
    public void doConcurrencyInsertUsers() {
        StopWatch stopWatch = new StopWatch();  // 计时工具
        stopWatch.start();
        final int INSERT_NUMBER = 100000;  // 10 万条数据


        // 数据分 10 组
        int j = 0;
        int batchSize = 10000;  // 每批插入的数据量
        User user;
        List<CompletableFuture<Void>> futureList = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < INSERT_NUMBER / batchSize; i++) {
            List<User> userList = new ArrayList<>();
            do {
                j++;
                user = new User();
                // todo 生成假数据的方法
                userList.add(user);
            } while (j % batchSize != 0);
            // 执行异步操作
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                userService.saveBatch(userList, userList.size());
            }, executorService);
            futureList.add(future);
        }
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();  // 确保所有异步任务执行完成才会统计时长
        stopWatch.stop();
        long totalTimeMillis = stopWatch.getTotalTimeMillis();
        System.out.println("耗时: " + totalTimeMillis);
    }
}
