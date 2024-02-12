package com.guyi.kindredspirits.common.contant;

/**
 * 系统的一些基础常量
 *
 * @author 孤诣
 */
public interface BaseConstant {

    /**
     * 判断字符串中是否包含特殊字符的【正则表达式】的匹配模式, 主要用户验证密码是否有效
     */
    String VALID_PATTER = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？\\s]";

    /**
     * 心跳检测的消息 - 检测
     */
    String HEARTBEAT_PING = "PING";

    /**
     * 获取未消息数 - 反馈
     */
    String UNREAD_NUMS = "unread";

    /**
     * 心跳检测的消息 - 反馈
     */
    String HEARTBEAT_PONG = "PONG";

    /**
     * 获取锁失败后允许重试的次数
     */
    int RETRIES_MAX_NUMBER = 100;

}
