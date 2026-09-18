package com.meowflow.common.context;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 调试管理器（断点 + 单步执行）。
 *
 * 设计参考 Dify/Coze Studio 调试模式：
 * - 用户在画布上为节点打点断点
 * - 执行到断点节点时引擎自动暂停（节点状态变为 PAUSED）
 * - 前端可调用 resume() 继续 / step() 单步 / stop() 终止
 * - 单步模式：执行完一个节点后再次暂停（即使该节点未设断点）
 *
 * <p>状态机：
 * <pre>
 *   RUNNING ──遇到断点──> PAUSED ──resume──> RUNNING
 *      │                    │
 *      │                    └────step────> RUNNING (执行下一节点后再次 PAUSED)
 *      └────cancel──> CANCELLED
 * </pre>
 *
 * <p>使用示例（WorkflowEngine）：
 * <pre>{@code
 *   DebugManager dm = DebugManager.attach(executionId);
 *   dm.addBreakpoint("node_123");
 *   // ...
 *   if (dm.shouldPauseBefore("node_123")) {
 *       dm.pauseAndAwait("node_123");   // 阻塞直到用户 resume/step
 *   }
 * }</pre>
 *
 * <p>注意：每个执行实例独立持有 DebugManager；并发执行多个工作流互不干扰。
 */
public class DebugManager {

    /** 调试状态 */
    public enum State {
        /** 正常运行 */
        RUNNING,
        /** 已暂停，等待 resume/step */
        PAUSED,
        /** 单步模式：下一个节点执行完毕后立即再次暂停 */
        STEPPING,
        /** 已停止（用户主动 stop 或 cancel） */
        STOPPED
    }

    @Getter
    private final Long executionId;

    /** 节点断点集合（节点 id 集合） */
    private final Set<String> breakpoints = ConcurrentHashMap.newKeySet();

    /** 当前状态 */
    private final AtomicReference<State> state = new AtomicReference<>(State.RUNNING);

    /** 当前暂停在哪个节点 */
    private volatile String pausedAtNodeId;

    /** 暂停原因（用于前端展示） */
    private volatile String pauseReason;

    /** 节点执行快照：nodeId → 输出变量（用于变量查看器） */
    private final Map<String, Map<String, Object>> nodeSnapshots = new ConcurrentHashMap<>();

    /** 用于阻塞 resume/step 的信号 */
    private volatile CompletableFuture<Void> resumeSignal;

    /**
     * 暂停串行化信号量（公平）。
     *
     * <p>并行批次里多个节点可能同时命中断点。若不加串行化，第二个节点会因为
     * 状态已经是 PAUSED 而 CAS 失败并「立即通过」，断点被静默忽略；
     * 或者两个线程互相覆盖 resumeSignal，导致其中一个永远等不到唤醒。</p>
     */
    private final java.util.concurrent.Semaphore pauseGate = new java.util.concurrent.Semaphore(1, true);

    private DebugManager(Long executionId) {
        this.executionId = executionId;
    }

    // ---------------------------------------------------------------
    // 实例管理（按 executionId 缓存）
    // ---------------------------------------------------------------

    private static final Map<Long, DebugManager> INSTANCES = new ConcurrentHashMap<>();

    /** 为执行创建并缓存调试管理器 */
    public static DebugManager attach(Long executionId) {
        return INSTANCES.computeIfAbsent(executionId, DebugManager::new);
    }

    /** 获取已存在的调试管理器（不存在返回 null） */
    public static DebugManager get(Long executionId) {
        return INSTANCES.get(executionId);
    }

    /** 销毁调试管理器（执行结束后调用） */
    public static void detach(Long executionId) {
        INSTANCES.remove(executionId);
    }

    // ---------------------------------------------------------------
    // 断点 API
    // ---------------------------------------------------------------

    /** 设置断点 */
    public boolean addBreakpoint(String nodeId) {
        return breakpoints.add(nodeId);
    }

    /** 移除断点 */
    public boolean removeBreakpoint(String nodeId) {
        return breakpoints.remove(nodeId);
    }

    /** 清空所有断点 */
    public void clearBreakpoints() {
        breakpoints.clear();
    }

    /** 当前所有断点 */
    public Set<String> getBreakpoints() {
        return Set.copyOf(breakpoints);
    }

    /** 是否在指定节点设置了断点 */
    public boolean hasBreakpoint(String nodeId) {
        return breakpoints.contains(nodeId);
    }

    // ---------------------------------------------------------------
    // 执行控制（被 WorkflowEngine 调用）
    // ---------------------------------------------------------------

    /**
     * 判断是否应在执行该节点前暂停。
     * 返回 true 表示需要调用 pauseAndAwait() 阻塞。
     */
    public boolean shouldPauseBefore(String nodeId) {
        State s = state.get();
        if (s == State.STOPPED) {
            // 抛出异常让引擎终止
            return false;
        }
        if (hasBreakpoint(nodeId)) return true;
        if (s == State.STEPPING) return true;
        return false;
    }

