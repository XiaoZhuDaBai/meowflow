package com.meowflow.executor.loadbalancer;

import com.meowflow.executor.registry.ExecutorNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * 随机负载均衡器
 */
@Component
public class RandomLoadBalancer implements LoadBalancer {

    private final Random random = new Random();

    @Override
    public ExecutorNode select(List<ExecutorNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        if (nodes.size() == 1) {
            return nodes.get(0);
        }
        return nodes.get(random.nextInt(nodes.size()));
    }

    @Override
    public String getName() {
        return "random";
    }
}
