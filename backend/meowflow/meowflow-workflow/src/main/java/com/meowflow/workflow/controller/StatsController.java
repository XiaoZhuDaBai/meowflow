package com.meowflow.workflow.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meowflow.common.result.Result;
import com.meowflow.workflow.entity.Workflow;
import com.meowflow.workflow.repository.ExecutionRepository;
import com.meowflow.workflow.repository.WorkflowRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计看板 API。
 * 提供工作流执行统计、趋势和成本聚合数据。
 * <p>
 * 前端路由前缀通过网关转发：/workflow/api/stats/* → 这里。
 */
@Slf4j
@RestController
@RequestMapping("/api/stats")
@Tag(name = "统计看板", description = "工作流执行统计聚合接口")
@RequiredArgsConstructor
public class StatsController {

    private final ExecutionRepository executionRepository;
    private final WorkflowRepository workflowRepository;

    // ==================== Overview ====================

    /**
     * GET /api/stats/overview
     * 返回今日统计 + 近7日趋势 + Top工作流。
     */
    @GetMapping("/overview")
    @Operation(summary = "统计概览", description = "获取今日统计、近7日趋势和执行次数Top工作流")
    public Result<OverviewResponse> overview() {
        LocalDateTime today = LocalDate.now().atStartOfDay();

        // 今日汇总
        long todayTotal = executionRepository.countTodayTotal(today);
        long todaySuccess = executionRepository.countTodaySuccess(today);
        Double avgMs = executionRepository.avgCostMsToday(today);
        Double totalCost = executionRepository.totalCostAmountToday(today);

        double successRate = todayTotal > 0
                ? Math.round(todaySuccess * 10000.0 / todayTotal) / 100.0
                : 0.0;

        TodayStats todayStats = new TodayStats();
        todayStats.setExecutions(todayTotal);
        todayStats.setSuccessRate(successRate);
        todayStats.setAvgDuration(avgMs != null ? avgMs.longValue() : 0L);
        todayStats.setCost(totalCost != null ? BigDecimal.valueOf(totalCost).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0);

        // 近7日趋势
        LocalDateTime sevenDaysAgo = today.minusDays(6);
        List<Map<String, Object>> rawDaily = executionRepository.dailyStats(sevenDaysAgo, LocalDateTime.now());
        List<TrendPoint> trend = buildTrend(rawDaily, sevenDaysAgo);

        // Top 工作流
        List<Map<String, Object>> topRaw = executionRepository.topWorkflows(5);
        List<Workflow> workflows = loadWorkflows(topRaw);
        List<TopWorkflow> topWorkflows = topRaw.stream()
                .map(wc -> {
                    TopWorkflow tw = new TopWorkflow();
                    tw.setId(String.valueOf(wc.get("workflow_id")));
                    tw.setName(workflows.stream()
                            .filter(w -> w.getId().equals(toLong(wc.get("workflow_id"))))
                            .findFirst()
                            .map(Workflow::getName)
                            .orElse("工作流-" + wc.get("workflow_id")));
                    tw.setExecutions(toLong(wc.get("exec_cnt")));
                    return tw;
                })
                .collect(Collectors.toList());

        OverviewResponse resp = new OverviewResponse();
        resp.setToday(todayStats);
        resp.setTrend(trend);
        resp.setTopWorkflows(topWorkflows);
        return Result.success(resp);
    }

    // ==================== Trend ====================

    /**
     * GET /api/stats/trend
     * 返回近7日执行趋势。
     */
    @GetMapping("/trend")
    @Operation(summary = "执行趋势", description = "获取近7日的执行次数、成功率和成本趋势")
    public Result<List<TrendPoint>> trend() {
        LocalDateTime sevenDaysAgo = LocalDate.now().atStartOfDay().minusDays(6);
        List<Map<String, Object>> rawDaily = executionRepository.dailyStats(sevenDaysAgo, LocalDateTime.now());
        return Result.success(buildTrend(rawDaily, sevenDaysAgo));
    }

    // ==================== Inflight ====================

    /**
     * GET /api/stats/inflight
     * 返回当前运行中的执行数量。
     */
    @GetMapping("/inflight")
    @Operation(summary = "在途执行数", description = "获取当前处于 running/pending 状态的执行数量")
    public Result<Long> inflight() {
        LambdaQueryWrapper<com.meowflow.workflow.entity.Execution> wrapper =
                new LambdaQueryWrapper<>();
        wrapper.in(com.meowflow.workflow.entity.Execution::getStatus, "running", "pending");
        long count = executionRepository.selectCount(wrapper);
        return Result.success(count);
    }

    // ==================== Cost ====================

