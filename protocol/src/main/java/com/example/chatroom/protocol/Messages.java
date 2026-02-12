package com.example.chatroom.protocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class Messages {
    private Messages() {
    }

    public static Message login(String userId, String token, int protocolVersion) {
        Message msg = base(MessageType.LOGIN, userId);
        msg.getHeader().setProtocolVersion(protocolVersion);
        msg.getBody().put("userId", userId);
        msg.getBody().put("token", token);
        return msg;
    }

    public static Message loginAck(boolean success, String reason, String traceId) {
        Message msg = base(MessageType.LOGIN_ACK, null);
        if (traceId != null) {
            msg.getHeader().setTraceId(traceId);
        }
        msg.getBody().put("success", success);
        msg.getBody().put("reason", reason);
        return msg;
    }

    public static Message chat(String from, String text, long sendTs) {
        Message msg = base(MessageType.CHAT, from);
        msg.getBody().put("text", text);
        msg.getBody().put("sendTs", sendTs);
        return msg;
    }

    public static Message broadcast(String from, String text, long sendTs, String traceId) {
        Message msg = base(MessageType.BROADCAST, from);
        if (traceId != null) {
            msg.getHeader().setTraceId(traceId);
        }
        msg.getBody().put("text", text);
        msg.getBody().put("sendTs", sendTs);
        return msg;
    }

    public static Message heartbeat(String from) {
        return base(MessageType.HEARTBEAT, from);
    }

    public static Message pong(String from, String traceId) {
        Message msg = base(MessageType.PONG, from);
        if (traceId != null) {
            msg.getHeader().setTraceId(traceId);
        }
        return msg;
    }

    public static Message error(String from, String code, String reason, String traceId) {
        Message msg = base(MessageType.ERROR, from);
        if (traceId != null) {
            msg.getHeader().setTraceId(traceId);
        }
        msg.getBody().put("code", code);
        msg.getBody().put("reason", reason);
        return msg;
    }

    public static Message bye(String from) {
        return base(MessageType.BYE, from);
    }

    public static Message base(MessageType type, String from) {
        Message msg = new Message();
        Header header = new Header();
        header.setProtocolVersion(ProtocolConstants.DEFAULT_PROTOCOL_VERSION);
        header.setMsgType(type.name());
        header.setMsgId(UUID.randomUUID().toString());
        header.setTraceId(header.getMsgId());
        header.setTs(System.currentTimeMillis());
        header.setFrom(from);
        header.setRoom(ProtocolConstants.DEFAULT_ROOM);
        msg.setHeader(header);
        msg.setBody(new LinkedHashMap<>());
        return msg;
    }

    public static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public static long asLong(Object value, long defaultValue) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ex) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static boolean asBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String str) {
            return Boolean.parseBoolean(str);
        }
        return defaultValue;
    }
}
