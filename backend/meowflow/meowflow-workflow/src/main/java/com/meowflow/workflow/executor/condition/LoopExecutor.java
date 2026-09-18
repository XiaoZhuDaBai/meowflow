package com.meowflow.workflow.executor.condition;

import com.meowflow.common.context.CancellationToken;
import com.meowflow.common.exception.NodeException;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoopExecutor extends AbstractNodeExecutor {

    private final LoopSubgraphDriver loopSubgraphDriver;

    @Override
    public NodeType getNodeType() {
        return NodeType.LOOP;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        CancellationToken token = context.getCancellationToken();
        LoopSubgraphDriver.LoopExecutionResult result = loopSubgraphDriver.executeSubgraph(
                context, node, input, token);

        if (!result.isSuccess()) {
            throw new NodeException(node.getId(), node.getType().getCode(),
                    "Loop subgraph failed: " + result.getErrorMessage());
        }

        log.info("Loop completed: iterations={}, exitReason={}",
                result.getIterations(), result.getExitReason());
        return NodeResult.success(node.getId(), node.getType(), node.getName(),
                new HashMap<>(result.getLastOutput()));
    }
}
