package com.example.chatroom.client.cli;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class CliMain {
    public static void main(String[] args) throws Exception {
        Map<String, String> map = parseArgs(args);
        String host = map.getOrDefault("host", "127.0.0.1");
        int port = Integer.parseInt(map.getOrDefault("port", "9000"));
        int heartbeat = Integer.parseInt(map.getOrDefault("heartbeatIntervalSec", "5"));

        CliClient client = new CliClient();
        client.connect(host, port);
        client.startHeartbeat(heartbeat);

        String userId = map.get("userId");
        if (userId != null) {
            client.login(userId);
        }

        System.out.println("Commands: /login <userId> | /send <text> | /bye | /help");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            if (line.isBlank()) {
                continue;
            }
            if (line.startsWith("/login")) {
                String[] parts = line.split("\\s+", 2);
                if (parts.length == 2) {
                    client.login(parts[1].trim());
                } else {
                    System.out.println("usage: /login <userId>");
                }
                continue;
            }
            if (line.startsWith("/send")) {
                String[] parts = line.split("\\s+", 2);
                if (parts.length == 2) {
                    client.sendChat(parts[1].trim());
                } else {
                    System.out.println("usage: /send <text>");
                }
                continue;
            }
            if (line.startsWith("/bye")) {
                client.sendBye();
                client.close();
                break;
            }
            if (line.startsWith("/help")) {
                System.out.println("Commands: /login <userId> | /send <text> | /bye | /help");
                continue;
            }
            client.sendChat(line.trim());
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
