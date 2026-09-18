package com.meowflow.infra.router;

import com.meowflow.infra.client.ChatClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 模型健康检查器
 */
@Slf4j
@Component
public class HealthChecker {

    private final List<ModelCandidate> candidates = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public HealthChecker() {
        scheduler.scheduleAtFixedRate(this::checkAll, 30, 30, TimeUnit.SECONDS);
    }

    public void registerCandidate(ModelCandidate candidate) {
        candidates.add(candidate);
        checkCandidate(candidate);
    }

    public void unregisterCandidate(String name) {
        candidates.removeIf(c -> c.getName().equals(name));
    }

    public void checkAll() {
        for (ModelCandidate candidate : candidates) {
            checkCandidate(candidate);
        }
    }

    public void checkCandidate(ModelCandidate candidate) {
        try {
            long start = System.currentTimeMillis();
            boolean available = candidate.getClient().isAvailable();
            long latency = System.currentTimeMillis() - start;

            candidate.markHealthy(available);
            if (available) {
                candidate.updateLatency(latency);
            }
            log.debug("Health check for {}: healthy={}, latency={}ms", candidate.getName(), available, latency);
        } catch (Exception e) {
            candidate.markHealthy(false);
            log.warn("Health check failed for {}: {}", candidate.getName(), e.getMessage());
        }
    }

    public List<ModelCandidate> getHealthyCandidates() {
        return candidates.stream()
                .filter(ModelCandidate::isHealthy)
                .toList();
    }

    public ModelCandidate getBestCandidate() {
        return candidates.stream()
                .filter(ModelCandidate::isHealthy)
                .max((a, b) -> Double.compare(a.getScore(), b.getScore()))
                .orElse(null);
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
