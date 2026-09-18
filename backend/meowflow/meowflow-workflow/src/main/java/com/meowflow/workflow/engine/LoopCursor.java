package com.meowflow.workflow.engine;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

/**
 * LOOP 节点的迭代游标。
 *
 * <p>记录每个 LOOP 节点当前迭代状态，用于子图多次迭代和快照序列化。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoopCursor implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前迭代序号（从 0 开始） */
    @Builder.Default
    private int iteration = 0;

    /**
     * 快照数据（用于恢复子作用域变量状态）。
     * key = variableName, value = variableValue
     */
    private Map<String, Object> snapshot;

    public void incrementIteration() {
        this.iteration++;
    }
}
