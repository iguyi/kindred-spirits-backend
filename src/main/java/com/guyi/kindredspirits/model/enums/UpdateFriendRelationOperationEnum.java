package com.guyi.kindredspirits.model.enums;

/**
 * 更新好友关系的操作类型枚举
 *
 * @author 孤诣
 */
public enum UpdateFriendRelationOperationEnum {

    /**
     * 删除操作
     */
    DELETE(1, "删除好友操作"),

    /**
     * 拉黑操作
     */
    HATE(2, "拉黑");

    private final int value;
    private final String text;

    UpdateFriendRelationOperationEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public static UpdateFriendRelationOperationEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        UpdateFriendRelationOperationEnum[] values = UpdateFriendRelationOperationEnum.values();
        for (UpdateFriendRelationOperationEnum updateFriendRelationOperationEnum : values) {
            if (updateFriendRelationOperationEnum.getValue() == value) {
                return updateFriendRelationOperationEnum;
            }
        }
        return null;
    }

    public int getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

}
