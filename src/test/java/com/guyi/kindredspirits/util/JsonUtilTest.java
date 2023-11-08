package com.guyi.kindredspirits.util;

import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * JsonUtil 测试类
 */
public class JsonUtilTest {
    @Test
    public void testJsonToTagPairMap() {
        String jsonStr = "{\n"
                    + "\"1\": [\n"
                        + "{\"tag\": \"Java\", \"weights\": 151000}, \n"
                        + "{\"tag\": \"Python\", \"weights\": 152000}\n"
                    + "],\n"
                    + "\"6\": [\n"
                        + "{\"tag\": \"大四\", \"weights\": 91000}\n"
                    + "],\n"
                    + "\"9\": [\n"
                        + "{\"tag\": \"乒乓球\", \"weights\": 51000}\n"
                    + "]\n"
                + "}";
        Map<String, List<TagPair>> stringListMap = JsonUtil.jsonToTagPairMap(jsonStr);
        System.out.println(stringListMap.toString());
    }
}
