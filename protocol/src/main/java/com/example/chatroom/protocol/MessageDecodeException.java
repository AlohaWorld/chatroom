package com.example.chatroom.protocol;

public class MessageDecodeException extends RuntimeException {
    public MessageDecodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
