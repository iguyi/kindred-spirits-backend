package com.guyi.kindredspirits.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * 算法工具测试类
 */
public class AlgorithmUtilTest {

    /**
     * 测试最短编辑距离算法 - 集合
     */
    @Test
    public void testMinDistanceList() {
        List<String> list1 = Arrays.asList("Java", "大四", "男");
        List<String> list2 = Arrays.asList("Java", "大四", "女");
        List<String> list3 = Arrays.asList("Java", "大三", "女");
        List<String> list4 = Arrays.asList("Python", "大三", "男");

        int value1 = AlgorithmUtil.minDistance(list1, list2);  // 1
        int value2 = AlgorithmUtil.minDistance(list1, list3);  // 2
        int value3 = AlgorithmUtil.minDistance(list1, list4);  // 2
        System.out.println(value1 + " " + value2 + " " + value3);
    }

}
