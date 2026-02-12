package com.example.chatroom.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatServer {
    private static final Logger log = LoggerFactory.getLogger(ChatServer.class);

    private final ServerConfig config;
    private final MetricsRegistry metrics;
    private final SessionManager sessions;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private DefaultEventExecutorGroup bizGroup;
    private MetricsHttpServer metricsHttpServer;
    private ScheduledExecutorService heartbeatScheduler;

    public ChatServer(ServerConfig config) {
        this.config = config;
        this.metrics = new MetricsRegistry();
        this.sessions = new SessionManager(metrics);
    }

    public void start() throws InterruptedException, IOException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        bizGroup = new DefaultEventExecutorGroup(config.getBizThreads());

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                        new WriteBufferWaterMark(config.getWriteBufferLow(), config.getWriteBufferHigh()))
                .childHandler(new ServerInitializer(sessions, metrics, config, bizGroup));

        ChannelFuture bindFuture = bootstrap.bind(config.getPort()).sync();
        Channel serverChannel = bindFuture.channel();

        metricsHttpServer = new MetricsHttpServer(config.getMetricsPort(), metrics);
        metricsHttpServer.start();

        startHeartbeatChecker();

        log.info("server_started port={} metricsPort={} mode={}",
                config.getPort(), config.getMetricsPort(), config.getMode().name().toLowerCase());

        serverChannel.closeFuture().sync();
    }

    public void stop() {
        if (metricsHttpServer != null) {
            metricsHttpServer.stop();
        }
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
        }
        if (bizGroup != null) {
            bizGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }

    private void startHeartbeatChecker() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "heartbeat-checker");
            t.setDaemon(true);
            return t;
        });
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            long intervalMs = config.getHeartbeatIntervalSec() * 1000L;
            sessions.allChannels().forEach(channel -> {
                if (channel == null || !channel.isActive()) {
                    return;
                }
                Long last = channel.attr(ServerHandler.LAST_HEARTBEAT).get();
                if (last == null) {
                    channel.attr(ServerHandler.LAST_HEARTBEAT).set(now);
                    channel.attr(ServerHandler.MISS_COUNT).set(0);
                    return;
                }
                if (now - last >= intervalMs) {
                    int miss = getIntAttr(channel, ServerHandler.MISS_COUNT);
                    miss++;
                    channel.attr(ServerHandler.MISS_COUNT).set(miss);
                    if (miss >= config.getHeartbeatMissThreshold()) {
                        ServerHandler.markDisconnectReason(channel, "heartbeat_timeout");
                        channel.close();
                    }
                } else {
                    channel.attr(ServerHandler.MISS_COUNT).set(0);
                }
            });
        }, config.getHeartbeatIntervalSec(), config.getHeartbeatIntervalSec(), TimeUnit.SECONDS);
    }

    private int getIntAttr(io.netty.channel.Channel channel, io.netty.util.AttributeKey<Integer> key) {
        Integer value = channel.attr(key).get();
        return value == null ? 0 : value;
    }
}
