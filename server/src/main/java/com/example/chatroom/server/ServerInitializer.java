package com.example.chatroom.server;

import com.example.chatroom.protocol.JsonMessageCodec;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {
    private final SessionManager sessions;
    private final MetricsRegistry metrics;
    private final ServerConfig config;
    private final DefaultEventExecutorGroup bizGroup;

    public ServerInitializer(SessionManager sessions, MetricsRegistry metrics, ServerConfig config, DefaultEventExecutorGroup bizGroup) {
        this.sessions = sessions;
        this.metrics = metrics;
        this.config = config;
        this.bizGroup = bizGroup;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(
                config.getMaxFrameBytes(), 0, 4, 0, 4));
        ch.pipeline().addLast(new LengthFieldPrepender(4));
        ch.pipeline().addLast(new JsonMessageCodec());
        // Handler runs on bizGroup to avoid blocking EventLoop.
        ch.pipeline().addLast(bizGroup, new ServerHandler(sessions, metrics, config));
    }
}
