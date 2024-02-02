package com.guyi.kindredspirits.common.contant;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 自定义线程池
 *
 * @author 孤诣
 */
public interface ThreadPoolConstant {

    /**
     * 用于缓存过期时, 重新构建缓存
     */
    ExecutorService RECREATION_CACHE = new ThreadPoolExecutor(
            60,
            100,
            5,
            TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(10000),
            new ThreadFactoryBuilder().setNameFormat("缓存更新-%d").build()
    );

}
