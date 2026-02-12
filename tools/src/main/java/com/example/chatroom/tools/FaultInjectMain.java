package com.example.chatroom.tools;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FaultInjectMain {
    public static void main(String[] args) throws Exception {
        Map<String, String> map = parseArgs(args);
        String host = map.getOrDefault("host", "127.0.0.1");
        int port = Integer.parseInt(map.getOrDefault("port", "9000"));
        String faultCase = map.getOrDefault("case", "all");

        if (faultCase.equalsIgnoreCase("all")) {
            runAll(host, port);
        } else {
            runCase(host, port, faultCase);
        }
    }

    private static void runAll(String host, int port) throws Exception {
        runCase(host, port, "missing_fields");
        runCase(host, port, "unknown_type");
        runCase(host, port, "bad_length");
        runCase(host, port, "too_large");
        runCase(host, port, "disconnect");
    }

    private static void runCase(String host, int port, String faultCase) throws Exception {
        System.out.println("Inject case=" + faultCase);
        switch (faultCase) {
            case "missing_fields" -> sendMissingFields(host, port);
            case "unknown_type" -> sendUnknownType(host, port);
            case "bad_length" -> sendBadLength(host, port);
            case "too_large" -> sendTooLarge(host, port);
            case "disconnect" -> sendDisconnect(host, port);
            default -> System.out.println("Unknown case: " + faultCase);
        }
    }

    private static void sendMissingFields(String host, int port) throws Exception {
        String json = "{" +
                "\"header\":{" +
                "\"protocolVersion\":1," +
                "\"msgType\":\"LOGIN\"," +
                "\"msgId\":\"" + UUID.randomUUID() + "\"," +
                "\"traceId\":\"" + UUID.randomUUID() + "\"," +
                "\"ts\":123456" +
                "}" +
                "}";
        sendFrame(host, port, json.getBytes(StandardCharsets.UTF_8), json.length());
    }

    private static void sendUnknownType(String host, int port) throws Exception {
        String json = "{" +
                "\"header\":{" +
                "\"protocolVersion\":1," +
                "\"msgType\":\"WHAT\"," +
                "\"msgId\":\"" + UUID.randomUUID() + "\"," +
                "\"traceId\":\"" + UUID.randomUUID() + "\"," +
                "\"ts\":123456" +
                "}," +
                "\"body\":{\"foo\":\"bar\"}" +
                "}";
        sendFrame(host, port, json.getBytes(StandardCharsets.UTF_8), json.length());
    }

    private static void sendBadLength(String host, int port) throws Exception {
        String json = "{\"header\":{\"protocolVersion\":1,\"msgType\":\"CHAT\",\"msgId\":\"" +
                UUID.randomUUID() + "\",\"traceId\":\"" + UUID.randomUUID() +
                "\",\"ts\":123456},\"body\":{\"text\":\"hi\"}}";
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        // Declare length smaller than payload to corrupt framing.
        sendFrame(host, port, payload, 2);
    }

    private static void sendTooLarge(String host, int port) throws Exception {
        byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);
        // Declare a huge frame length to trigger TooLongFrameException.
        sendFrame(host, port, payload, 50 * 1024 * 1024);
    }

    private static void sendDisconnect(String host, int port) throws Exception {
        try (Socket socket = new Socket(host, port)) {
            socket.close();
        }
    }

    private static void sendFrame(String host, int port, byte[] payload, int declaredLength) throws IOException {
        try (Socket socket = new Socket(host, port)) {
            OutputStream out = socket.getOutputStream();
            byte[] lengthBytes = ByteBuffer.allocate(4).putInt(declaredLength).array();
            out.write(lengthBytes);
            out.write(payload);
            out.flush();
        }
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
