package com.guyi.kindredspirits.util;

import java.util.*;

/**
 * 算法工具类
 *
 * @author 孤诣
 */
public class AlgorithmUtil {

    /**
     * 最短编辑距离算法 - 标签
     *
     * @param current - 当前用户的标签列表
     * @param other   - 其他用户的标签列表
     * @return 当前用户和其他用户的相似度
     */
    public static int minDistance(List<String> current, List<String> other) {
        int n = current.size();
        int m = other.size();

        if (n * m == 0) {
            return n + m;
        }

        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i < n + 1; i++) {
            d[i][0] = i;
        }

        for (int j = 0; j < m + 1; j++) {
            d[0][j] = j;
        }

        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < m + 1; j++) {
                int left = d[i - 1][j] + 1;
                int down = d[i][j - 1] + 1;
                int leftDown = d[i - 1][j - 1];
                if (!Objects.equals(current.get(i - 1), other.get(j - 1))) {
                    leftDown += 1;
                }
                d[i][j] = Math.min(left, Math.min(down, leftDown));
            }
        }
        return d[n][m];
    }

    public static double similarity(Map<String, List<Integer>> currentUserTag,
                                    Map<String, List<Integer>> otherUserTag) {
        // 当前用户权值
        double currentUserWeight = 0.0;

        // 其他用户权值
        double otherUserWeight = 0.0;

        // 点积
        double dotProduct = 0.0;

        Set<String> otherItemSuperTagIds = otherUserTag.keySet();
        for (String otherItemSuperTagId: otherItemSuperTagIds) {
            if (!currentUserTag.containsKey(otherItemSuperTagId)) {
                continue;
            }

            List<Integer> currentItemTagWeightsList = currentUserTag.get(otherItemSuperTagId);
            List<Integer> otherItemTagWeightsList = otherUserTag.get(otherItemSuperTagId);

            // 计算(顶层)父标签权值 - 点积
            double currentUserProduct = calculateParentTagWeight(currentItemTagWeightsList);
            double otherUserValuesProduct = calculateParentTagWeight(otherItemTagWeightsList);
            dotProduct += currentUserProduct * otherUserValuesProduct;

            // 计算用户权值
            currentUserWeight += Math.pow(currentUserProduct, 2);
            otherUserWeight += Math.pow(otherUserValuesProduct, 2);
        }

        return dotProduct / Math.max(Math.sqrt(currentUserWeight) * Math.sqrt(otherUserWeight), 0.00000001);
    }

    /**
     * 计算父标签权重
     *
     * @param tagWeights - 父标签下的子标签
     * @return 父标签权重
     */
    private static double calculateParentTagWeight(List<Integer> tagWeights) {
        // 初始权重
        double weight = 0.0;

        for (Integer tagWeight : tagWeights) {
            weight += tagWeight;
        }

        return weight / tagWeights.size();
    }
}
