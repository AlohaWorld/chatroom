package com.example.chatroom.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private final Map<String, Channel> userToChannel = new ConcurrentHashMap<>();
    private final Map<ChannelId, String> channelToUser = new ConcurrentHashMap<>();
    private final MetricsRegistry metrics;

    public SessionManager(MetricsRegistry metrics) {
        this.metrics = metrics;
    }

    public LoginResult login(String userId, Channel channel) {
        if (userId == null || userId.isBlank()) {
            return new LoginResult(false, "userId_required");
        }
        Channel old = userToChannel.put(userId, channel);
        channelToUser.put(channel.id(), userId);
        if (old != null && old != channel) {
            ServerHandler.markDisconnectReason(old, "duplicate_login_kick");
            old.close();
            return new LoginResult(true, "duplicate_login_kick_old");
        }
        if (old == null) {
            metrics.incOnlineUsers();
        }
        return new LoginResult(true, "ok");
    }

    public String logout(Channel channel, String reason) {
        String userId = channelToUser.remove(channel.id());
        if (userId == null) {
            return null;
        }
        Channel current = userToChannel.get(userId);
        if (current == channel) {
            userToChannel.remove(userId, channel);
            metrics.decOnlineUsers();
            if (reason != null) {
                metrics.incDisconnect(reason);
            }
        }
        return userId;
    }

    public String getUser(Channel channel) {
        return channelToUser.get(channel.id());
    }

    public Collection<Channel> allChannels() {
        return userToChannel.values();
    }

    public int onlineUsers() {
        return userToChannel.size();
    }

    public record LoginResult(boolean success, String reason) {
    }
}
