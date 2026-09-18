package com.meowflow.workflow.event;

import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 执行事件下沉器接口。
 *
 * <p>负责将工作流执行过程中产生的 {@link RunEvent} 写入持久化存储，
 * 支持 SSE 实时推送、崩溃恢复快照等场景。</p>
 *
 * <p>实现类通过 {@code @ConditionalOnProperty} 控制启用状态。</p>
 *
 * <h3>线程安全</h3>
 * 所有实现必须是线程安全的。
 */
public interface RunEventSink {

    /**
     * 追加一条执行事件。
     *
     * @param event 事件
     */
    void append(RunEvent event);

    /**
     * 追加多条执行事件（批量优化）。
     *
     * @param events 事件列表
     */
    default void appendAll(List<RunEvent> events) {
        for (RunEvent event : events) {
            append(event);
        }
    }

    /**
     * 更新执行级别状态。
     *
     * @param executionId 执行 ID
     * @param status      新状态
     * @param output      工作流输出（可为 null）
     */
    void updateExecutionStatus(Long executionId, String status, Map<String, Object> output);

    /**
     * 创建执行快照。
     *
     * @param context 执行上下文
     * @param waitingReason 等待原因（可为 null）
     * @param lastEventId  最新事件 ID（可为 null）
     * @return 快照版本号
     */
    int createSnapshot(ExecutionContext context, String waitingReason, String lastEventId);

    /**
     * 获取指定 executionId 的最新快照。
     *
     * @param executionId 执行 ID
     * @return 快照（若不存在返回空）
     */
    Optional<com.meowflow.workflow.entity.ExecutionSnapshot> getLatestSnapshot(Long executionId);

    /**
     * 获取指定 executionId 自 cursor 以来的所有事件。
     *
     * @param executionId 执行 ID
     * @param cursor      游标（格式同 Redis Stream cursor）；传入 "0-0" 表示从头开始
     * @return 事件列表 + 新游标
     */
    EventPage fetchEvents(Long executionId, String cursor);

    /**
     * 刷新并确保所有待写入数据已持久化。
     *
     * <p>对于 Redis Stream 实现，这通常对应 {@code XREAD BLOCK} 可见性延迟。</p>
     */
    default void flush() {
    }

    /**
     * 事件分页结果。
     */
    record EventPage(List<RunEvent> events, String nextCursor) {}
}
