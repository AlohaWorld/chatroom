package com.example.chatroom.client.cli;

import com.example.chatroom.protocol.Message;
import com.example.chatroom.protocol.MessageType;
import com.example.chatroom.protocol.Messages;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.time.Instant;
import java.util.Map;

public class ClientHandler extends SimpleChannelInboundHandler<Message> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        if (msg == null || msg.getHeader() == null) {
            return;
        }
        MessageType type = MessageType.from(msg.getHeader().getMsgType());
        if (type == null) {
            return;
        }
        Map<String, Object> body = msg.getBody();
        switch (type) {
            case LOGIN_ACK -> {
                boolean success = Messages.asBoolean(body.get("success"), false);
                String reason = Messages.asString(body.get("reason"));
                System.out.println("LOGIN_ACK success=" + success + " reason=" + reason);
            }
            case BROADCAST -> {
                String from = msg.getHeader().getFrom();
                String text = Messages.asString(body.get("text"));
                long sendTs = Messages.asLong(body.get("sendTs"), 0);
                String ts = sendTs > 0 ? Instant.ofEpochMilli(sendTs).toString() : Instant.now().toString();
                System.out.println("[" + ts + "] " + from + ": " + text);
            }
            case ERROR -> {
                String code = Messages.asString(body.get("code"));
                String reason = Messages.asString(body.get("reason"));
                System.out.println("ERROR code=" + code + " reason=" + reason);
            }
            case PONG -> System.out.println("PONG");
            default -> System.out.println("RECV " + type.name());
        }
    }
}
