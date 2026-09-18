package com.meowflow.executor.loadbalancer;

import com.meowflow.executor.registry.ExecutorNode;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 最小连接数负载均衡器
 */
@Component
public class LeastConnectionsLoadBalancer implements LoadBalancer {

    @Override
    public ExecutorNode select(List<ExecutorNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        if (nodes.size() == 1) {
            return nodes.get(0);
        }

        return nodes.stream()
                .filter(ExecutorNode::canAcceptTask)
                .min(Comparator.comparingInt(node -> node.getCurrentTasks().get()))
                .orElse(null);
    }

    @Override
    public String getName() {
        return "least-connections";
    }
}
