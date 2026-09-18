package com.meowflow.executor.loadbalancer;

import com.meowflow.executor.registry.ExecutorNode;

import java.util.List;

/**
 * 负载均衡器接口
 */
public interface LoadBalancer {

    /**
     * 选择一个执行节点
     *
     * @param nodes 可用节点列表
     * @return 选中的节点
     */
    ExecutorNode select(List<ExecutorNode> nodes);

    /**
     * 获取负载均衡器名称
     */
    String getName();
}
