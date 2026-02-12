package com.example.chatroom.protocol;

public enum MessageType {
    LOGIN,
    LOGIN_ACK,
    CHAT,
    BROADCAST,
    HEARTBEAT,
    PONG,
    ERROR,
    BYE;

    public static MessageType from(String value) {
        if (value == null) {
            return null;
        }
        try {
            return MessageType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
