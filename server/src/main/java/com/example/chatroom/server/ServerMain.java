package com.example.chatroom.server;

import java.io.IOException;
import java.util.Map;

public class ServerMain {
    public static void main(String[] args) throws IOException, InterruptedException {
        Map<String, String> map = Args.parse(args);
        ServerConfig config = new ServerConfig();

        config.setPort(Args.intValue(map, "port", config.getPort()));
        int defaultMetricsPort = config.getPort() + 1;
        config.setMetricsPort(Args.intValue(map, "metricsPort", defaultMetricsPort));
        config.setHeartbeatIntervalSec(Args.intValue(map, "heartbeatIntervalSec", config.getHeartbeatIntervalSec()));
        config.setHeartbeatMissThreshold(Args.intValue(map, "heartbeatMissThreshold", config.getHeartbeatMissThreshold()));
        config.setMaxFrameBytes(Args.intValue(map, "maxFrameBytes", config.getMaxFrameBytes()));
        config.setWriteBufferLow(Args.intValue(map, "writeBufferLow", config.getWriteBufferLow()));
        config.setWriteBufferHigh(Args.intValue(map, "writeBufferHigh", config.getWriteBufferHigh()));
        config.setBackpressureUnwritableThreshold(Args.intValue(map, "backpressureUnwritableThreshold",
                config.getBackpressureUnwritableThreshold()));
        config.setBizThreads(Args.intValue(map, "bizThreads", config.getBizThreads()));

        String mode = Args.stringValue(map, "mode", config.getMode().name());
        if (mode != null) {
            if (mode.equalsIgnoreCase("backpressure")) {
                config.setMode(ServerConfig.Mode.BACKPRESSURE);
            } else {
                config.setMode(ServerConfig.Mode.NAIVE);
            }
        }

        ChatServer server = new ChatServer(config);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }
}
