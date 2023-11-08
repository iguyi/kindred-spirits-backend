package com.guyi.kindredspirits.util;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JSON 工具类
 *
 * @author 张仕恒
 */
public class JsonUtil {

    private final static Gson G = new Gson();

    private JsonUtil() {
    }

    /**
     * 用户标签字段的值转为集合
     *
     * @param tags - user 表中的 tags 字段对应的值
     * @return tags 对应的集合
     */
    public static Set<String> tagsToSet(String tags) {
        return G.fromJson(tags, new TypeToken<Set<String>>() {
        }.getType());
    }

    /**
     * 用户标签字段的值转为列表
     *
     * @param tags - user 表中的 tags 字段对应的值
     * @return tags 对应的集合
     */
    public static List<String> tagsToList(String tags) {
        return G.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
    }

    public static Set<TagPair> jsonToTagPairSet(String jsonStr) {
        return G.fromJson(jsonStr, new TypeToken<Set<TagPair>>() {
        }.getType());
    }

    /**
     * 用于将 user 表的 tags 字段的 JSON 字符串转为 Map<String, List<TagPair>>
     * tags 字段存储数据的格式如下:
     *  {
     *      "superParentId": {
     *          {"tag": "name", "weights": weights
     *      },
     *      "superParentId": {
     *          {"tag": "name", "weights": weights
     *      }
     *  }
     */
    public static Map<String, List<TagPair>> jsonToTagPairMap(String jsonStr) {
        return G.fromJson(jsonStr, new TypeToken<Map<String, List<TagPair>>>() {
        }.getType());
    }
}
