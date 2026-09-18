package com.meowflow.workflow.executor.condition;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class BranchExecutor extends AbstractNodeExecutor {

    @Override
    public NodeType getNodeType() {
        return NodeType.BRANCH;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        List<Map<String, Object>> branches = (List<Map<String, Object>>) input.get("branches");
        if (branches == null || branches.isEmpty()) {
            branches = new ArrayList<>();
        }

        List<String> matchedBranches = new ArrayList<>();
        List<String> skippedBranches = new ArrayList<>();

        for (int i = 0; i < branches.size(); i++) {
            Map<String, Object> branch = branches.get(i);
            String branchName = (String) branch.getOrDefault("name", "branch_" + i);
            Object condition = branch.get("condition");

            boolean match = false;
            if (condition != null) {
                String condStr = context.resolveExpression(String.valueOf(condition)).toString();
                match = evaluateCondition(condStr);
            } else {
                match = true;
            }

            if (match) {
                matchedBranches.add(branchName);
                log.debug("Branch matched: {}", branchName);
            } else {
                skippedBranches.add(branchName);
                log.debug("Branch skipped: {}", branchName);
            }
        }

        Map<String, Object> output = new HashMap<>();
        output.put("branches", branches);
        output.put("matchedBranches", matchedBranches);
        output.put("skippedBranches", skippedBranches);
        output.put("matchedCount", matchedBranches.size());
        output.put("skippedCount", skippedBranches.size());

        // EdgeRouter 只认 selectedBranch 来做条件路由。BRANCH 原来只输出
        // matchedBranches 列表，导致 BRANCH 后面的条件边永远命中不了、下游被静默跳过。
        // 这里额外输出第一个命中的分支名（保留 matchedBranches 供完整信息使用）。
        if (!matchedBranches.isEmpty()) {
            output.put("selectedBranch", matchedBranches.get(0));
        }

        if (!matchedBranches.isEmpty()) {
            Object matchedData = branches.stream()
                    .filter(b -> matchedBranches.contains(b.getOrDefault("name", "")))
                    .findFirst()
                    .orElse(new HashMap<>())
                    .get("data");
            output.put("matchedData", matchedData);
        }

        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }

    private boolean evaluateCondition(String condition) {
        if (condition == null || condition.isEmpty()) return false;
        condition = condition.trim();
        if (condition.equalsIgnoreCase("true")) return true;
        try {
            return Double.parseDouble(condition) != 0;
        } catch (NumberFormatException e) {
            return Boolean.parseBoolean(condition);
        }
    }
}
