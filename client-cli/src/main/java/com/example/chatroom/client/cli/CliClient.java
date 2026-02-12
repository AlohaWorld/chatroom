package com.example.chatroom.client.cli;

import com.example.chatroom.protocol.JsonMessageCodec;
import com.example.chatroom.protocol.Messages;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CliClient {
    private final EventLoopGroup group = new NioEventLoopGroup();
    private Channel channel;
    private ScheduledExecutorService heartbeatScheduler;

    public void connect(String host, int port) throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
                        ch.pipeline().addLast(new LengthFieldPrepender(4));
                        ch.pipeline().addLast(new JsonMessageCodec());
                        ch.pipeline().addLast(new ClientHandler());
                    }
                });
        ChannelFuture future = bootstrap.connect(host, port).sync();
        channel = future.channel();
        System.out.println("Connected to " + host + ":" + port);
    }

    public void login(String userId) {
        send(Messages.login(userId, null, 1));
    }

    public void sendChat(String text) {
        long now = System.currentTimeMillis();
        send(Messages.chat(null, text, now));
    }

    public void sendBye() {
        send(Messages.bye(null));
    }

    public void startHeartbeat(int intervalSec) {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cli-heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatScheduler.scheduleAtFixedRate(() -> send(Messages.heartbeat(null)),
                intervalSec, intervalSec, TimeUnit.SECONDS);
    }

    public void close() {
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
        }
        if (channel != null) {
            channel.close();
        }
        group.shutdownGracefully();
    }

    private void send(Object msg) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(msg);
        }
    }
}
