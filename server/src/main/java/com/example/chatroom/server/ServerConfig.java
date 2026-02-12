package com.example.chatroom.server;

public class ServerConfig {
    public enum Mode {
        NAIVE,
        BACKPRESSURE
    }

    private int port = 9000;
    private int metricsPort = 9001;
    private Mode mode = Mode.NAIVE;
    private int heartbeatIntervalSec = 5;
    private int heartbeatMissThreshold = 3;
    private int maxFrameBytes = 1024 * 1024;
    private int writeBufferLow = 32 * 1024;
    private int writeBufferHigh = 64 * 1024;
    private int backpressureUnwritableThreshold = 3;
    private int bizThreads = 4;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMetricsPort() {
        return metricsPort;
    }

    public void setMetricsPort(int metricsPort) {
        this.metricsPort = metricsPort;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public int getHeartbeatIntervalSec() {
        return heartbeatIntervalSec;
    }

    public void setHeartbeatIntervalSec(int heartbeatIntervalSec) {
        this.heartbeatIntervalSec = heartbeatIntervalSec;
    }

    public int getHeartbeatMissThreshold() {
        return heartbeatMissThreshold;
    }

    public void setHeartbeatMissThreshold(int heartbeatMissThreshold) {
        this.heartbeatMissThreshold = heartbeatMissThreshold;
    }

    public int getMaxFrameBytes() {
        return maxFrameBytes;
    }

    public void setMaxFrameBytes(int maxFrameBytes) {
        this.maxFrameBytes = maxFrameBytes;
    }

    public int getWriteBufferLow() {
        return writeBufferLow;
    }

    public void setWriteBufferLow(int writeBufferLow) {
        this.writeBufferLow = writeBufferLow;
    }

    public int getWriteBufferHigh() {
        return writeBufferHigh;
    }

    public void setWriteBufferHigh(int writeBufferHigh) {
        this.writeBufferHigh = writeBufferHigh;
    }

    public int getBackpressureUnwritableThreshold() {
        return backpressureUnwritableThreshold;
    }

    public void setBackpressureUnwritableThreshold(int backpressureUnwritableThreshold) {
        this.backpressureUnwritableThreshold = backpressureUnwritableThreshold;
    }

    public int getBizThreads() {
        return bizThreads;
    }

    public void setBizThreads(int bizThreads) {
        this.bizThreads = bizThreads;
    }
}
