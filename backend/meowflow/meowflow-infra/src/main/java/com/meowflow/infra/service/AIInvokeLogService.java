package com.meowflow.infra.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 调用日志服务
 */
@Slf4j
@Service
public class AIInvokeLogService extends ServiceImpl<AIInvokeLogMapper, AIInvokeLog> {

    private final AICostCalculator costCalculator;

    public AIInvokeLogService(AICostCalculator costCalculator) {
        this.costCalculator = costCalculator;
    }

    /**
     * 记录 AI 调用
     */
    public void logInvoke(AIInvokeLog invokeLog) {
        if (invokeLog == null) {
            return;
        }
        // 自动计算成本
        costCalculator.calculateAndFill(invokeLog);
        // 异步保存，避免阻塞主流程
        saveAsync(invokeLog);
    }

    /**
     * 异步保存日志
     */
    @Async("logExecutor")
    public void saveAsync(AIInvokeLog invokeLog) {
        try {
            this.save(invokeLog);
            log.debug("AI invoke log saved: model={}, cost={}", invokeLog.getModel(), invokeLog.getCostAmount());
        } catch (Exception e) {
            log.error("Failed to save AI invoke log: {}", invokeLog, e);
        }
    }

    /**
     * 按 executionId 查询
     */
    public List<AIInvokeLog> findByExecutionId(Long executionId) {
        return this.list(
                new LambdaQueryWrapper<AIInvokeLog>()
                        .eq(AIInvokeLog::getExecutionId, executionId)
                        .orderByAsc(AIInvokeLog::getCreateTime)
        );
    }

    /**
     * 按 workflowId 查询
     */
    public List<AIInvokeLog> findByWorkflowId(String workflowId) {
        return this.list(
                new LambdaQueryWrapper<AIInvokeLog>()
                        .eq(AIInvokeLog::getWorkflowId, workflowId)
                        .orderByAsc(AIInvokeLog::getCreateTime)
        );
    }

    /**
     * 按用户查询 (分页)
     */
    public IPage<AIInvokeLog> findByUserId(Long userId, PageRequest pageRequest) {
        return this.page(
                new Page<>(pageRequest.getPage(), pageRequest.getSize()),
                new LambdaQueryWrapper<AIInvokeLog>()
                        .eq(AIInvokeLog::getUserId, userId)
                        .orderByDesc(AIInvokeLog::getCreateTime)
        );
    }

    /**
     * 按用户和时间范围查询 (分页)
     */
    public IPage<AIInvokeLog> findByUserIdAndTimeRange(Long userId, LocalDateTime from,
                                                        LocalDateTime to, PageRequest pageRequest) {
        return this.page(
                new Page<>(pageRequest.getPage(), pageRequest.getSize()),
                new LambdaQueryWrapper<AIInvokeLog>()
                        .eq(AIInvokeLog::getUserId, userId)
                        .ge(from != null, AIInvokeLog::getCreateTime, from)
                        .le(to != null, AIInvokeLog::getCreateTime, to)
                        .orderByDesc(AIInvokeLog::getCreateTime)
        );
    }

    /**
     * 成本统计
     */
    public CostStatistics getCostStatistics(Long userId, LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<AIInvokeLog> query = new LambdaQueryWrapper<AIInvokeLog>()
                .eq(userId != null, AIInvokeLog::getUserId, userId)
                .ge(from != null, AIInvokeLog::getCreateTime, from)
                .le(to != null, AIInvokeLog::getCreateTime, to)
                .eq(AIInvokeLog::getStatus, 1); // 只统计成功的调用

        List<AIInvokeLog> logs = this.list(query);

        if (logs == null || logs.isEmpty()) {
            return new CostStatistics(0, BigDecimal.ZERO, 0L, 0L, 0);
        }

        BigDecimal totalCost = BigDecimal.ZERO;
        long totalPromptTokens = 0;
        long totalCompletionTokens = 0;
        int successCount = 0;
        int failCount = 0;

        for (AIInvokeLog log : logs) {
            if (log.getCostAmount() != null) {
                totalCost = totalCost.add(BigDecimal.valueOf(log.getCostAmount()));
            }
            if (log.getPromptTokens() != null) {
                totalPromptTokens += log.getPromptTokens();
            }
            if (log.getCompletionTokens() != null) {
                totalCompletionTokens += log.getCompletionTokens();
            }
            if (log.getStatus() != null && "success".equals(log.getStatus())) {
                successCount++;
            } else {
                failCount++;
            }
        }

        return new CostStatistics(
                logs.size(),
                totalCost.setScale(4, RoundingMode.HALF_UP),
                totalPromptTokens,
                totalCompletionTokens,
                successCount
        );
    }

    /**
     * 按模型分组统计成本
     */
    public List<ModelCostSummary> getCostByModel(Long userId, LocalDateTime from, LocalDateTime to) {
        List<AIInvokeLogMapper.ModelCostRow> rows = baseMapper.selectCostByModel(userId, from, to);
        return rows.stream()
                .map(row -> new ModelCostSummary(
                        row.getModel(),
                        row.getProvider(),
                        row.getInvokeCount(),
                        row.getTotalCost(),
                        row.getTotalTokens()
                ))
                .toList();
    }

    /**
     * 成本统计结果
     */
    @Data
    @AllArgsConstructor
    public static class CostStatistics {
        /** 总调用次数 */
        private int totalInvocations;
        /** 总费用 (USD) */
        private BigDecimal totalCost;
        /** 总输入 tokens */
        private long totalPromptTokens;
        /** 总输出 tokens */
        private long totalCompletionTokens;
        /** 成功次数 */
        private int successCount;
    }

    /**
     * 模型费用汇总
     */
    @Data
    @AllArgsConstructor
    public static class ModelCostSummary {
        private String model;
        private String provider;
        private int invokeCount;
        private BigDecimal totalCost;
        private long totalTokens;
    }

    /**
     * 分页请求
     */
    @Data
    public static class PageRequest {
        private int page = 1;
        private int size = 20;

        public PageRequest() {
        }

        public PageRequest(int page, int size) {
            this.page = page;
            this.size = size;
        }
    }
}

