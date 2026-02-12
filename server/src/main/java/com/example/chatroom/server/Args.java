package com.example.chatroom.server;

import java.util.HashMap;
import java.util.Map;

public final class Args {
    private Args() {
    }

    public static Map<String, String> parse(String[] args) {
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

    public static int intValue(Map<String, String> map, String key, int defaultValue) {
        String value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public static String stringValue(Map<String, String> map, String key, String defaultValue) {
        return map.getOrDefault(key, defaultValue);
    }
}
