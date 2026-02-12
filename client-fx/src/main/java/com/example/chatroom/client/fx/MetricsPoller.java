package com.example.chatroom.client.fx;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MetricsPoller {
    private final String url;
    private final Consumer<Integer> onlineConsumer;
    private final ScheduledExecutorService scheduler;
    private final HttpClient httpClient;

    public MetricsPoller(String host, int port, Consumer<Integer> onlineConsumer) {
        this.url = "http://" + host + ":" + port + "/metrics";
        this.onlineConsumer = onlineConsumer;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "fx-metrics");
            t.setDaemon(true);
            return t;
        });
        this.httpClient = HttpClient.newBuilder().build();
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::poll, 1, 2, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    private void poll() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int online = parseOnline(response.body());
            if (online >= 0) {
                onlineConsumer.accept(online);
            }
        } catch (IOException | InterruptedException ex) {
            // Ignore temporary errors to keep UI responsive.
        }
    }

    private int parseOnline(String body) {
        if (body == null) {
            return -1;
        }
        String[] lines = body.split("\\n");
        for (String line : lines) {
            if (line.startsWith("online_users")) {
                String[] parts = line.split("\\s+");
                if (parts.length == 2) {
                    try {
                        return Integer.parseInt(parts[1]);
                    } catch (NumberFormatException ex) {
                        return -1;
                    }
                }
            }
        }
        return -1;
    }
}
