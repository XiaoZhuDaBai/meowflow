package com.meowflow.workflow.engine;

import lombok.Getter;

import java.util.List;

/**
 * DAG cycle exception - thrown when a circular dependency is detected in the workflow graph.
 */
@Getter
public class DAGHasCycleException extends RuntimeException {

    private final List<String> cycleNodes;

    public DAGHasCycleException(String message, List<String> cycleNodes) {
        super(message);
        this.cycleNodes = List.copyOf(cycleNodes);
    }

    public DAGHasCycleException(List<String> cycleNodes) {
        super("工作流存在循环依赖: " + cycleNodes);
        this.cycleNodes = List.copyOf(cycleNodes);
    }

    @Override
    public String toString() {
        return "DAGHasCycleException{" +
                "cycleNodes=" + cycleNodes +
                ", message=" + getMessage() +
                '}';
    }
}
