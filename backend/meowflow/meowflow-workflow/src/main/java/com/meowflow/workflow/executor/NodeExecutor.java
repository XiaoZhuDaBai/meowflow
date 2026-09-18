package com.meowflow.workflow.executor;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public interface NodeExecutor {

    /**
     * 同步执行节点。
     *
     * @param context 执行上下文
     * @param node    节点定义
     * @return 节点执行结果
     */
    NodeResult execute(ExecutionContext context, NodeDefinition node);

    /**
     * 异步执行节点（可选实现）。
     *
     * 默认实现直接调用 {@link #execute(ExecutionContext, NodeDefinition)} 并包装为已完成的未来。
     * 子类可覆盖此方法以支持真正的异步（如 LLM 调用、外部 HTTP）。
     *
     * @param context 执行上下文
     * @param node    节点定义
     * @param timeout 单节点超时时长；调用方应遵守此超时
     * @return 异步执行结果
     */
    default CompletableFuture<NodeResult> executeAsync(ExecutionContext context, NodeDefinition node, Duration timeout) {
        try {
            return CompletableFuture.completedFuture(execute(context, node));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    default NodeType getNodeType() {
        return null;
    }

    default boolean supports(NodeType type) {
        return getNodeType() == type;
    }
}
