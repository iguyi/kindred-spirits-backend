package com.guyi.kindredspirits.common.contant;

import java.util.concurrent.TimeUnit;

/**
 * 标签常量
 *
 * @author 孤诣
 */
public interface TagConstant {

    /**
     * 标签缓存过期时间
     */
    long TAG_CACHE_TIMEOUT = 20L;

    /**
     * 标签缓存时间的统一单位
     */
    TimeUnit UNIT = TimeUnit.HOURS;

}
