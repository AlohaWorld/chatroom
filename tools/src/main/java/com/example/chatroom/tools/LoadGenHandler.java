package com.example.chatroom.tools;

import com.example.chatroom.protocol.Message;
import com.example.chatroom.protocol.MessageType;
import com.example.chatroom.protocol.Messages;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Map;

public class LoadGenHandler extends SimpleChannelInboundHandler<Message> {
    private final LoadGenStats stats;

    public LoadGenHandler(LoadGenStats stats) {
        this.stats = stats;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        if (msg == null || msg.getHeader() == null) {
            return;
        }
        MessageType type = MessageType.from(msg.getHeader().getMsgType());
        if (type == MessageType.BROADCAST) {
            Map<String, Object> body = msg.getBody();
            long sendTs = Messages.asLong(body == null ? null : body.get("sendTs"), 0);
            if (sendTs > 0) {
                long latency = System.currentTimeMillis() - sendTs;
                stats.recordLatency(latency);
            }
        }
    }
}
