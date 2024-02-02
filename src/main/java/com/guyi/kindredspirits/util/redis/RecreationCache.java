package com.guyi.kindredspirits.util.redis;

import com.guyi.kindredspirits.common.contant.ThreadPoolConstant;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * 重新构建缓存
 *
 * @author 孤诣
 */
public class RecreationCache {

    private RecreationCache(){}

    public static void recreation(Runnable runnable) {
        ThreadPoolConstant.RECREATION_CACHE.execute(runnable);
    }

    public static <T> T recreation(Callable<T> task) throws ExecutionException, InterruptedException {
        Future<T> submit = ThreadPoolConstant.RECREATION_CACHE.submit(task);
        return submit.get();
    }

}
