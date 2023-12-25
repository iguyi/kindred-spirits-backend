package com.guyi.kindredspirits.model.enums;

/**
 * Friend(好友表) relationStatus 字段的枚举值
 *
 * @author 孤诣
 */
public enum FriendRelationStatusEnum {

    /**
     * 正常好友
     */
    NORMAL(0, "正常好友"),

    /**
     * activeUser 删除了 passiveUser
     */
    ACTIVE_DELETE(1, "activeUser 删除了 passiveUser"),

    /**
     * passiveUserId 删除了 activeUserId
     */
    PASSIVE_DELETE(2, "passiveUserId 删除了 activeUserId"),

    /**
     * activeUserId 拉黑 passiveUserId
     */
    ACTIVE_HATE(3, "activeUserId 拉黑 passiveUserId"),

    /**
     * passiveUserId 拉黑 activeUserId
     */
    PASSIVE_HATE(4, "passiveUserId 拉黑 activeUserId"),

    /**
     * passiveUserId 和 activeUserId 互相删除
     */
    ALL_DELETE(5, "互相删除"),

    /**
     * passiveUserId 和 activeUserId 互相拉黑
     */
    ALL_HATE(6, "互相拉黑");

    private int value;
    private String text;

    public static FriendRelationStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        FriendRelationStatusEnum[] values = FriendRelationStatusEnum.values();
        for (FriendRelationStatusEnum friendRelationStatusEnum : values) {
            if (friendRelationStatusEnum.getValue() == value) {
                return friendRelationStatusEnum;
            }
        }
        return null;
    }

    FriendRelationStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
