package com.example.chatroom.tools;

import com.example.chatroom.protocol.JsonMessageCodec;
import com.example.chatroom.protocol.Messages;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadGenMain {
    public static void main(String[] args) throws Exception {
        Map<String, String> map = parseArgs(args);
        String host = map.getOrDefault("host", "127.0.0.1");
        int port = Integer.parseInt(map.getOrDefault("port", "9000"));
        int clients = Integer.parseInt(map.getOrDefault("clients", "50"));
        int durationSec = Integer.parseInt(map.getOrDefault("durationSec", "30"));
        double msgRate = Double.parseDouble(map.getOrDefault("msgRate", "100"));
        int payloadSize = Integer.parseInt(map.getOrDefault("payloadSize", "32"));

        System.out.println("LoadGen host=" + host + " port=" + port +
                " clients=" + clients + " msgRate=" + msgRate + " durationSec=" + durationSec);

        EventLoopGroup group = new NioEventLoopGroup();
        LoadGenStats stats = new LoadGenStats();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
                        ch.pipeline().addLast(new LengthFieldPrepender(4));
                        ch.pipeline().addLast(new JsonMessageCodec());
                        ch.pipeline().addLast(new LoadGenHandler(stats));
                    }
                });

        List<Channel> channels = new ArrayList<>();
        for (int i = 0; i < clients; i++) {
            Channel channel = bootstrap.connect(host, port).sync().channel();
            channels.add(channel);
            channel.writeAndFlush(Messages.login("loadgen-" + i, null, 1));
        }

        String payload = "x".repeat(Math.max(1, payloadSize));
        ScheduledExecutorService sender = Executors.newSingleThreadScheduledExecutor();
        AtomicInteger rr = new AtomicInteger();
        long intervalMs = 100;
        double perInterval = msgRate * intervalMs / 1000.0;
        double[] remainder = new double[]{0};

        sender.scheduleAtFixedRate(() -> {
            double total = perInterval + remainder[0];
            int toSend = (int) total;
            remainder[0] = total - toSend;
            for (int i = 0; i < toSend; i++) {
                if (channels.isEmpty()) {
                    return;
                }
                int idx = Math.abs(rr.getAndIncrement() % channels.size());
                Channel ch = channels.get(idx);
                long now = System.currentTimeMillis();
                ch.writeAndFlush(Messages.chat(null, payload, now));
                stats.sent.increment();
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);

        ScheduledExecutorService reporter = Executors.newSingleThreadScheduledExecutor();
        reporter.scheduleAtFixedRate(() -> {
            LoadGenStats.Snapshot snap = stats.snapshotAndReset();
            System.out.println("[stats] sent=" + snap.sent() +
                    " recv=" + snap.recv() +
                    " p50=" + snap.p50() +
                    " p95=" + snap.p95() +
                    " p99=" + snap.p99());
        }, 5, 5, TimeUnit.SECONDS);

        Thread.sleep(durationSec * 1000L);

        sender.shutdownNow();
        reporter.shutdownNow();
        for (Channel ch : channels) {
            ch.close();
        }
        group.shutdownGracefully();

        LoadGenStats.Snapshot finalSnap = stats.snapshotAndReset();
        System.out.println("[final] sent=" + finalSnap.sent() +
                " recv=" + finalSnap.recv() +
                " p50=" + finalSnap.p50() +
                " p95=" + finalSnap.p95() +
                " p99=" + finalSnap.p99());
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                continue;
            }
            String key;
            String value;
            int eq = arg.indexOf('=');
            if (eq > 2) {
                key = arg.substring(2, eq);
                value = arg.substring(eq + 1);
            } else {
                key = arg.substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    value = args[++i];
                } else {
                    value = "true";
                }
            }
            map.put(key, value);
        }
        return map;
    }
}
