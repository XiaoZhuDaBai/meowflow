package com.meowflow.infra.router;

import com.meowflow.infra.client.ChatClient;
import lombok.Data;

import java.util.List;

/**
 * 模型候选对象
 */
@Data
public class ModelCandidate {

    private final String name;
    private final String provider;
    private final ChatClient client;
    private final int priority;
    private final double weight;
    private volatile boolean healthy = true;
    private volatile long lastCheckTime;
    private volatile double latency;

    public ModelCandidate(String name, String provider, ChatClient client) {
        this(name, provider, client, 100, 1.0);
    }

    public ModelCandidate(String name, String provider, ChatClient client, int priority, double weight) {
        this.name = name;
        this.provider = provider;
        this.client = client;
        this.priority = priority;
        this.weight = weight;
        this.lastCheckTime = System.currentTimeMillis();
    }

    public void markHealthy(boolean healthy) {
        this.healthy = healthy;
        this.lastCheckTime = System.currentTimeMillis();
    }

    public void updateLatency(long latencyMs) {
        this.latency = latencyMs;
    }

    public double getScore() {
        if (!healthy) return 0;
        double latencyScore = Math.max(0, 100 - latency / 10);
        return priority * weight * (latencyScore / 100);
    }

    public boolean supports(List<String> capabilities) {
        return true;
    }
}
