package com.guyi.kindredspirits.common.enums;

/**
 * 聊天类型枚举
 *
 * @author 孤诣
 */
public enum ChatTypeEnum {

    /**
     * 私聊
     */
    PRIVATE_CHAT(1, "私聊"),

    /**
     * 群聊(队内聊天)
     */
    GROUP_CHAT(2, "群聊");

    public static ChatTypeEnum getEnumByType(Integer type) {
        if (type == null) {
            return null;
        }
        ChatTypeEnum[] chatTypeEnums = ChatTypeEnum.values();
        for (ChatTypeEnum chatTypeEnum : chatTypeEnums) {
            if (chatTypeEnum.getType().equals(type)) {
                return chatTypeEnum;
            }
        }
        return null;
    }

    Integer type;
    String text;

    ChatTypeEnum(Integer type, String text) {
        this.type = type;
        this.text = text;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