    /**
     * GET /api/stats/cost
     * 返回成本统计：总成本、各工作流分摊。
     */
    @GetMapping("/cost")
    @Operation(summary = "成本统计", description = "获取总成本、工作流分摊和近7日成本趋势")
    public Result<CostResponse> cost() {
        LocalDateTime sevenDaysAgo = LocalDate.now().atStartOfDay().minusDays(6);

        // 总成本
        Double total = executionRepository.totalCostAmount();

        // 各工作流分摊
        List<Map<String, Object>> costByWfRaw = executionRepository.costByWorkflow();
        List<Long> wfIds = costByWfRaw.stream()
                .map(m -> toLong(m.get("workflow_id")))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<Workflow> workflows = loadWorkflowsByIds(wfIds);
        double finalTotal = total != null ? total : 0.0;
        List<CostByWorkflow> byWorkflow = costByWfRaw.stream()
                .map(wc -> {
                    CostByWorkflow item = new CostByWorkflow();
                    Long wfId = toLong(wc.get("workflow_id"));
                    item.setWorkflowId(String.valueOf(wfId));
                    item.setWorkflowName(workflows.stream()
                            .filter(w -> w.getId().equals(wfId))
                            .findFirst()
                            .map(Workflow::getName)
                            .orElse("工作流-" + wfId));
                    double cost = toDouble(wc.get("total"));
                    item.setCost(BigDecimal.valueOf(cost).setScale(2, RoundingMode.HALF_UP).doubleValue());
                    item.setPercentage(finalTotal > 0
                            ? Math.round(cost * 10000.0 / finalTotal) / 100.0
                            : 0.0);
                    return item;
                })
                .collect(Collectors.toList());

        // 近7日成本趋势
        List<Map<String, Object>> rawDaily = executionRepository.dailyStats(sevenDaysAgo, LocalDateTime.now());
        List<CostTrendPoint> costTrend = rawDaily.stream()
                .map(ds -> {
                    CostTrendPoint p = new CostTrendPoint();
                    Object dateObj = ds.get("exec_date");
                    if (dateObj instanceof java.sql.Date) {
                        p.setDate(((java.sql.Date) dateObj).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
                    } else if (dateObj instanceof java.time.LocalDate) {
                        p.setDate(((java.time.LocalDate) dateObj).format(DateTimeFormatter.ISO_LOCAL_DATE));
                    } else {
                        p.setDate(String.valueOf(dateObj));
                    }
                    long successCnt = toLong(ds.get("success_cnt"));
                    double estimatedCost = successCnt * 0.05; // 估算：实际应以 costAmount 为准
                    p.setCost(BigDecimal.valueOf(estimatedCost).setScale(2, RoundingMode.HALF_UP).doubleValue());
                    return p;
                })
                .collect(Collectors.toList());

        CostResponse resp = new CostResponse();
        resp.setTotal(BigDecimal.valueOf(finalTotal).setScale(2, RoundingMode.HALF_UP).doubleValue());
        resp.setByWorkflow(byWorkflow);
        resp.setByModel(Collections.emptyList()); // 模型成本维度需 node_execution 表成本明细
        resp.setTrend(costTrend);
        return Result.success(resp);
    }

    // ==================== Helpers ====================

    private List<TrendPoint> buildTrend(List<Map<String, Object>> rawDaily, LocalDateTime from) {
        // 构建从 from 开始的 7 天（含）序列，缺的天数补 0
        Map<String, Map<String, Object>> map = rawDaily.stream()
                .collect(Collectors.toMap(
                        m -> {
                            Object d = m.get("exec_date");
                            if (d instanceof java.sql.Date) return ((java.sql.Date) d).toLocalDate().toString();
                            if (d instanceof java.time.LocalDate) return ((java.time.LocalDate) d).toString();
                            return String.valueOf(d);
                        },
                        m -> m,
                        (a, b) -> a
                ));

        List<TrendPoint> trend = new ArrayList<>();
        LocalDate cursor = from.toLocalDate();
        LocalDate end = LocalDate.now();
        while (!cursor.isAfter(end)) {
            TrendPoint p = new TrendPoint();
            p.setDate(cursor.format(DateTimeFormatter.ISO_LOCAL_DATE));
            Map<String, Object> ds = map.get(cursor.toString());
            if (ds != null) {
                long total = toLong(ds.get("total"));
                long success = toLong(ds.get("success_cnt"));
                p.setExecutions(total);
                p.setSuccessRate(total > 0
                        ? Math.round(success * 10000.0 / total) / 100.0
                        : 0.0);
                p.setCost(0.0);
            } else {
                p.setExecutions(0L);
                p.setSuccessRate(0.0);
                p.setCost(0.0);
            }
            trend.add(p);
            cursor = cursor.plusDays(1);
        }
        return trend;
    }

    private List<Workflow> loadWorkflows(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) return Collections.emptyList();
        List<Long> ids = items.stream()
                .map(m -> toLong(m.get("workflow_id")))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return loadWorkflowsByIds(ids);
    }

    private List<Workflow> loadWorkflowsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        List<Workflow> result = new ArrayList<>();
        for (Long id : ids) {
            Workflow w = workflowRepository.selectById(id);
            if (w != null) result.add(w);
        }
        return result;
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(String.valueOf(val)); } catch (Exception e) { return null; }
    }

    private Double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Double) return (Double) val;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(String.valueOf(val)); } catch (Exception e) { return 0.0; }
    }

    // ==================== DTOs ====================

    @Data
    public static class OverviewResponse {
        private TodayStats today;
        private List<TrendPoint> trend;
        private List<TopWorkflow> topWorkflows;
    }

    @Data
    public static class TodayStats {
        private long executions;
        private double successRate;
        private long avgDuration;    // ms
        private double cost;
    }

    @Data
    public static class TrendPoint {
        private String date;         // "2026-07-16"
        private long executions;
        private double successRate;
        private double cost;
    }

    @Data
    public static class TopWorkflow {
        private String id;
        private String name;
        private long executions;
    }

    @Data
    public static class CostResponse {
        private double total;
        private List<CostByWorkflow> byWorkflow;
        private List<CostByModel> byModel;
        private List<CostTrendPoint> trend;
    }

    @Data
    public static class CostByWorkflow {
        private String workflowId;
        private String workflowName;
        private double cost;
        private double percentage;
    }

    @Data
    public static class CostByModel {
        private String model;
        private long calls;
        private double cost;
    }

    @Data
    public static class CostTrendPoint {
        private String date;
        private double cost;
    }
}
