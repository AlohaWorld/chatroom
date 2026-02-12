package com.example.chatroom.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public class MetricsRegistry {
    private final AtomicInteger currentConnections = new AtomicInteger();
    private final AtomicInteger onlineUsers = new AtomicInteger();
    private final Map<String, LongAdder> inboundByType = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> outboundByType = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> disconnectByReason = new ConcurrentHashMap<>();
    private final LongAdder invalidMsgTotal = new LongAdder();
    private final LongAdder backpressureDropTotal = new LongAdder();
    private final LongAdder backpressureDisconnectTotal = new LongAdder();
    private final LongAdder unwritableEventsTotal = new LongAdder();
    private final LatencyTracker broadcastLatency = new LatencyTracker(10_000);

    public void incCurrentConnections() {
        currentConnections.incrementAndGet();
    }

    public void decCurrentConnections() {
        currentConnections.decrementAndGet();
    }

    public void incOnlineUsers() {
        onlineUsers.incrementAndGet();
    }

    public void decOnlineUsers() {
        onlineUsers.decrementAndGet();
    }

    public void incInbound(String msgType) {
        inboundByType.computeIfAbsent(msgType, k -> new LongAdder()).increment();
    }

    public void incOutbound(String msgType) {
        outboundByType.computeIfAbsent(msgType, k -> new LongAdder()).increment();
    }

    public void incInvalid() {
        invalidMsgTotal.increment();
    }

    public void incDisconnect(String reason) {
        disconnectByReason.computeIfAbsent(reason, k -> new LongAdder()).increment();
    }

    public void incBackpressureDrop() {
        backpressureDropTotal.increment();
    }

    public void incBackpressureDisconnect() {
        backpressureDisconnectTotal.increment();
    }

    public void incUnwritableEvent() {
        unwritableEventsTotal.increment();
    }

    public void recordBroadcastLatency(long ms) {
        broadcastLatency.record(ms);
    }

    public String formatPrometheus() {
        StringBuilder sb = new StringBuilder();
        sb.append("current_connections ").append(currentConnections.get()).append('\n');
        sb.append("online_users ").append(onlineUsers.get()).append('\n');

        inboundByType.forEach((type, counter) -> sb.append("inbound_msg_total{type=\"")
                .append(type).append("\"} ").append(counter.sum()).append('\n'));
        outboundByType.forEach((type, counter) -> sb.append("outbound_msg_total{type=\"")
                .append(type).append("\"} ").append(counter.sum()).append('\n'));

        LatencyTracker.Snapshot snapshot = broadcastLatency.snapshot();
        sb.append("broadcast_latency_ms_p50 ").append(snapshot.p50()).append('\n');
        sb.append("broadcast_latency_ms_p95 ").append(snapshot.p95()).append('\n');
        sb.append("broadcast_latency_ms_p99 ").append(snapshot.p99()).append('\n');

        sb.append("invalid_msg_total ").append(invalidMsgTotal.sum()).append('\n');
        disconnectByReason.forEach((reason, counter) -> sb.append("disconnect_total{reason=\"")
                .append(reason).append("\"} ").append(counter.sum()).append('\n'));
        sb.append("backpressure_drop_total ").append(backpressureDropTotal.sum()).append('\n');
        sb.append("backpressure_disconnect_total ").append(backpressureDisconnectTotal.sum()).append('\n');
        sb.append("unwritable_events_total ").append(unwritableEventsTotal.sum()).append('\n');
        return sb.toString();
    }
}
