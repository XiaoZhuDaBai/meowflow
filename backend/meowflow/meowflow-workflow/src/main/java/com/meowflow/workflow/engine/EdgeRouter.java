package com.meowflow.workflow.engine;

import com.meowflow.workflow.definition.Edge;
import com.meowflow.workflow.definition.EdgeType;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.WorkflowDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 边路由决策器。
 *
 * 根据上游节点的执行结果，从该节点的所有出边中筛选出实际需要推进的下游边。
 * 规则优先级：
 * <ol>
 *   <li>ERROR 边：仅当上游节点失败（{@link NodeResult.NodeStatus#FAILED}）时选中。</li>
 *   <li>CONDITION_TRUE 边：仅当上游结果中 {@code selectedBranch == "true"} 时选中。</li>
 *   <li>CONDITION_FALSE 边：仅当上游结果中 {@code selectedBranch == "false"} 时选中。</li>
 *   <li>CONDITION 边：逐个求值边上的表达式，未配置表达式时按标签/分支名路由。</li>
 *   <li>其余所有类型的边均按 DEFAULT / 无类型处理。</li>
 * </ol>
 *
 * 当无任何显式条件边被命中时，回退到所有非 ERROR 边（默认行为）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EdgeRouter {

    /**
     * 从上游节点的出边列表中选出需要推进的下游目标节点 ID。
     *
     * @param upstream       上游节点定义
     * @param outgoing      上游的所有出边
     * @param upstreamResult 上游节点执行结果（可为 null，表示尚未执行）
     * @param definition    当前工作流定义（用于校验 target 节点是否存在）
     * @return 需要推进的目标节点 ID 列表；不会返回 null
     */
    public List<String> selectDownstreamEdges(
            NodeDefinition upstream,
            List<Edge> outgoing,
            NodeResult upstreamResult,
            WorkflowDefinition definition,
            ExecutionContext context) {

        if (outgoing == null || outgoing.isEmpty()) {
            return List.of();
        }

        // 先过滤掉指向不存在节点或 NOTE 节点的边（防御性）
        List<Edge> validEdges = outgoing.stream()
                .filter(e -> e.getTarget() != null)
                .filter(e -> definition.findNode(e.getTarget()) != null)
                .collect(Collectors.toList());

        if (validEdges.isEmpty()) {
            return List.of();
        }

        // 如果上游未执行或无结果，按 DEFAULT 处理
        if (upstreamResult == null) {
            return defaultEdges(validEdges);
        }

        NodeResult.NodeStatus status = upstreamResult.getStatus();
        Map<String, Object> output = upstreamResult.getOutput();

        // 1. ERROR 边
        if (status == NodeResult.NodeStatus.FAILED || status == NodeResult.NodeStatus.CANCELLED) {
            List<String> errorTargets = validEdges.stream()
                    .filter(e -> e.getType() == EdgeType.ERROR)
                    .map(Edge::getTarget)
                    .collect(Collectors.toList());
            if (!errorTargets.isEmpty()) {
                log.debug("EdgeRouter: upstream node {} failed/cancelled, routing to {} error edges",
                        upstream.getId(), errorTargets.size());
                return errorTargets;
            }
            // 没有 ERROR 边时，失败节点不再推进下游
            log.debug("EdgeRouter: upstream node {} failed with no error edge, stopping",
                    upstream.getId());
            return List.of();
        }

        // 2. 显式条件边：命中任一条件边后不再回退到默认边。
        boolean hasExplicitConditionEdges = validEdges.stream()
                .anyMatch(e -> e.getType() == EdgeType.CONDITION
                        || e.getType() == EdgeType.CONDITION_TRUE
                        || e.getType() == EdgeType.CONDITION_FALSE);
        if (hasExplicitConditionEdges && output != null) {
            List<String> matched = selectConditionEdges(context, validEdges, output);
            if (!matched.isEmpty()) {
                log.debug("EdgeRouter: upstream node {} routed to {} condition edges",
                        upstream.getId(), matched.size());
                return matched;
            }
            log.debug("EdgeRouter: upstream node {} has condition edges but none matched", upstream.getId());
            return List.of();
        }

        // 3. 回退：所有非 ERROR 边
        return defaultEdges(validEdges);
    }

    /**
     * 路由条件边。
     *
     * <p>上游节点表达「选中了哪个分支」的方式并不统一，这里统一提取：
     * <ul>
     *   <li>{@code selectedBranch} —— IF / CONDITION / SWITCH / BRANCH 使用</li>
     *   <li>{@code matchedBranches}（列表）—— BRANCH 使用</li>
     *   <li>{@code category} —— AI_CLASSIFY / QUESTION_CLASSIFIER 使用</li>
     * </ul>
     * 它们都会与边的 label / config.branch 做比较；另外也支持边上的 expression。</p>
     */
    private List<String> selectConditionEdges(ExecutionContext context,
                                              List<Edge> validEdges,
                                              Map<String, Object> output) {
        boolean selectedIsTrue = toBoolean(output.get("selectedBranch"));
        boolean selectedIsFalse = isExplicitFalse(output.get("selectedBranch"));
        Set<String> keys = branchKeys(output);

        List<String> targets = new ArrayList<>();
        for (Edge edge : validEdges) {
            EdgeType type = edge.getType();
            if (type == EdgeType.CONDITION_TRUE) {
                if (selectedIsTrue) {
                    targets.add(edge.getTarget());
                }
            } else if (type == EdgeType.CONDITION_FALSE) {
                if (selectedIsFalse) {
                    targets.add(edge.getTarget());
                }
            } else if (type == EdgeType.CONDITION
                    && matchesConditionEdge(context, edge, keys, selectedIsTrue, selectedIsFalse)) {
                targets.add(edge.getTarget());
            }
        }
        return targets;
    }

    /** 收集上游结果里所有可用于匹配分支的键（分支名 / 分类名）。 */
    private Set<String> branchKeys(Map<String, Object> output) {
        Set<String> keys = new HashSet<>();
        Object selected = output.get("selectedBranch");
        if (selected != null) {
            addKey(keys, selected);
        }
        Object matched = output.get("matchedBranches");
        if (matched instanceof List<?> list) {
            for (Object item : list) {
                addKey(keys, item);
            }
        } else if (matched instanceof String text && !text.isBlank()) {
            // 兼容逗号分隔的字符串写法
            for (String part : text.split(",")) {
                addKey(keys, part);
            }
        }
        Object category = output.get("category");
        if (category != null) {
            addKey(keys, category);
        }
        return keys;
    }

    private void addKey(Set<String> keys, Object raw) {
        if (raw == null) return;
        String text = String.valueOf(raw).trim();
        if (!text.isEmpty()) {
            keys.add(text.toLowerCase());
        }
    }

    /** selectedBranch 是否显式表示「否」。 */
    private boolean isExplicitFalse(Object selected) {
        if (selected == null) return false;
        if (selected instanceof Boolean b) return !b;
        String s = selected.toString().trim();
        return s.equalsIgnoreCase("false") || s.equals("0")
                || s.isEmpty() || inferBooleanLabel(s) == Boolean.FALSE;
    }

    private boolean matchesConditionEdge(ExecutionContext context,
                                         Edge edge,
                                         Set<String> branchKeys,
                                         boolean selectedIsTrue,
                                         boolean selectedIsFalse) {
        Map<String, Object> config = edge.getData() != null ? edge.getData().getConfig() : null;
        if (config != null) {
            // 1) 显式条件表达式优先级最高
            Object expression = config.get("expression");
            if (expression != null && !expression.toString().isBlank()) {
                return evaluateConditionExpression(context, expression.toString());
            }
            // 2) 显式绑定分支名
            Object branch = config.get("branch");
            if (branch != null && !branch.toString().isBlank()
                    && branchKeys.contains(branch.toString().trim().toLowerCase())) {
                return true;
            }
        }

        // 3) 上游节点的「输出端口」也可作为分支标识。
        //    前端把端点名写在 edge.sourceHandle（true / false / 分支名）上，
        //    而 edge.label 可能为空或只是展示用的中文名。
        String port = edge.getSourceHandle();
        if (port != null && !port.isBlank()) {
            String normalizedPort = port.trim().toLowerCase();
            if (branchKeys.contains(normalizedPort)) {
                return true;
            }
            Boolean portKind = inferBooleanLabel(port.trim());
            if (portKind != null) {
                return portKind ? selectedIsTrue : selectedIsFalse;
            }
        }

        String label = edge.getData() != null ? edge.getData().getLabel() : null;
        if (label == null || label.isBlank()) {
            return false;
        }
        String normalizedLabel = label.trim();

        // 4) 边标签与上游分支名/分类名精确匹配（BRANCH / SWITCH / AI 分类）
        if (branchKeys.contains(normalizedLabel.toLowerCase())) {
            return true;
        }

        // 5) 布尔语义标签（"true"/"是"/"通过" …）与 selectedBranch 匹配
        Boolean labelKind = inferBooleanLabel(normalizedLabel);
        if (labelKind != null) {
            return labelKind ? selectedIsTrue : selectedIsFalse;
        }
        return false;
    }

    private boolean evaluateConditionExpression(ExecutionContext context, String expression) {
        Object resolved = context.resolveExpression(expression);
        if (resolved instanceof Boolean) return (Boolean) resolved;
        if (resolved instanceof Number) return ((Number) resolved).doubleValue() != 0;
        String s = String.valueOf(resolved).trim();
        if (s.equalsIgnoreCase("true")) return true;
        if (s.equalsIgnoreCase("false") || s.isEmpty()) return false;

        String[] operators = {"==", "!=", ">=", "<=", ">", "<",
                "contains", "startsWith", "endsWith"};
        for (String op : operators) {
            int idx = s.indexOf(op);
            if (idx > 0) {
                String leftExpr = s.substring(0, idx).trim();
                String rightExpr = s.substring(idx + op.length()).trim();
                Object left = context.resolveExpression(leftExpr);
                Object right = context.resolveExpression(rightExpr);
                return compare(left, op, right);
            }
        }
        return false;
    }

    private boolean compare(Object left, String op, Object right) {
        int cmp = compareValues(left, right);
        return switch (op) {
            case "==" -> cmp == 0;
            case "!=" -> cmp != 0;
            case ">=" -> cmp >= 0;
            case "<=" -> cmp <= 0;
            case ">" -> cmp > 0;
            case "<" -> cmp < 0;
            case "contains" -> left != null && left.toString().contains(right != null ? right.toString() : "");
            case "startsWith" -> left != null && left.toString().startsWith(right != null ? right.toString() : "");
            case "endsWith" -> left != null && left.toString().endsWith(right != null ? right.toString() : "");
            default -> false;
        };
    }

    private int compareValues(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return Double.compare(((Number) left).doubleValue(), ((Number) right).doubleValue());
        }
        return String.valueOf(left).compareTo(String.valueOf(right));
    }

    private Boolean inferBooleanLabel(String label) {
        Set<String> trueLabels = Set.of(
                "true", "是", "通过", "成功", "高", "够", "充足", "未注册", "有",
                "已逾期", "批量", "未超限", "可补", "完整", "全部补齐", "短假", "审核");
        Set<String> falseLabels = Set.of(
                "false", "否", "失败", "低", "不足", "已注册", "空", "无",
                "超支", "复核", "拒绝", "一般", "中等", "大额", "投诉", "通用",
                "FAQ", "转人工", "长假/黑名单", "未通过", "不通过");
        if (trueLabels.contains(label)) return true;
        if (falseLabels.contains(label)) return false;
        return null;
    }

    private boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0;
        String s = value.toString().trim();
        if (s.equalsIgnoreCase("true") || s.equals("1")) return true;
        if (s.equalsIgnoreCase("false") || s.equals("0")) return false;
        return false;
    }

    private List<String> defaultEdges(List<Edge> validEdges) {
        return validEdges.stream()
                .filter(e -> e.getType() != EdgeType.ERROR)
                .map(Edge::getTarget)
                .collect(Collectors.toList());
    }

    /**
     * 检查给定上游节点是否应该停止推进（无下游且非终止节点）。
     */
    public boolean shouldStop(NodeDefinition node) {
        return node.getType() != null && node.getType().isEnd();
    }
}