    /**
     * 在断点处阻塞等待用户操作。
     *
     * <p>等待过程会周期性检查取消令牌：否则用户调用执行取消接口时，引擎线程
     * 正停在这里，永远检查不到令牌，导致该线程与 DebugManager 条目永久泄漏
     * （只有 /debug/stop 能解开）。</p>
     *
     * @param nodeId 当前暂停节点
     * @param reason 暂停原因 (BREAKPOINT / STEPPING)
     * @param token  取消令牌；为 null 时退化为「只等 resume/step/stop」
     */
    public void pauseAndAwait(String nodeId, String reason, CancellationToken token) {
        // 同一执行内只允许一个节点处于「暂停中」，其余排队等待。
        try {
            pauseGate.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            // 必须先把 resumeSignal 放好再做状态 CAS：否则 resume() 可能落在
            // 「CAS 已成功、signal 还没赋值」的窗口里 —— 它会把状态改回 RUNNING
            // 并返回 true，却没有任何 future 可完成，于是本线程拿着一个永远
            // 不会被唤醒的 future 死等。
            CompletableFuture<Void> mySignal = new CompletableFuture<>();
            this.resumeSignal = mySignal;

            if (!state.compareAndSet(State.RUNNING, State.PAUSED)
                    && !state.compareAndSet(State.STEPPING, State.PAUSED)) {
                // 已被 stop，或上一次暂停尚未结束：直接放行，不做无谓等待
                return;
            }
            this.pausedAtNodeId = nodeId;
            this.pauseReason = reason;
            log("Paused at node {} (reason={})", nodeId, reason);
            try {
                // 以 200ms 为片轮询，既能及时响应 resume/step/stop，
                // 也能及时响应执行取消。
                while (true) {
                    if (shouldAbort(token)) {
                        break;
                    }
                    try {
                        mySignal.get(200, TimeUnit.MILLISECONDS);
                        break;
                    } catch (TimeoutException te) {
                        // 继续轮询
                    }
                }
            } catch (InterruptedException e) {
                log("Resume interrupted: {}", e.getMessage());
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log("Resume failed: {}", e.getMessage());
            } finally {
                this.pausedAtNodeId = null;
                this.pauseReason = null;
            }
        } finally {
            pauseGate.release();
        }
    }

    /**
     * 暂停/等待期间是否应中止执行（用户点了停止，或执行已被取消）。
     */
    public boolean shouldAbort(CancellationToken token) {
        return isStopped() || (token != null && token.isCancelled());
    }

    /** 用户调用：恢复执行 */
    public boolean resume() {
        if (state.get() != State.PAUSED) return false;
        log("Resumed by user");
        state.set(State.RUNNING);
        CompletableFuture<Void> sig = resumeSignal;
        resumeSignal = null;
        if (sig != null) sig.complete(null);
        return true;
    }

    /** 用户调用：单步执行（下一节点完成后再次暂停） */
    public boolean step() {
        if (state.get() != State.PAUSED) return false;
        log("Step mode triggered");
        state.set(State.STEPPING);
        CompletableFuture<Void> sig = resumeSignal;
        resumeSignal = null;
        if (sig != null) sig.complete(null);
        return true;
    }

    /** 用户调用：停止执行（等同于 cancel） */
    public boolean stop() {
        State prev = state.getAndSet(State.STOPPED);
        CompletableFuture<Void> sig = resumeSignal;
        resumeSignal = null;
        if (sig != null) sig.complete(null);
        log("Stopped by user (prev state={})", prev);
        return prev != State.STOPPED;
    }

    /** 当前是否已停止 */
    public boolean isStopped() {
        return state.get() == State.STOPPED;
    }

    // ---------------------------------------------------------------
    // 节点快照（变量查看器）
    // ---------------------------------------------------------------

    /**
     * 记录节点执行输出（供前端变量查看器使用）。
     *
     * <p>注意：不能用 {@code Map.copyOf(output)} —— 它不允许 null 值，
     * 而节点输出里出现 null 字段非常常见（例如 BRANCH 的 matchedData、
     * HTTP/AI 节点的可选字段）。一旦踩到就会抛 NPE，并沿着
     * WorkflowEngine → executeNodeWithTimeout 把**整个工作流执行**打成 failed。
     * 这里改为过滤掉 null 值后存入可变 Map。</p>
     */
    public void recordSnapshot(String nodeId, Map<String, Object> output) {
        if (output == null || nodeId == null) return;
        Map<String, Object> sanitized = new java.util.HashMap<>();
        for (Map.Entry<String, Object> entry : output.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                sanitized.put(entry.getKey(), entry.getValue());
            }
        }
        nodeSnapshots.put(nodeId, sanitized);
    }

    /** 获取节点执行快照 */
    public Map<String, Object> getSnapshot(String nodeId) {
        return nodeSnapshots.get(nodeId);
    }

    /** 获取所有节点快照 */
    public Map<String, Map<String, Object>> getAllSnapshots() {
        return Map.copyOf(nodeSnapshots);
    }

    // ---------------------------------------------------------------
    // 状态查询
    // ---------------------------------------------------------------

    public State getState() {
        return state.get();
    }

    public String getPausedAtNodeId() {
        return pausedAtNodeId;
    }

    public String getPauseReason() {
        return pauseReason;
    }

    /** 调试快照（前端轮询用） */
    public Map<String, Object> snapshot() {
        return Map.of(
                "executionId", executionId,
                "state", state.get().name(),
                "pausedAtNodeId", pausedAtNodeId == null ? "" : pausedAtNodeId,
                "pauseReason", pauseReason == null ? "" : pauseReason,
                "breakpoints", getBreakpoints(),
                "nodeSnapshots", getAllSnapshots()
        );
    }

    // ---------------------------------------------------------------
    // 日志（最小化依赖，复用 slf4j 风格）
    // ---------------------------------------------------------------

    private void log(String fmt, Object... args) {
        // 避免引入 lombok @Slf4j，使用 java.util.logging 简化
        java.util.logging.Logger.getLogger("DebugManager")
                .info(String.format("[exec=%d] " + fmt, prepend(args, executionId)));
    }

    private static Object[] prepend(Object[] args, Object first) {
        Object[] out = new Object[args.length + 1];
        out[0] = first;
        System.arraycopy(args, 0, out, 1, args.length);
        return out;
    }
}
