package com.guyi.kindredspirits.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Set;

/**
 * JSON 工具类
 */
public class JsonUtil {

    final static Gson gson = new Gson();

    private JsonUtil() {
    }

    /**
     * 用户标签字段的值转为集合
     *
     * @param tags - user 表中的 tags 字段对应的值
     * @return tags 对应的集合
     */
    public static Set<String> tagsToSet(String tags) {
        return gson.fromJson(tags, new TypeToken<Set<String>>() {
        }.getType());
    }

    /**
     * 用户标签字段的值转为列表
     *
     * @param tags - user 表中的 tags 字段对应的值
     * @return tags 对应的集合
     */
    public static List<String> tagsToList(String tags) {
        return gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
    }
}
