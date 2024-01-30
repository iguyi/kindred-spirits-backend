package com.guyi.kindredspirits.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JSON 工具类
 *
 * @author 孤诣
 */
public class JsonUtil {

    public final static Gson G = new Gson();

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
     * {
     * "superParentId": {
     * {"tag": "name", "weights": weights}
     * },
     * "superParentId": {
     * {"tag": "name", "weights": weights}
     * }
     * }
     */
    public static Map<String, List<TagPair>> jsonToTagPairMap(String jsonStr) {
        return G.fromJson(jsonStr, new TypeToken<Map<String, List<TagPair>>>() {
        }.getType());
    }

    /**
     * 通用 JSON 转 Java 对象
     *
     * @param json  - JSON 字符串
     * @param clazz - 返回值 Class
     * @param <R>   - 返回值类型
     * @return R 类型对象
     * @throws JsonSyntaxException JSON 格式不正确
     */
    public static <R> R fromJson(String json, Class<R> clazz) throws JsonSyntaxException {
        return G.fromJson(json, clazz);
    }

    /**
     * 更加通用的 JSON 转 Java 对象
     *
     * @param json    - JSON 字符串
     * @param typeOfT - 目标数据的泛型类型信息
     * @param <R>     - 返回值类型
     * @return 目标类型的对象
     * @throws JsonSyntaxException JSON 格式不正确
     */
    public static <R> R fromJson(String json, Type typeOfT) throws JsonSyntaxException {
        return G.fromJson(json, typeOfT);
    }

}
