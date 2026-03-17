package com.example.chatroom.server;

import com.example.chatroom.protocol.Message;
import com.example.chatroom.protocol.MessageType;
import com.example.chatroom.protocol.Messages;
import com.example.chatroom.protocol.MessageDecodeException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;

public class ServerHandler extends SimpleChannelInboundHandler<Message> {
    private static final Logger log = LoggerFactory.getLogger(ServerHandler.class);

    public static final AttributeKey<String> USER_ID = AttributeKey.valueOf("userId");
    public static final AttributeKey<Long> LAST_HEARTBEAT = AttributeKey.valueOf("lastHeartbeat");
    public static final AttributeKey<Integer> MISS_COUNT = AttributeKey.valueOf("heartbeatMissCount");
    public static final AttributeKey<Integer> UNWRITABLE_STREAK = AttributeKey.valueOf("unwritableStreak");
    public static final AttributeKey<String> DISCONNECT_REASON = AttributeKey.valueOf("disconnectReason");

    private final SessionManager sessions;
    private final MetricsRegistry metrics;
    private final ServerConfig config;

    public ServerHandler(SessionManager sessions, MetricsRegistry metrics, ServerConfig config) {
        this.sessions = sessions;
        this.metrics = metrics;
        this.config = config;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        metrics.incCurrentConnections();
        touchHeartbeat(ctx.channel());
        log.info("channel_active channelId={}", ctx.channel().id().asShortText());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        metrics.decCurrentConnections();
        String reason = ctx.channel().attr(DISCONNECT_REASON).get();
        if (reason == null) {
            reason = "client_close";
        }
        String userId = sessions.logout(ctx.channel(), reason);
        if (userId != null) {
            broadcastSystem(userId + " left");
        }
        log.info("channel_inactive channelId={} reason={}", ctx.channel().id().asShortText(), reason);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        if (msg == null || msg.getHeader() == null) {
            metrics.incInvalid();
            safeSendError(ctx.channel(), "INVALID_HEADER", "header_missing", null);
            return;
        }
        String msgTypeValue = msg.getHeader().getMsgType();
        MessageType type = MessageType.from(msgTypeValue);
        if (type == null) {
            metrics.incInvalid();
            safeSendError(ctx.channel(), "UNKNOWN_TYPE", "msgType_unknown", msg.getHeader().getTraceId());
            return;
        }

        metrics.incInbound(type.name());
        bindMdc(ctx.channel(), msg, type);
        try {
            switch (type) {
                case LOGIN -> handleLogin(ctx, msg);
                case CHAT -> handleChat(ctx, msg);
                case HEARTBEAT -> handleHeartbeat(ctx, msg);
                case BYE -> handleBye(ctx, msg);
                case PONG, LOGIN_ACK, BROADCAST, ERROR ->
                    log.debug("client_sent_unexpected type={} channelId={}", type, ctx.channel().id().asShortText());
                default ->
                    safeSendError(ctx.channel(), "UNKNOWN_TYPE", "msgType_unknown", msg.getHeader().getTraceId());
            }
        } finally {
            MDC.clear();
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        if (!ctx.channel().isWritable()) {
            metrics.incUnwritableEvent();
            log.warn("channel_unwritable channelId={}", ctx.channel().id().asShortText());
        }
        ctx.fireChannelWritabilityChanged();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        metrics.incInvalid();
        String traceId = null;
        if (cause instanceof MessageDecodeException) {
            safeSendError(ctx.channel(), "DECODE_ERROR", "decode_failed", traceId);
        } else if (cause instanceof TooLongFrameException) {
            safeSendError(ctx.channel(), "FRAME_TOO_LARGE", "frame_too_large", traceId);
        } else if (cause instanceof CorruptedFrameException) {
            safeSendError(ctx.channel(), "FRAME_CORRUPT", "frame_corrupt", traceId);
        } else {
            safeSendError(ctx.channel(), "SERVER_EXCEPTION", "unexpected_error", traceId);
        }
        markDisconnectReason(ctx.channel(), "exception");
        log.warn("exception channelId={} msg={}", ctx.channel().id().asShortText(), cause.toString());
        ctx.close();
    }

    private void handleLogin(ChannelHandlerContext ctx, Message msg) {
        Map<String, Object> body = msg.getBody();
        String userId = Messages.asString(body == null ? null : body.get("userId"));
        SessionManager.LoginResult result = sessions.login(userId, ctx.channel());
        if (!result.success()) {
            metrics.incInvalid();
            safeSendError(ctx.channel(), "LOGIN_FAILED", result.reason(), msg.getHeader().getTraceId());
            return;
        }
        ctx.channel().attr(USER_ID).set(userId);
        touchHeartbeat(ctx.channel());
        send(ctx.channel(), Messages.loginAck(true, result.reason(), msg.getHeader().getTraceId()));
        broadcastSystem(userId + " joined");
        log.info("login_success userId={} channelId={}", userId, ctx.channel().id().asShortText());
    }

    private void handleChat(ChannelHandlerContext ctx, Message msg) {
        String userId = sessions.getUser(ctx.channel());
        if (userId == null) {
            metrics.incInvalid();
            safeSendError(ctx.channel(), "NOT_LOGGED_IN", "login_required", msg.getHeader().getTraceId());
            return;
        }
        Map<String, Object> body = msg.getBody();
        String text = Messages.asString(body == null ? null : body.get("text"));
        if (text == null || text.isBlank()) {
            metrics.incInvalid();
            safeSendError(ctx.channel(), "EMPTY_TEXT", "text_required", msg.getHeader().getTraceId());
            return;
        }
        long sendTs = Messages.asLong(body == null ? null : body.get("sendTs"), System.currentTimeMillis());
        Message broadcast = Messages.broadcast(userId, text, sendTs, msg.getHeader().getTraceId());
        broadcastToAll(broadcast);
    }

    private void handleHeartbeat(ChannelHandlerContext ctx, Message msg) {
        touchHeartbeat(ctx.channel());
        send(ctx.channel(), Messages.pong(sessions.getUser(ctx.channel()), msg.getHeader().getTraceId()));
    }

    private void handleBye(ChannelHandlerContext ctx, Message msg) {
        markDisconnectReason(ctx.channel(), "client_close");
        ctx.close();
    }

    private void broadcastToAll(Message broadcast) {
        long startNs = System.nanoTime();
        for (Channel channel : sessions.allChannels()) {
            if (channel == null || !channel.isActive()) {
                continue;
            }
            // ChatServer启动时有naive和backpressure两种mode，naive中无背压处理
            // backpressure中有限流处理
            if (config.getMode() == ServerConfig.Mode.BACKPRESSURE) {
                if (!channel.isWritable()) {
                    int streak = getIntAttr(channel, UNWRITABLE_STREAK);
                    streak++;
                    channel.attr(UNWRITABLE_STREAK).set(streak);
                    metrics.incBackpressureDrop();
                    if (streak >= config.getBackpressureUnwritableThreshold()) {
                        metrics.incBackpressureDisconnect();
                        markDisconnectReason(channel, "backpressure");
                        channel.close();
                    }
                    continue;
                } else {
                    channel.attr(UNWRITABLE_STREAK).set(0);
                }
            }
            send(channel, broadcast);
        }
        long tookMs = (System.nanoTime() - startNs) / 1_000_000;
        metrics.recordBroadcastLatency(tookMs);
        log.info("broadcast_done tookMs={} mode={}", tookMs, config.getMode().name().toLowerCase());
    }

    private void broadcastSystem(String text) {
        Message broadcast = Messages.broadcast("SYSTEM", text, System.currentTimeMillis(), null);
        broadcastToAll(broadcast);
    }

    private void send(Channel channel, Message msg) {
        if (channel == null || !channel.isActive()) {
            return;
        }
        metrics.incOutbound(msg.getHeader().getMsgType());
        channel.writeAndFlush(msg);
    }

    private void safeSendError(Channel channel, String code, String reason, String traceId) {
        Message err = Messages.error(sessions.getUser(channel), code, reason, traceId);
        send(channel, err);
    }

    private void bindMdc(Channel channel, Message msg, MessageType type) {
        MDC.put("traceId", msg.getHeader().getTraceId());
        MDC.put("userId", sessions.getUser(channel));
        MDC.put("channelId", channel.id().asShortText());
        MDC.put("msgType", type.name());
    }

    public static void markDisconnectReason(Channel channel, String reason) {
        if (channel != null) {
            channel.attr(DISCONNECT_REASON).set(reason);
        }
    }

    public static void touchHeartbeat(Channel channel) {
        if (channel != null) {
            channel.attr(LAST_HEARTBEAT).set(System.currentTimeMillis());
            channel.attr(MISS_COUNT).set(0);
        }
    }

    private int getIntAttr(Channel channel, AttributeKey<Integer> key) {
        Integer value = channel.attr(key).get();
        return value == null ? 0 : value;
    }
}
