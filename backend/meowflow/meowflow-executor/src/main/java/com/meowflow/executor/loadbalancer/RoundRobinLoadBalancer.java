package com.meowflow.executor.loadbalancer;

import com.meowflow.executor.registry.ExecutorNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 轮询负载均衡器
 */
@Component
public class RoundRobinLoadBalancer implements LoadBalancer {

    private final AtomicLong counter = new AtomicLong(0);

    @Override
    public ExecutorNode select(List<ExecutorNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        if (nodes.size() == 1) {
            return nodes.get(0);
        }

        int index = (int) (counter.getAndIncrement() % nodes.size());
        return nodes.get(index);
    }

    @Override
    public String getName() {
        return "round-robin";
    }

    public void reset() {
        counter.set(0);
    }
}
