package com.example.chatroom.protocol;

import java.util.Map;

public class Message {
    private Header header;
    private Map<String, Object> body;

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Map<String, Object> getBody() {
        return body;
    }

    public void setBody(Map<String, Object> body) {
        this.body = body;
    }
}
