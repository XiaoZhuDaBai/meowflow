package com.meowflow.common.trace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 链路追踪日志实体
 * <p>
 * 记录每个 @TraceNode 方法的调用信息，包括：
 * <ul>
 *   <li>全局链路 ID (traceId)</li>
 *   <li>当前 Span ID (spanId)</li>
 *   <li>父 Span ID (parentSpanId)</li>
 *   <li>节点名称和分类</li>
 *   <li>输入输出数据</li>
 *   <li>执行状态和时间</li>
 * </ul>
 *
 * <p>存储方式：异步写入日志文件，可扩展为数据库存储
 *
 * @see TraceNode
 * @see TraceAspect
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 全局链路追踪 ID
     */
    private String traceId;

    /**
     * 当前 Span ID
     */
    private String spanId;

    /**
     * 父 Span ID
     */
    private String parentSpanId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点分类：ai / flow / tool / notify / trigger / default
     */
    private String category;

    /**
     * 开始时间 (毫秒时间戳)
     */
    private long startTime;

    /**
     * 结束时间 (毫秒时间戳)
     */
    private long endTime;

    /**
     * 执行时长 (毫秒)
     */
    private long duration;

    /**
     * 执行状态：SUCCESS / FAILED
     */
    private String status;

    /**
     * 输入数据 (JSON 字符串，已脱敏)
     */
    private String inputData;

    /**
     * 输出数据 (JSON 字符串，已脱敏)
     */
    private String outputData;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 错误堆栈
     */
    private String errorStack;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 请求路径
     */
    private String requestUri;

    /**
     * HTTP 方法
     */
    private String httpMethod;

    /**
     * 客户端 IP
     */
    private String clientIp;

    /**
     * 服务端 IP
     */
    private String serverIp;

    /**
     * 创建 TraceLog 的便捷方法
     */
    public static TraceLogBuilder builder() {
        return new TraceLogBuilder().startTime(System.currentTimeMillis());
    }

    /**
     * 标记为成功
     */
    public TraceLog markSuccess() {
        this.status = "SUCCESS";
        this.endTime = System.currentTimeMillis();
        this.duration = this.endTime - this.startTime;
        return this;
    }

    /**
     * 标记为失败
     */
    public TraceLog markFailed(String errorMsg, String errorStack) {
        this.status = "FAILED";
        this.errorMsg = errorMsg;
        this.errorStack = errorStack;
        this.endTime = System.currentTimeMillis();
        this.duration = this.endTime - this.startTime;
        return this;
    }

    /**
     * 转为日志格式字符串
     */
    public String toLogString() {
        return String.format(
                "[TRACE] traceId=%s spanId=%s parentSpan=%s node=%s category=%s status=%s duration=%dms user=%s",
                traceId, spanId, parentSpanId, nodeName, category, status, duration, userId
        );
    }

    /**
     * 转为 JSON 格式字符串 (用于持久化)
     */
    public String toJson() {
        return String.format(
                "{\"traceId\":\"%s\",\"spanId\":\"%s\",\"parentSpanId\":\"%s\",\"nodeName\":\"%s\",\"category\":\"%s\",\"status\":\"%s\",\"duration\":%d,\"startTime\":%d,\"endTime\":%d,\"userId\":\"%s\",\"requestUri\":\"%s\",\"inputData\":%s,\"outputData\":%s,\"errorMsg\":\"%s\"}",
                nullToEmpty(traceId),
                nullToEmpty(spanId),
                nullToEmpty(parentSpanId),
                nullToEmpty(nodeName),
                nullToEmpty(category),
                nullToEmpty(status),
                duration,
                startTime,
                endTime,
                nullToEmpty(userId == null ? null : String.valueOf(userId)),
                nullToEmpty(requestUri),
                nullToEmpty(inputData),
                nullToEmpty(outputData),
                nullToEmpty(errorMsg)
        );
    }

    private String nullToEmpty(String str) {
        return str == null ? "" : str.replace("\"", "\\\"");
    }
}
