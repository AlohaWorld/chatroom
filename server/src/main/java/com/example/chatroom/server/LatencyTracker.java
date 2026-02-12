package com.example.chatroom.server;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

public class LatencyTracker {
    private final AtomicLongArray ring;
    private final AtomicLong index = new AtomicLong();

    public LatencyTracker(int size) {
        this.ring = new AtomicLongArray(size);
    }

    public void record(long valueMs) {
        if (valueMs < 0) {
            return;
        }
        long i = index.getAndIncrement();
        int slot = (int) (i % ring.length());
        ring.set(slot, valueMs);
    }

    public Snapshot snapshot() {
        long written = Math.min(index.get(), ring.length());
        if (written == 0) {
            return new Snapshot(0, 0, 0);
        }
        long[] values = new long[(int) written];
        for (int i = 0; i < written; i++) {
            values[i] = ring.get(i);
        }
        Arrays.sort(values);
        long p50 = percentile(values, 0.50);
        long p95 = percentile(values, 0.95);
        long p99 = percentile(values, 0.99);
        return new Snapshot(p50, p95, p99);
    }

    private long percentile(long[] values, double ratio) {
        if (values.length == 0) {
            return 0;
        }
        int index = (int) Math.ceil(ratio * values.length) - 1;
        index = Math.max(0, Math.min(index, values.length - 1));
        return values[index];
    }

    public record Snapshot(long p50, long p95, long p99) {
    }
}
