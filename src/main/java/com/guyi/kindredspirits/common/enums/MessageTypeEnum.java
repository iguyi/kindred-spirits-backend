package com.guyi.kindredspirits.common.enums;

/**
 * 消息类型枚举
 *
 * @author 孤诣
 */
public enum MessageTypeEnum {

    /**
     * 好友申请通过、入队申请通过等
     */
    SYSTEM_MESSAGE(0, "系统消息"),

    /**
     * 好友申请、入队申请等
     */
    VERIFY_MESSAGE(1, "验证消息"),

    /**
     * 需要系统处理的消息, 具体待定
     */
    NOTICE_MESSAGE(2, "消息通知");

    public static MessageTypeEnum getEnumByType(Integer type) {
        if (type == null) {
            return null;
        }
        MessageTypeEnum[] messageTypeEnums = MessageTypeEnum.values();
        for (MessageTypeEnum messageTypeEnum : messageTypeEnums) {
            if (messageTypeEnum.getType().equals(type)) {
                return messageTypeEnum;
            }
        }
        return null;
    }

    private Integer type;
    private String text;

    MessageTypeEnum(int type, String text) {
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
