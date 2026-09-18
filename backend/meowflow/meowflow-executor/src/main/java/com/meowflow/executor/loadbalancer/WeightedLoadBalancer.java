package com.meowflow.executor.loadbalancer;

import com.meowflow.executor.registry.ExecutorNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 加权负载均衡器
 */
@Component
public class WeightedLoadBalancer implements LoadBalancer {

    @Override
    public ExecutorNode select(List<ExecutorNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        if (nodes.size() == 1) {
            return nodes.get(0);
        }

        int totalWeight = nodes.stream()
                .mapToInt(ExecutorNode::getWeight)
                .sum();

        if (totalWeight <= 0) {
            return nodes.get(0);
        }

        int randomWeight = (int) (Math.random() * totalWeight);
        int currentWeight = 0;

        for (ExecutorNode node : nodes) {
            currentWeight += node.getWeight();
            if (randomWeight < currentWeight) {
                return node;
            }
        }

        return nodes.get(nodes.size() - 1);
    }

    @Override
    public String getName() {
        return "weighted";
    }
}
