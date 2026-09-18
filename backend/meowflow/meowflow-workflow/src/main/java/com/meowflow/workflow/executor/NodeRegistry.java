package com.meowflow.workflow.executor;

import com.meowflow.workflow.definition.NodeType;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class NodeRegistry {

    private final List<NodeExecutor> executors;
    private final Map<NodeType, NodeExecutor> executorMap = new EnumMap<>(NodeType.class);

    public NodeRegistry() {
        this.executors = List.of();
    }

    @Autowired
    public NodeRegistry(List<NodeExecutor> executors) {
        this.executors = executors;
    }

    @PostConstruct
    public void init() {
        for (NodeExecutor executor : executors) {
            register(executor);
        }
    }

    /**
     * 手动注册执行器。
     */
    public void register(NodeType type, NodeExecutor executor) {
        executorMap.put(type, executor);
        log.debug("Registered node executor: type={}, executor={}",
                type, executor.getClass().getSimpleName());
    }

    /**
     * 自动注册（从 init 调用）。
     */
    private void register(NodeExecutor executor) {
        NodeType type = executor.getNodeType();
        if (type != null) {
            executorMap.put(type, executor);
            log.info("Registered node executor: type={}, executor={}",
                    type, executor.getClass().getSimpleName());
        }
    }

    public NodeExecutor getExecutor(NodeType type) {
        return executorMap.get(type);
    }

    public NodeExecutor getExecutor(String typeCode) {
        try {
            NodeType type = NodeType.fromCode(typeCode);
            return getExecutor(type);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown node type code: {}", typeCode);
            return null;
        }
    }

    public boolean isRegistered(NodeType type) {
        return executorMap.containsKey(type);
    }

    public List<NodeType> getRegisteredTypes() {
        return List.copyOf(executorMap.keySet());
    }
}

