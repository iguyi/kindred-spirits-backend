package com.guyi.kindredspirits.util.lock;

/**
 * @author 孤诣
 */
public interface LockCallback {

    /**
     * LockUtil 类中的回调方法, 用于编写任务逻辑
     */
    void execute();

}