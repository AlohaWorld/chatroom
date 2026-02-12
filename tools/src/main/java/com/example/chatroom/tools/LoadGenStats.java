package com.example.chatroom.tools;

import java.util.concurrent.atomic.LongAdder;

public class LoadGenStats {
    public final LongAdder sent = new LongAdder();
    public final LongAdder received = new LongAdder();
    private final LatencyTracker latency = new LatencyTracker(10_000);

    public void recordLatency(long ms) {
        latency.record(ms);
        received.increment();
    }

    public Snapshot snapshotAndReset() {
        long sentCount = sent.sumThenReset();
        long recvCount = received.sumThenReset();
        LatencyTracker.Snapshot snap = latency.snapshot();
        latency.reset();
        return new Snapshot(sentCount, recvCount, snap.p50(), snap.p95(), snap.p99());
    }

    public record Snapshot(long sent, long recv, long p50, long p95, long p99) {
    }
}
