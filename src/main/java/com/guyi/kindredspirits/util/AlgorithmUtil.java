package com.guyi.kindredspirits.util;

import org.springframework.data.util.Pair;

import java.util.*;

/**
 * 算法工具类
 *
 * @author 孤诣
 */
public class AlgorithmUtil {

    /**
     * 用户初始权值权值
     */
    private static final double USER_WEIGHT_INIT = 0.0;

    /**
     * 初始点积
     */
    private static final double DOT_PRODUCT_INIT = 0.0;

    /**
     * 0.00000001, 进行余弦相似度计算时, 分母出现 0 时将使用该值替换 0
     */
    private static final double EPSILON = 1e-8;

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

    /**
     * 余弦相似度
     *
     * @param currentUserTag - 当前用户的标签
     * @param otherUserTag   - 其他用户的标签
     * @return 相似度
     */
    public static double similarity(Map<String, List<Integer>> currentUserTag,
                                    Map<String, List<Integer>> otherUserTag) {
        // 当前用户权值
        double currentUserWeight = USER_WEIGHT_INIT;

        // 其他用户权值
        double otherUserWeight = USER_WEIGHT_INIT;

        // 点积
        double dotProduct = DOT_PRODUCT_INIT;

        // 用户的特征向量, 有各自标签的权重组合而成
        List<Double> currentUserVector = new ArrayList<>();
        List<Double> otherUserVector = new ArrayList<>();

        // 向量元素之和, 用于避免在两个用户的特征向量都是只有一个影响因素的情况下出现相似度恒等于 1 的问题
        double currentUserVectorSum = 0.0;
        double otherUserVectorSum = 0.0;

        // 根据用户标签生成特征向量
        Set<String> currentItemSuperTagIds = currentUserTag.keySet();
        for (String currentItemSuperTagId : currentItemSuperTagIds) {
            double currentUserVectorItem = calculateParentTagWeight(currentUserTag.get(currentItemSuperTagId));
            currentUserVector.add(currentUserVectorItem);
            currentUserVectorSum += currentUserVectorItem;

            if (!otherUserTag.containsKey(currentItemSuperTagId)) {
                otherUserVector.add(0.0);
                continue;
            }
            double otherUserVectorItem = calculateParentTagWeight(otherUserTag.get(currentItemSuperTagId));
            otherUserVector.add(otherUserVectorItem);
            otherUserVectorSum += otherUserVectorItem;
        }
        currentUserVector.add(currentUserVectorSum / currentUserVector.size());
        otherUserVector.add(otherUserVectorSum / otherUserVector.size());

        // 计算结果并返回
        int size = currentUserVector.size();
        for (int i = 0; i < size; i++) {
            Double currentUserVectorItem = currentUserVector.get(i);
            Double otherUserVectorItem = otherUserVector.get(i);
            // 根据比重调整计算参数
            double a = currentUserVectorItem * (currentUserVectorItem / currentUserVectorSum);
            double b = otherUserVectorItem * (otherUserVectorItem / otherUserVectorSum);
            dotProduct += (a * b);
            currentUserWeight += Math.pow(a, 2);
            otherUserWeight += Math.pow(b, 2);
        }
        return dotProduct / Math.max(Math.sqrt(currentUserWeight) * Math.sqrt(otherUserWeight), EPSILON);
    }

    /**
     * 余弦相似度
     *
     * @param currentUserTag - 当前用户的标签
     * @param otherUserTag   - 其他用户的标签
     * @return 相似度
     */
    public static double similarityPro(Map<String, List<Integer>> currentUserTag,
                                       Map<String, List<Integer>> otherUserTag) {
        /*
         当前用户的熵值, 等于当前用户的不同顶层标签的权值之和。
         顶层标签对于一个用户的权值 = 该用户拥有该顶层标签下的所有子标签权值之和 / 该用户拥有该顶层标签下的所有子标签权值的数目
         */
        double currentUserEntropy = 0.0;

        /*
         metaList 存储每一个分量的相似度。
         metaList 的每个元素: (相似度, 对应顶层标签总权值的平均权重)
         */
        List<Pair<Double, Double>> metaList = new ArrayList<>();

        Set<String> currentUserTagKey = currentUserTag.keySet();
        for (String key : currentUserTagKey) {
            // 获取当前用户的标签组
            List<Integer> currentUserTagValue = currentUserTag.get(key);

            // 计算当前标签组的熵
            double currentUserChildEntropy = 0.0;
            for (Integer weight : currentUserTagValue) {
                currentUserChildEntropy += weight;
            }
            currentUserEntropy += currentUserChildEntropy;

            // 【比较用户】未设置该组标签
            if (!otherUserTag.containsKey(key)) {
                metaList.add(Pair.of(0.0, currentUserChildEntropy));
                continue;
            }

            /*
            相似度计算过程中的相关数据
            currentUserWeight: 当前用户权值
            otherUserWeight: 其他用户权值
            dotProduct: 点积
             */
            double currentUserWeight = USER_WEIGHT_INIT;
            double otherUserWeight = USER_WEIGHT_INIT;
            double dotProduct = DOT_PRODUCT_INIT;

            // 当前【比较用户】存在该组标签, 则获取
            List<Integer> otherUserTagValue = otherUserTag.get(key);

            // 相关计算
            for (Integer value : currentUserTagValue) {
                if (otherUserTagValue.contains(value)) {
                    dotProduct += Math.pow(value, 2);
                    otherUserWeight += Math.pow(value, 2);
                }
                currentUserWeight += Math.pow(value, 2);
            }
            double result = dotProduct / Math.max(Math.sqrt(currentUserWeight) * Math.sqrt(otherUserWeight), EPSILON);

            // 保存计算结果
            metaList.add(Pair.of(result, currentUserChildEntropy));
        }

        // 最终相似度计算
        double finalSimilarity = 0.0;
        for (Pair<Double, Double> meta : metaList) {
            // 最终相似度 = sum(各组标签相似度 / 对应组的重要度)
            finalSimilarity += (meta.getFirst() * (meta.getSecond() / currentUserEntropy));
        }

        return Math.round(finalSimilarity * 100.0) / 100.0;
    }

    /**
     * 计算父标签权重
     *
     * @param tagWeights - 父标签下的子标签
     * @return 父标签权重
     */
    private static double calculateParentTagWeight(List<Integer> tagWeights) {
        return tagWeights.stream().mapToInt(Integer::intValue).average().orElse(USER_WEIGHT_INIT);
    }

}
