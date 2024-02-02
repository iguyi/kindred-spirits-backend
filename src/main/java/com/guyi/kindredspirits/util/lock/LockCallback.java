package com.guyi.kindredspirits.util.lock;

/**
 * @param <T> 任务执行结果的类型
 * @author 孤诣
 */
public interface LockCallback<T> {

    /**
     * LockUtil 类中的回调方法, 用于编写任务逻辑
     *
     * @return 处理结果
     */
    T execute();

}