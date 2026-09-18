package com.meowflow.workflow.engine;

import com.meowflow.common.context.CancellationToken;
import com.meowflow.common.context.DebugManager;
import com.meowflow.common.exception.NodeException;
import com.meowflow.common.exception.WorkflowException;
import com.meowflow.workflow.definition.Edge;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.definition.WorkflowDefinition;
import com.meowflow.workflow.event.RunEvent;
import com.meowflow.workflow.event.RunEventSink;
import com.meowflow.workflow.executor.NodeRegistry;
import com.meowflow.workflow.executor.NodeExecutor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WorkflowEngine {

    private final NodeRegistry nodeRegistry;
    private final EdgeRouter edgeRouter;
    private final NodeExecutionRecorder nodeExecutionRecorder;
    private final CancellationRegistry cancellationRegistry;
    private final Executor workflowExecutorPool;
    private final MeterRegistry meterRegistry;

    @Autowired(required = false)
    private RunEventSink eventSink;

    private final Counter executionSuccessCounter;
    private final Counter executionFailedCounter;

    /** DAG 拓扑缓存：避免每次执行都重新排序。Key: workflowId+version。*/
    private final Map<String, DAGSorter.SortResult> dagCache = new ConcurrentHashMap<>();

    /** 节点在途计数器（workflow_node_inflight 指标） */
    private final Map<String, AtomicInteger> nodeInflightCounts = new ConcurrentHashMap<>();

    /**
     * 节点执行专用线程池。
     *
     * <p>节点必须跑在**独立于工作流引擎**的线程上，否则无法实施超时中断：
     * 引擎线程若自己同步跑节点，超时只能等它自然结束（阻塞式节点会让
     * 节点级 timeout 完全失效）。</p>
     *
     * <p>同时也绝不能退回「提交回 workflowExecutorPool」的老做法：引擎自身就跑在
     * 那个池上，嵌套提交会在池满时自等待死锁。两个池彼此独立即可避免该问题。</p>
     *
     * <p>每个引擎线程在同一时刻只等一个节点，因此线程数按「并发执行数」而非
     * 「并行分支数」衡量即可。队列满时由调用方线程（引擎线程）兜底执行，
     * 保证不会拒绝任务、也不会死锁。</p>
     */
    private final ExecutorService nodeExecutionPool = new ThreadPoolExecutor(
            32, 64, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(256),
            r -> {
                Thread t = new Thread(r, "workflow-node-" + NODE_THREAD_SEQ.incrementAndGet());
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy());

    private static final AtomicInteger NODE_THREAD_SEQ = new AtomicInteger(0);

    /**
     * 节点超时与调试清理专用的轻量调度线程池（守护线程，单线程）。
     *
     * <p>只负责「到点触发 future 完成」和「延迟清理 DebugManager」，
     * 不执行任何节点逻辑。</p>
     */
    private final ScheduledExecutorService timeoutScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "workflow-timeout-scheduler");
                t.setDaemon(true);
                return t;
            });

    @Value("${workflow.engine.parallel.enabled:true}")
    private boolean parallelEnabled;

    public WorkflowEngine(NodeRegistry nodeRegistry,
                          EdgeRouter edgeRouter,
                          NodeExecutionRecorder nodeExecutionRecorder,
                          CancellationRegistry cancellationRegistry,
                          @Qualifier("workflowExecutorPool") Executor workflowExecutorPool,
                          MeterRegistry meterRegistry) {
        this.nodeRegistry = nodeRegistry;
        this.edgeRouter = edgeRouter;
        this.nodeExecutionRecorder = nodeExecutionRecorder;
        this.cancellationRegistry = cancellationRegistry;
        this.workflowExecutorPool = workflowExecutorPool;
        this.meterRegistry = meterRegistry;

        this.executionSuccessCounter = Counter.builder("workflow_execution_total")
                .tag("status", "success")
                .description("Total successful workflow executions")
                .register(meterRegistry);
        this.executionFailedCounter = Counter.builder("workflow_execution_total")
                .tag("status", "failed")
                .description("Total failed workflow executions")
                .register(meterRegistry);
    }

    public ExecutionResult execute(ExecutionContext context) {
        long startTime = System.currentTimeMillis();
        context.setStartTime(startTime);
        context.markRunning();

        // 注册取消令牌
        CancellationToken token = cancellationRegistry.register(context.getExecutionId());
        context.setCancellationToken(token);

        // 调试模式：挂载 DebugManager（如果用户预先设置了断点也能生效）
        // 同时注册执行结束钩子，确保清理
        DebugManager.attach(context.getExecutionId());

        emitEvent(RunEvent.executionStarted(context.getExecutionId(), context.getWorkflowId()));

        try {
            WorkflowDefinition definition = context.getDefinition();

            // 持久化的定义 JSON 里不含 workflowId / version（WorkflowDefinitionRequest
            // 没有这两个字段），这里用执行上下文补齐，供后续逻辑与调试展示使用。
            if (definition.getVersion() == null) {
                definition.setVersion(context.getVersion());
            }

            // 1. 获取拓扑排序（带缓存）
            DAGSorter.SortResult sortResult = getOrComputeDAG(context, definition);
            List<NodeDefinition> sortedNodes = sortResult.sortedNodes();
            Map<String, List<String>> outgoingEdgeTargets = sortResult.outgoingEdges();

            // 1c. 构建 source-target -> Edge 查找表（用于 EdgeRouter）
            Map<String, List<Edge>> outgoingEdges = new HashMap<>();
            for (Edge edge : definition.getEdges()) {
                outgoingEdges.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge);
            }

            if (sortedNodes.isEmpty()) {
                throw new WorkflowException(context.getWorkflowId().toString(), "工作流拓扑为空（无有效可执行节点）");
            }

            if (definition.getTriggerNodes().isEmpty()) {
                throw new WorkflowException(context.getWorkflowId().toString(), "工作流没有触发节点");
            }

            // 2. 构建 nodeId -> NodeDefinition 映射
            Map<String, NodeDefinition> nodeMap = sortedNodes.stream()
                    .collect(Collectors.toMap(NodeDefinition::getId, n -> n));

            // 3. 计算入度
            //
            // inDegrees 是**不可变**的拓扑事实，必须原样保留：兜底阶段要靠它判断
            // 某个节点是否还有未结算的上游。运行时递减的是 requiredArrivals，
            // 它是 inDegrees 的一份可变副本 —— 早期版本把 inDegrees 直接当
            // requiredArrivals 用，递减会把它改花，导致兜底阶段误判
            // （例如未命中分支的节点 inDegree 被减到 0，被当成「无依赖根节点」而执行）。
            Map<String, Integer> inDegrees = computeInDegrees(sortedNodes, definition);
            Map<String, Integer> requiredArrivals = new HashMap<>(inDegrees);
            Map<String, Integer> arrivedCounts = new HashMap<>();
            Map<String, Integer> joinArrived = new HashMap<>();
            Map<String, Boolean> joinReleased = new HashMap<>();
            Set<String> handledFailures = new HashSet<>();

            // 4. 初始化就绪队列（所有触发节点入队）
            Deque<String> readyQueue = new ArrayDeque<>();
            for (NodeDefinition trigger : definition.getTriggerNodes()) {
                if (nodeMap.containsKey(trigger.getId())) {
                    readyQueue.add(trigger.getId());
                    log.debug("Trigger node enqueued: {}", trigger.getId());
                }
            }

            // 5. 主循环：就绪队列驱动执行
            while (!readyQueue.isEmpty()) {
                context.checkCancellation();

                // 5a. 收集本批次所有就绪节点
                List<NodeDefinition> batch = new ArrayList<>();
                while (!readyQueue.isEmpty() && batch.size() < 64) {
                    String nid = readyQueue.poll();
                    NodeDefinition nd = nodeMap.get(nid);
                    if (nd != null && context.getNodeResult(nid) == null) {
                        batch.add(nd);
                    }
                }

                if (batch.isEmpty()) continue;

                // 5b. 并行或顺序执行本批次
                if (parallelEnabled && batch.size() > 1) {
                    executeBatchInParallel(context, batch, nodeMap, outgoingEdges,
                            definition, inDegrees, token, result -> {
                        // 每完成一个节点，就尝试推进下游
                        if (result != null && !result.isSuccess()) {
                            handleFailedNode(context, result, outgoingEdges, nodeMap, requiredArrivals,
                                    arrivedCounts, readyQueue, joinArrived, joinReleased, handledFailures);
                        } else if (result != null) {
                            List<Edge> nEdges = outgoingEdges.getOrDefault(result.getNodeId(), List.of());
                            List<String> downstreamIds = edgeRouter.selectDownstreamEdges(
                                    nodeMap.get(result.getNodeId()), nEdges, result, definition, context);
                            advanceDownstream(context, nodeMap.get(result.getNodeId()), nEdges,
                                    downstreamIds, requiredArrivals, arrivedCounts, readyQueue, nodeMap,
                                    joinArrived, joinReleased);
                        }
                    });
                } else {
                    // 顺序执行（阶段一/阶段二兼容模式）
                    for (NodeDefinition node : batch) {
                        context.checkCancellation();
                        NodeResult result = executeNodeWithTimeout(context, node, token);
                        nodeExecutionRecorder.recordFinished(context.getExecutionId(), node, result);
                        context.setNodeResult(node.getId(), result);

                        if (!result.isSuccess()) {
                            handleFailedNode(context, result, outgoingEdges, nodeMap, requiredArrivals,
                                    arrivedCounts, readyQueue, joinArrived, joinReleased, handledFailures);
                            continue;
                        }

                        List<Edge> nEdges = outgoingEdges.getOrDefault(node.getId(), List.of());
                        List<String> downstreamIds = edgeRouter.selectDownstreamEdges(
                                node, nEdges, result, definition, context);
                        advanceDownstream(context, node, nEdges, downstreamIds, requiredArrivals,
                                arrivedCounts, readyQueue, nodeMap, joinArrived, joinReleased);
                    }
                }
            }

            // 5c. 汇总所有已执行结束节点的输出，作为工作流最终输出。
            for (NodeDefinition node : sortedNodes) {
                NodeResult nodeResult = context.getNodeResult(node.getId());
                if (nodeResult != null && nodeResult.isSuccess()
                        && node.getType() != null && node.getType().isEnd()
                        && nodeResult.getOutput() != null) {
                    context.mergeOutput(nodeResult.getOutput());
                }
            }

            // 6. 兜底：结算仍未执行的节点，并级联推进拓扑。
            //
            // 这里必须跑到「不动点」：跳过某个节点会把它对下游的入边消耗掉，
            // 而这一步可能让原本不可达的下游变得**可执行**。典型场景是条件分支
            // 汇聚的菱形结构（A→B、A→C、B→D、C→D）：C 未命中被跳过，
            // 消耗掉 C→D 后 D 才就绪 —— 此时 D 应当执行，而不是被标成
            // "Unreachable node" 让工作流静默丢结果。
            settleUnreachedNodes(context, sortedNodes, outgoingEdges, nodeMap, inDegrees,
                    requiredArrivals, arrivedCounts, readyQueue, joinArrived, joinReleased);

            // 7. 汇总结果
            boolean anyFailed = context.getNodeResults().values().stream()
                    .anyMatch(r -> r.isFailed() && !handledFailures.contains(r.getNodeId()));
            boolean anyCancelled = context.getNodeResults().values().stream()
                    .anyMatch(r -> r.getStatus() == NodeResult.NodeStatus.CANCELLED
                                || r.getStatus() == NodeResult.NodeStatus.TIMED_OUT);

            long costMs = System.currentTimeMillis() - startTime;

            if (anyFailed) {
                context.markFailed("One or more nodes failed");
                executionFailedCounter.increment();
                emitEvent(RunEvent.executionFailed(context.getExecutionId(), context.getWorkflowId(),
                        "One or more nodes failed"));
                return ExecutionResult.failed(
                        context.getExecutionId(),
                        context.getWorkflowId(),
                        context.getVersion(),
                        context.getInput(),
                        "One or more nodes failed",
                        costMs
                );
            }

            if (anyCancelled) {
                context.markCancelled("Workflow was cancelled or timed out");
                executionFailedCounter.increment();
                emitEvent(RunEvent.executionCancelled(context.getExecutionId(), context.getWorkflowId(),
                        "cancelled_or_timed_out"));
                return ExecutionResult.cancelled(
                        context.getExecutionId(),
                        context.getWorkflowId(),
                        context.getVersion(),
                        context.getInput(),
                        costMs
                );
            }

            context.markSuccess();
            executionSuccessCounter.increment();
            // 便于前端与排障确认「这次跑的到底是哪个版本」
            Map<String, Object> successOutput = context.getOutput();
            if (successOutput != null && context.getVersion() != null) {
                successOutput.put("_version", context.getVersion());
            }
            emitEvent(RunEvent.executionSucceeded(context.getExecutionId(), context.getWorkflowId(),
                    context.getOutput()));
            return ExecutionResult.success(
                    context.getExecutionId(),
                    context.getWorkflowId(),
                    context.getVersion(),
                    context.getInput(),
                    context.getOutput(),
                    costMs
            );

        } catch (WorkflowCancelledException e) {
            log.info("Workflow {} cancelled: {}", context.getExecutionId(), e.getMessage());
            // 用户主动取消不是「失败」，必须回传 cancelled，
            // 否则前端与统计会把取消当成执行错误。
            context.markCancelled(e.getMessage());
            emitEvent(RunEvent.executionCancelled(context.getExecutionId(), context.getWorkflowId(),
                    e.getMessage()));
            return ExecutionResult.cancelled(
                    context.getExecutionId(),
                    context.getWorkflowId(),
                    context.getVersion(),
                    context.getInput(),
                    context.getElapsedMs()
            );
        } catch (WorkflowException e) {
            log.error("Workflow execution failed: {}", e.getMessage(), e);
            context.markFailed(e.getMessage());
            return ExecutionResult.failed(
                    context.getExecutionId(),
                    context.getWorkflowId(),
                    context.getVersion(),
                    context.getInput(),
                    e.getMessage(),
                    context.getElapsedMs()
            );
        } catch (Exception e) {
            log.error("Unexpected error during workflow execution", e);
            context.markFailed(e.getMessage());
            return ExecutionResult.failed(
                    context.getExecutionId(),
                    context.getWorkflowId(),
                    context.getVersion(),
                    context.getInput(),
                    e.getMessage(),
                    context.getElapsedMs()
            );
        } finally {
            cancellationRegistry.unregister(context.getExecutionId());
            // 调试模式：清理 DebugManager（保留快照 30 秒以便用户事后查询，然后销毁）
            scheduleDebugManagerCleanup(context.getExecutionId());
        }
    }

    /**
     * 延迟销毁 DebugManager：保留 30 秒供前端查询最终变量状态。
     *
     * <p>用调度线程池的延时任务实现，绝不能占用 {@code workflowExecutorPool}
     * 的线程去 sleep —— 那会白白吃掉节点执行容量。</p>
     */
    private void scheduleDebugManagerCleanup(Long executionId) {
        try {
            timeoutScheduler.schedule(
                    () -> DebugManager.detach(executionId), 30, TimeUnit.SECONDS);
        } catch (RejectedExecutionException e) {
            // 调度器已关闭（应用停机中），直接清理即可
            DebugManager.detach(executionId);
        }
    }

    private NodeResult executeNodeWithRetry(ExecutionContext context,
                                            NodeDefinition node,
                                            NodeExecutor executor) {
        int maxRetries = resolveNodeRetries(node);
        boolean retryOnFail = resolveNodeRetryOnFail(node, maxRetries);
        NodeResult result = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                result = executor.execute(context, node);
            } catch (NodeException e) {
                result = NodeResult.failed(node.getId(), node.getType(), node.getName(), e.getMessage(), e);
            } catch (Exception e) {
                result = NodeResult.failed(node.getId(), node.getType(), node.getName(), e.getMessage(), e);
            }

            if (result.isSuccess() || attempt >= maxRetries || !retryOnFail) {
                break;
            }

            log.warn("Node {} failed (attempt {}/{}), retrying: {}",
                    node.getId(), attempt + 1, maxRetries + 1, result.getErrorMessage());
            emitEvent(RunEvent.nodeRetrying(context.getExecutionId(), context.getWorkflowId(),
                    node.getId(), attempt + 1, maxRetries + 1));
            try {
                Thread.sleep(500L * (attempt + 1));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (result == null) {
            return NodeResult.failed(node.getId(), node.getType(), node.getName(),
                    "Node executor returned null", null);
        }
        return result;
    }

    private NodeResult applyFallbackOnError(ExecutionContext context,
                                            NodeDefinition node,
                                            NodeResult result) {
        if (result == null) {
            return NodeResult.failed(node.getId(), node.getType(), node.getName(),
                    "Node executor returned null", null);
        }
        if (result.isSuccess()) {
            return result;
        }

        Object fallback = readConfigValue(node.getData(), "fallbackValue", null);
        Object onError = readConfigValue(node.getData(), "onError", null);
        if (onError instanceof Map && ((Map<?, ?>) onError).get("defaultValue") != null) {
            fallback = ((Map<?, ?>) onError).get("defaultValue");
        }
        if (fallback == null) {
            return result;
        }

        Object resolved = fallback instanceof String
                ? context.resolveExpression((String) fallback)
                : fallback;
        Map<String, Object> output = new HashMap<>();
        if (resolved instanceof Map) {
            output.putAll((Map<String, Object>) resolved);
        } else {
            output.put("value", resolved);
        }
        output.put("_originalError", result.getErrorMessage());

        log.info("Node {} used fallback value after error: {}", node.getId(), result.getErrorMessage());
        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }

    private int resolveNodeRetries(NodeDefinition node) {
        Object value = readConfigValue(node.getData(), "retries", null);
        if (value == null) {
            value = readConfigValue(node.getData(), "retryTimes", null);
        }
        if (value == null) return 0;
        try {
            return Math.max(0, Math.min(10, Integer.parseInt(value.toString())));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean resolveNodeRetryOnFail(NodeDefinition node, int maxRetries) {
        if (maxRetries <= 0) return false;
        Object value = readConfigValue(node.getData(), "retryOnFail", true);
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }

    private void handleFailedNode(ExecutionContext context,
                                  NodeResult result,
                                  Map<String, List<Edge>> outgoingEdges,
                                  Map<String, NodeDefinition> nodeMap,
                                  Map<String, Integer> inDegrees,
                                  Map<String, Integer> arrivedCounts,
                                  Deque<String> readyQueue,
                                  Map<String, Integer> joinArrived,
                                  Map<String, Boolean> joinReleased,
                                  Set<String> handledFailures) {
        if (result == null || result.isSuccess()) return;
        NodeDefinition upstream = nodeMap.get(result.getNodeId());
        if (upstream == null) return;

        List<Edge> nEdges = outgoingEdges.getOrDefault(result.getNodeId(), List.of());
        List<String> errorTargets = edgeRouter.selectDownstreamEdges(
                upstream, nEdges, result, context.getDefinition(), context);
        if (errorTargets.isEmpty()) return;

        handledFailures.add(result.getNodeId());
        advanceDownstream(context, upstream, nEdges, errorTargets, inDegrees, arrivedCounts,
                readyQueue, nodeMap, joinArrived, joinReleased);
    }

    /**
     * 带超时控制的节点执行。
     */
    private NodeResult executeNodeWithTimeout(ExecutionContext context,
                                               NodeDefinition node,
                                               CancellationToken token) {
        // NOTE 节点：纯 UI 标注，直接成功
        if (node.getType() == NodeType.NOTE) {
            return NodeResult.success(node.getId(), node.getType(), node.getName(), null);
        }

        // ================================================================
        // 调试模式：在节点执行前检查断点/单步状态
        // 参考 Coze Studio debug 包设计
        // ================================================================
        DebugManager debug = DebugManager.get(context.getExecutionId());
        if (debug != null) {
            if (debug.isStopped()) {
                // 用户在调试面板点击 Stop：立即取消本次执行
                return NodeResult.cancelled(node.getId(), node.getType(), node.getName(),
                        "Debug stopped by user");
            }
            if (debug.shouldPauseBefore(node.getId())) {
                String reason = debug.hasBreakpoint(node.getId()) ? "BREAKPOINT" : "STEPPING";
                log.info("Pausing at node {} (reason={})", node.getId(), reason);
                debug.pauseAndAwait(node.getId(), reason, token);
                // 用户 resume 后若又触发 stop，则再次检查
                if (debug.isStopped()) {
                    return NodeResult.cancelled(node.getId(), node.getType(), node.getName(),
                            "Debug stopped by user");
                }
                // 暂停期间用户取消了执行：必须在这里退出，
                // 否则线程会一直停到断点结束后才检查令牌。
                if (token.isCancelled()) {
                    return NodeResult.cancelled(node.getId(), node.getType(), node.getName(),
                            token.getCancelReason());
                }
            }
        }

        NodeExecutor executor = nodeRegistry.getExecutor(node.getType());
        if (executor == null) {
            return NodeResult.failed(
                    node.getId(),
                    node.getType(),
                    node.getName(),
                    "Unsupported node type: " + node.getType(),
                    null
            );
        }

        Duration timeout = context.getNodeTimeout(node);
        log.debug("Executing node {} with timeout {}", node.getId(), timeout);

        // 立即记录开始（可让前端看到 "running" 状态）
        nodeExecutionRecorder.recordStarted(context.getExecutionId(), node, null);
        emitEvent(RunEvent.nodeStarted(context.getExecutionId(), context.getWorkflowId(),
                node.getId(), node.getType() != null ? node.getType().getCode() : null, node.getName()));

        // 在途计数器 +1
        String nodeTypeKey = node.getType() != null ? node.getType().getCode() : "unknown";
        AtomicInteger inflight = nodeInflightCounts.computeIfAbsent(nodeTypeKey, k -> {
            AtomicInteger cnt = new AtomicInteger(0);
            Gauge.builder("workflow_node_inflight", cnt, AtomicInteger::get)
                    .tag("type", k)
                    .description("Number of nodes currently executing")
                    .register(meterRegistry);
            return cnt;
        });
        inflight.incrementAndGet();

        Timer.Sample timerSample = Timer.start(meterRegistry);

        // ================================================================
        // 节点执行：提交到**独立的节点线程池**，由本线程带超时等待。
        //
        // 两个设计要点：
        // 1) 节点跑在别的线程上，超时才能真正中断它（阻塞式节点尤其重要）；
        //    若在当前线程同步执行，orTimeout 只是给一个已经同步完成的 future
        //    设闹钟，永远不会生效。
        // 2) 池必须与 workflowExecutorPool 分离。引擎本身跑在那个池上，
        //    早期版本把节点任务再提交回同一个池，队列深达 1000 永不扩容，
        //    12 路并行就会把 core 线程全部占死并自等待，最终整个服务再也
        //    执行不了任何工作流（而 /actuator/health 仍是 200）。
        // ================================================================
        CompletableFuture<NodeResult> future = CompletableFuture
                .supplyAsync(() -> {
                    // 节点执行过程中也要周期检查取消
                    try {
                        NodeResult result = executeNodeWithRetry(context, node, executor);
                        return applyFallbackOnError(context, node, result);
                    } catch (NodeException e) {
                        log.error("Node {} failed: {}", node.getId(), e.getMessage());
                        return NodeResult.failed(node.getId(), node.getType(), node.getName(),
                                e.getMessage(), e);
                    } catch (Exception e) {
                        log.error("Unexpected error in node {}", node.getId(), e);
                        return NodeResult.failed(node.getId(), node.getType(), node.getName(),
                                e.getMessage(), e);
                    }
                }, nodeExecutionPool);

        // 超时后中断节点线程，并让 future 以 TimeoutException 完成
        try {
            timeoutScheduler.schedule(() -> {
                if (future.completeExceptionally(
                        new TimeoutException("Node timeout: " + node.getId()))) {
                    future.cancel(true);
                }
            }, timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ree) {
            log.warn("Timeout scheduler rejected for node {}, running without timeout: {}",
                    node.getId(), ree.getMessage());
        }

        // 轮询取消信号（最多 timeout 时长 + 少量宽限，等待 exceptionally 映射结果）
        long deadline = System.currentTimeMillis() + timeout.toMillis() + 2000L;
        while (!future.isDone()) {
            if (token.isCancelled()) {
                future.cancel(true);
                inflight.decrementAndGet();
                NodeResult cancelledResult = NodeResult.cancelled(node.getId(), node.getType(), node.getName(),
                        token.getCancelReason());
                timerSample.stop(Timer.builder("workflow_node_duration_seconds")
                        .tag("type", nodeTypeKey)
                        .tag("status", "cancelled")
                        .register(meterRegistry));
                emitEvent(RunEvent.nodeFinished(context.getExecutionId(), context.getWorkflowId(),
                        node.getId(), node.getType() != null ? node.getType().getCode() : null, node.getName(),
                        "cancelled", null, null, null));
                return cancelledResult;
            }
            if (System.currentTimeMillis() > deadline) {
                // 调度器未能触发（例如被拒绝），按超时处理
                future.completeExceptionally(new TimeoutException("Node timeout: " + node.getId()));
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                future.cancel(true);
                inflight.decrementAndGet();
                return NodeResult.cancelled(node.getId(), node.getType(), node.getName(), "Interrupted");
            }
        }

        try {
            NodeResult result = future.get();
            inflight.decrementAndGet();
            String statusTag = result.getStatus() != null ? result.getStatus().name().toLowerCase() : "unknown";
            timerSample.stop(Timer.builder("workflow_node_duration_seconds")
                    .tag("type", nodeTypeKey)
                    .tag("status", statusTag)
                    .register(meterRegistry));
            emitEvent(RunEvent.nodeFinished(context.getExecutionId(), context.getWorkflowId(),
                    node.getId(), node.getType() != null ? node.getType().getCode() : null, node.getName(),
                    statusTag, result.getOutput(), result.getCostMs(), result.getCostToken()));

            // ================================================================
            // 调试模式：记录节点执行快照，供前端变量查看器使用。
            // 这是纯粹的旁路记录，绝不能因为它的异常把整个执行打成 failed。
            // ================================================================
            try {
                DebugManager debugAfter = DebugManager.get(context.getExecutionId());
                if (debugAfter != null && result.getOutput() != null) {
                    debugAfter.recordSnapshot(node.getId(), result.getOutput());
                }
            } catch (Exception snapshotError) {
                log.warn("Failed to record debug snapshot for node {}: {}",
                        node.getId(), snapshotError.getMessage());
            }

            return result;
        } catch (CancellationException ce) {
            inflight.decrementAndGet();
            return NodeResult.cancelled(node.getId(), node.getType(), node.getName(),
                    token.getCancelReason());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            inflight.decrementAndGet();
            return NodeResult.cancelled(node.getId(), node.getType(), node.getName(), "Interrupted");
        } catch (ExecutionException ee) {
            inflight.decrementAndGet();
            Throwable cause = ee.getCause() != null ? ee.getCause() : ee;
            if (cause instanceof TimeoutException) {
                log.warn("Node {} timed out after {}", node.getId(), timeout);
                timerSample.stop(Timer.builder("workflow_node_duration_seconds")
                        .tag("type", nodeTypeKey)
                        .tag("status", "timed_out")
                        .register(meterRegistry));
                emitEvent(RunEvent.nodeFinished(context.getExecutionId(), context.getWorkflowId(),
                        node.getId(), node.getType() != null ? node.getType().getCode() : null, node.getName(),
                        "timed_out", null, null, null));
                return NodeResult.timedOut(node.getId(), node.getType(), node.getName(), timeout);
            }
            log.error("Unexpected exception awaiting node {}", node.getId(), ee);
            return NodeResult.failed(node.getId(), node.getType(), node.getName(),
                    cause.getMessage(), cause);
        }
    }

    /**
     * 计算入度。
     *
     * <p>必须与 {@link DAGSorter} 保持一致：DAGSorter 会剔除所有「接触 NOTE 节点」
     * 的边（NOTE 是纯 UI 装饰），这里若按原始边表计数，内联注释后面的节点会永远
     * 差一个到达数，从而被静默跳过却让工作流报成功。</p>
     */
    private Map<String, Integer> computeInDegrees(List<NodeDefinition> nodes, WorkflowDefinition definition) {
        Map<String, Integer> inDegrees = new HashMap<>();
        Set<String> executableIds = new HashSet<>();
        Set<String> noteIds = new HashSet<>();
        for (NodeDefinition node : nodes) {
            inDegrees.put(node.getId(), 0);
            executableIds.add(node.getId());
        }
        if (definition.getNodes() != null) {
            for (NodeDefinition node : definition.getNodes()) {
                if (node.getType() == NodeType.NOTE) {
                    noteIds.add(node.getId());
                }
            }
        }
        if (definition.getEdges() != null) {
            for (Edge edge : definition.getEdges()) {
                // 跳过接触 NOTE 的边，与 DAGSorter 的过滤规则对齐
                if (noteIds.contains(edge.getSource()) || noteIds.contains(edge.getTarget())) {
                    continue;
                }
                if (executableIds.contains(edge.getTarget())) {
                    inDegrees.merge(edge.getTarget(), 1, Integer::sum);
                }
            }
        }
        return inDegrees;
    }

    /**
     * 推进一个已执行节点的所有出边。
     *
     * <p>与静态入度不同：每个上游节点执行后，它的所有出边都会释放一个“依赖名额”，
     * 只有被选中的边才增加到达计数。这样条件分支未命中的下游不会因为永远等不到
     * 入度而卡死，也不会被错误地执行。</p>
     */
    /**
     * 结算所有仍未执行的节点：能执行的执行，不能执行的标记为跳过。
     *
     * <p>反复迭代直到不动点 —— 「跳过」会消耗下游的未决入边，这可能让下游
     * 从不该执行变成应该执行。判定标准与 {@link #advanceDownstream} 完全一致：
     * 所有入边的上游都已结算（{@code remaining == 0}）且至少有一条真实命中
     * （{@code arrived > 0}）即视为可达。</p>
     */
    private void settleUnreachedNodes(ExecutionContext context,
                                      List<NodeDefinition> sortedNodes,
                                      Map<String, List<Edge>> outgoingEdges,
                                      Map<String, NodeDefinition> nodeMap,
                                      Map<String, Integer> inDegrees,
                                      Map<String, Integer> requiredArrivals,
                                      Map<String, Integer> arrivedCounts,
                                      Deque<String> readyQueue,
                                      Map<String, Integer> joinArrived,
                                      Map<String, Boolean> joinReleased) {
        Set<String> triggerIds = new HashSet<>();
        for (NodeDefinition node : sortedNodes) {
            if (node.getType() != null && node.getType().isTrigger()) {
                triggerIds.add(node.getId());
            }
        }

        boolean progressed = true;
        while (progressed) {
            progressed = false;
            for (NodeDefinition node : sortedNodes) {
                if (context.getNodeResult(node.getId()) != null) continue;

                int total = inDegrees.getOrDefault(node.getId(), 0);
                int pending = requiredArrivals.getOrDefault(node.getId(), 0);
                int arrived = arrivedCounts.getOrDefault(node.getId(), 0);

                // 只有在整个图中都没有入边的节点才算根节点。
                // 这里必须用不可变的 inDegrees 判断，不能用被递减过的 pending：
                // 未命中分支的节点其 pending 会被减到 0，若据此当成「无依赖根节点」
                // 执行，就会把不该跑的分支跑起来。
                if (total == 0) {
                    // 触发节点早已处理过；这里的根节点是编辑器里游离的节点
                    if (!triggerIds.contains(node.getId())) {
                        readyQueue.add(node.getId());
                    }
                    break;
                }

                // 所有入边的上游都已结算，且至少有一条真实命中 -> 可达
                if (pending == 0 && arrived > 0) {
                    break;
                }

                markSkippedAndCascade(context, node, outgoingEdges, nodeMap, requiredArrivals,
                        arrivedCounts, readyQueue, joinArrived, joinReleased, "Unreachable node");
                progressed = true;
            }
        }

        // 级联后才变得可执行的节点，在就绪队列里正常跑完
        executeReadyQueue(context, nodeMap, outgoingEdges, requiredArrivals, arrivedCounts,
                readyQueue, joinArrived, joinReleased);
    }

    /** 把就绪队列里的节点全部执行完（兜底阶段使用）。 */
    private void executeReadyQueue(ExecutionContext context,
                                   Map<String, NodeDefinition> nodeMap,
                                   Map<String, List<Edge>> outgoingEdges,
                                   Map<String, Integer> inDegrees,
                                   Map<String, Integer> arrivedCounts,
                                   Deque<String> readyQueue,
                                   Map<String, Integer> joinArrived,
                                   Map<String, Boolean> joinReleased) {
        while (!readyQueue.isEmpty()) {
            context.checkCancellation();
            String nid = readyQueue.poll();
            NodeDefinition node = nodeMap.get(nid);
            if (node == null || context.getNodeResult(nid) != null) continue;

            NodeResult result = executeNodeWithTimeout(context, node, context.getCancellationToken());
            nodeExecutionRecorder.recordFinished(context.getExecutionId(), node, result);
            context.setNodeResult(node.getId(), result);

            if (!result.isSuccess()) continue;

            List<Edge> nEdges = outgoingEdges.getOrDefault(node.getId(), List.of());
            List<String> downstreamIds = edgeRouter.selectDownstreamEdges(
                    node, nEdges, result, context.getDefinition(), context);
            advanceDownstream(context, node, nEdges, downstreamIds, inDegrees,
                    arrivedCounts, readyQueue, nodeMap, joinArrived, joinReleased);
        }
    }

    /**
     * 把节点标记为「跳过」，并级联结算它对下游的依赖。
     *
     * <p>被跳过的节点不会有执行结果，因此它永远不会走正常的
     * {@link #advanceDownstream} 流程。若不在跳过时替它结算下游的
     * 「未决入边」，条件分支汇聚处会因为未命中的那条分支始终未结算而
     * 永远差一个到达数。</p>
     *
     * <p>级联时 {@code selectedTargets} 传空：跳过意味着「没有选中任何下游」，
     * 只做依赖消耗，不做到达计数。</p>
     */
    private void markSkippedAndCascade(ExecutionContext context,
                                       NodeDefinition node,
                                       Map<String, List<Edge>> outgoingEdges,
                                       Map<String, NodeDefinition> nodeMap,
                                       Map<String, Integer> inDegrees,
                                       Map<String, Integer> arrivedCounts,
                                       Deque<String> readyQueue,
                                       Map<String, Integer> joinArrived,
                                       Map<String, Boolean> joinReleased,
                                       String reason) {
        if (context.getNodeResult(node.getId()) != null) return;

        NodeResult skipped = NodeResult.skipped(node.getId(), node.getType(), node.getName());
        context.setNodeResult(node.getId(), skipped);
        nodeExecutionRecorder.recordSkipped(context.getExecutionId(), node, reason);

        // 先标记结果、再级联：advanceDownstream 内会用 getNodeResult != null
        // 防止把已跳过的节点重新入队。
        advanceDownstream(context, node, outgoingEdges.getOrDefault(node.getId(), List.of()),
                List.of(), inDegrees, arrivedCounts, readyQueue, nodeMap, joinArrived, joinReleased);
    }

    private void advanceDownstream(ExecutionContext context,
                                   NodeDefinition upstream,
                                   List<Edge> outgoing,
                                   List<String> selectedTargets,
                                   Map<String, Integer> requiredArrivals,
                                   Map<String, Integer> arrivedCounts,
                                   Deque<String> readyQueue,
                                   Map<String, NodeDefinition> nodeMap,
                                   Map<String, Integer> joinArrived,
                                   Map<String, Boolean> joinReleased) {
        if (upstream == null || outgoing == null) return;
        Set<String> selected = new HashSet<>(selectedTargets);

        for (Edge edge : outgoing) {
            String targetId = edge.getTarget();
            NodeDefinition downstream = nodeMap.get(targetId);
            if (downstream == null || downstream.getType() == NodeType.NOTE) continue;

            // 只要这条边的上游节点执行结束，就消耗掉该下游的一个「未决入边」。
            // 注意：**未选中的分支也要消耗**，否则条件分支汇聚处会永远等不到。
            int remaining = Math.max(0, requiredArrivals.getOrDefault(targetId, 0) - 1);
            requiredArrivals.put(targetId, remaining);

            boolean selectedHere = selected.contains(targetId);
            if (selectedHere) {
                arrivedCounts.merge(targetId, 1, Integer::sum);
            }

            if (downstream.getType() == NodeType.JOIN) {
                if (!selectedHere) continue;
                int arrived = joinArrived.merge(targetId, 1, Integer::sum);
                if (!joinReleased.getOrDefault(targetId, false)
                        && isJoinReady(downstream, remaining, arrived,
                                arrivedCounts.getOrDefault(targetId, 0))
                        && context.getNodeResult(targetId) == null) {
                    joinReleased.put(targetId, true);
                    readyQueue.add(targetId);
                }
                continue;
            }

            // 所有入边的上游都已执行结束（remainingArrivals == 0），且至少有一条
            // 真正命中（arrivedCounts > 0）时，该节点才就绪。
            //
            // 这个判断必须在「未选中分支」上也执行：条件分支汇聚的菱形结构里，
            // 未命中分支的那条边是最后一个把 remainingArrivals 减到 0 的，
            // 若提前 continue 跳过判断，汇聚节点就永远不会入队、被静默标成
            // "Unreachable node"，工作流却报 success。
            if (remaining == 0
                    && arrivedCounts.getOrDefault(targetId, 0) > 0
                    && context.getNodeResult(targetId) == null) {
                readyQueue.add(targetId);
            }
        }
    }

    private boolean isJoinReady(NodeDefinition join,
                                int remainingArrivals,
                                int joinArrived,
                                int totalArrived) {
        Map<String, Object> data = join.getData();
        Object strategy = readConfigValue(data, "strategy", "ALL");
        if ("ANY".equalsIgnoreCase(strategy.toString())) {
            return joinArrived >= 1;
        }
        if ("N_OF_M".equalsIgnoreCase(strategy.toString())) {
            Object required = readConfigValue(data, "requiredCount", 1);
            try {
                return joinArrived >= Math.max(1, Integer.parseInt(required.toString()));
            } catch (NumberFormatException e) {
                return joinArrived >= 1;
            }
        }
        return remainingArrivals == 0 && totalArrived > 0;
    }

    private Object readConfigValue(Map<String, Object> data, String key, Object defaultValue) {
        if (data == null) return defaultValue;
        Object nested = data.get("config");
        if (nested instanceof Map && ((Map<?, ?>) nested).containsKey(key)) {
            return ((Map<?, ?>) nested).get(key);
        }
        return data.getOrDefault(key, defaultValue);
    }

    /**
     * 取拓扑排序结果（带缓存）。
     *
     * <p>缓存键必须由执行上下文提供。持久化的工作流定义 JSON 里不含 workflowId
     * （{@code WorkflowDefinitionRequest} 没有该字段），若直接用
     * {@code definition.getWorkflowId()} 取键，所有工作流都会落到同一个 "null:null"
     * 条目上，导致第二个工作流复用第一个工作流的拓扑、一个节点都不执行却返回
     * success。</p>
     */
    private DAGSorter.SortResult getOrComputeDAG(ExecutionContext context, WorkflowDefinition definition) {
        String cacheKey = cacheKeyOf(context.getWorkflowId(), context.getVersion());
        return dagCache.computeIfAbsent(cacheKey, k -> DAGSorter.topologicalSort(definition));
    }

    private static String cacheKeyOf(Long workflowId, String version) {
        return workflowId + ":" + version;
    }

    /**
     * 同步执行（兼容旧调用方）。
     */
    public NodeResult executeNode(ExecutionContext context, NodeDefinition node) {
        if (node.getType() == NodeType.NOTE) {
            return NodeResult.success(node.getId(), node.getType(), node.getName(), null);
        }
        NodeExecutor executor = nodeRegistry.getExecutor(node.getType());
        if (executor == null) {
            return NodeResult.failed(node.getId(), node.getType(), node.getName(),
                    "Unsupported node type: " + node.getType(), null);
        }
        try {
            return executor.execute(context, node);
        } catch (NodeException e) {
            log.error("Node {} failed: {}", node.getId(), e.getMessage(), e);
            return NodeResult.failed(node.getId(), node.getType(), node.getName(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error executing node {}", node.getId(), e);
            return NodeResult.failed(node.getId(), node.getType(), node.getName(), e.getMessage(), e);
        }
    }

    public CompletableFuture<ExecutionResult> executeAsync(ExecutionContext context) {
        return CompletableFuture.supplyAsync(() -> execute(context), workflowExecutorPool);
    }

    /**
     * 发布前编译校验。
     * 校验通过则将拓扑结果缓存到 dagCache。
     */
    public void compileAndValidate(WorkflowDefinition definition, Long workflowId) {
        String cacheKey = workflowId + ":" + definition.getVersion();
        dagCache.computeIfAbsent(cacheKey, k -> DAGSorter.topologicalSort(definition));
    }

    public boolean validateDefinition(WorkflowDefinition definition) {
        if (definition.getNodes() == null || definition.getNodes().isEmpty()) {
            return false;
        }
        if (definition.getTriggerNodes().isEmpty()) {
            return false;
        }
        if (DAGSorter.hasCycle(definition)) {
            return false;
        }
        return true;
    }

    /**
     * 并行执行一批节点（最多 64 个）。
     *
     * <p>所有节点完成后统一推进下游。失败节点不阻塞其他节点。</p>
     *
     * @param onNodeDone 每节点完成后的回调（用于推进下游入度）
     */
    private void executeBatchInParallel(
            ExecutionContext context,
            List<NodeDefinition> batch,
            Map<String, NodeDefinition> nodeMap,
            Map<String, List<Edge>> outgoingEdges,
            WorkflowDefinition definition,
            Map<String, Integer> inDegrees,
            CancellationToken token,
            java.util.function.Consumer<NodeResult> onNodeDone) {

        List<CompletableFuture<NodeResult>> futures = new ArrayList<>();

        for (NodeDefinition node : batch) {
            CompletableFuture<NodeResult> f = CompletableFuture
                    .supplyAsync(() -> {
                        if (token.isCancelled()) {
                            return NodeResult.cancelled(node.getId(), node.getType(), node.getName(),
                                    token.getCancelReason());
                        }
                        return executeNodeWithTimeout(context, node, token);
                    }, workflowExecutorPool)
                    .exceptionally(ex -> {
                        log.error("Unexpected exception in parallel node {}: {}", node.getId(), ex.getMessage());
                        return NodeResult.failed(node.getId(), node.getType(), node.getName(),
                                ex.getMessage(), ex);
                    });
            futures.add(f);
        }

        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        try {
            allOf.join();
        } catch (Exception e) {
            log.warn("Parallel batch completed with exception: {}", e.getMessage());
        }

        for (int i = 0; i < batch.size(); i++) {
            NodeDefinition node = batch.get(i);
            NodeResult result;
            try {
                result = futures.get(i).getNow(NodeResult.failed(node.getId(), node.getType(), node.getName(),
                        "Batch interrupted", null));
            } catch (Exception e) {
                result = NodeResult.failed(node.getId(), node.getType(), node.getName(),
                        e.getMessage(), e);
            }

            nodeExecutionRecorder.recordFinished(context.getExecutionId(), node, result);
            context.setNodeResult(node.getId(), result);

            if (onNodeDone != null) {
                onNodeDone.accept(result);
            }
        }
    }

    private void emitEvent(RunEvent event) {
        if (eventSink != null) {
            try {
                eventSink.append(event);
            } catch (Exception e) {
                log.warn("Failed to emit event {}: {}", event.getEvent(), e.getMessage());
            }
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setEventSink(RunEventSink eventSink) {
        this.eventSink = eventSink;
    }

    @jakarta.annotation.PreDestroy
    public void shutdownTimeoutScheduler() {
        timeoutScheduler.shutdownNow();
        nodeExecutionPool.shutdownNow();
    }
}
