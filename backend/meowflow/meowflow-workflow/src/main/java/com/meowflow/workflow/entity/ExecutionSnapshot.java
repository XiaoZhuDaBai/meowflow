package com.meowflow.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 执行快照：记录工作流运行到某一时刻的完整业务状态。
 *
 * <p>用于崩溃恢复与断点续跑。
 * 不含实时资源（如 LLM 连接池、线程池状态），仅含可序列化的业务数据。</p>
 *
 * <p>存储策略：每个节点完成后更新快照（最终一致性）；异步写入以避免影响执行吞吐。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("mf_wf_execution_snapshot")
public class ExecutionSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long executionId;

    private Long workflowId;

    private String version;

    private String status;

    /**
     * 已完成节点列表：JSON 数组。
     * 每个元素为 { "nodeId": "...", "status": "SUCCESS", "output": {...} }
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String completedNodes;

    /**
     * 当前变量上下文：JSON 对象。
     * 对应 {@link com.meowflow.workflow.engine.ExecutionContext#getVariables()}
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String variables;

    /**
     * LOOP 游标记录：JSON 对象。
     * key = loopNodeId, value = currentIteration
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String loopCursors;

    /**
     * 等待原因（可选）。
     * 如 "waiting_for_join" / "waiting_for_fork" / "loop_iterating"
     */
    private String waitingReason;

    /**
     * 等待中的边 ID（可选）。
     */
    private String pendingEdgeId;

    /**
     * 已消费事件的最新 ID（用于 SSE cursor）。
     */
    private String lastEventId;

    /**
     * 快照版本号，每次更新自增。
     * 用于乐观锁检测并发写入。
     */
    private Integer snapshotVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
