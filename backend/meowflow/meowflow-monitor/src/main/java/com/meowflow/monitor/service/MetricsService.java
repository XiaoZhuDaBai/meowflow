package com.meowflow.monitor.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.monitor.dto.MetricDTO;
import com.meowflow.monitor.dto.MetricsQuery;
import com.meowflow.monitor.entity.MetricDaily;
import com.meowflow.monitor.repository.MetricRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MetricRepository metricRepository;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        registerSystemMetrics();
    }

    private void registerSystemMetrics() {
        Gauge.builder("jvm.memory.heap.used", ManagementFactory.getMemoryMXBean(), bean -> bean.getHeapMemoryUsage().getUsed() / 1024.0 / 1024.0)
                .tag("type", "heap")
                .register(meterRegistry);

        Gauge.builder("jvm.memory.heap.max", ManagementFactory.getMemoryMXBean(), bean -> bean.getHeapMemoryUsage().getMax() / 1024.0 / 1024.0)
                .tag("type", "heap")
                .register(meterRegistry);

        Gauge.builder("jvm.memory.nonheap.used", ManagementFactory.getMemoryMXBean(), bean -> bean.getNonHeapMemoryUsage().getUsed() / 1024.0 / 1024.0)
                .tag("type", "nonheap")
                .register(meterRegistry);

        Gauge.builder("jvm.threads.live", ManagementFactory.getThreadMXBean(), ThreadMXBean::getThreadCount)
                .register(meterRegistry);

        Gauge.builder("jvm.threads.peak", ManagementFactory.getThreadMXBean(), ThreadMXBean::getPeakThreadCount)
                .register(meterRegistry);

        log.info("System metrics registered");
    }

    public void recordCounter(String name, String... tags) {
        Counter counter = Counter.builder(name)
                .tags(tags)
                .register(meterRegistry);
        counter.increment();
    }

    public void recordCounter(String name, double amount, String... tags) {
        Counter counter = Counter.builder(name)
                .tags(tags)
                .register(meterRegistry);
        counter.increment(amount);
    }

    public void recordTimer(String name, long durationMs, String... tags) {
        Timer timer = Timer.builder(name)
                .tags(tags)
                .register(meterRegistry);
        timer.record(java.time.Duration.ofMillis(durationMs));
    }

    public void recordGauge(String name, double value, String... tags) {
        Gauge.builder(name, () -> value)
                .tags(tags)
                .register(meterRegistry);
    }

    public void recordWorkflowExecution(String workflowId, boolean success, long durationMs) {
        String status = success ? "success" : "failed";
        recordCounter("workflow.executions", "status", status);
        recordCounter("workflow.executions.total", "workflow_id", workflowId, "status", status);
        recordTimer("workflow.execution.duration", durationMs, "workflow_id", workflowId);
    }

    public void recordNodeExecution(String workflowId, String nodeId, String nodeType, boolean success, long durationMs) {
        String status = success ? "success" : "failed";
        recordCounter("node.executions", "type", nodeType, "status", status);
        recordTimer("node.execution.duration", durationMs, "type", nodeType);
    }

    public void recordApiCall(String endpoint, int statusCode, long durationMs) {
        String status = statusCode < 400 ? "success" : "error";
        recordCounter("api.calls", "endpoint", endpoint, "status", status);
        recordTimer("api.call.duration", durationMs, "endpoint", endpoint);
    }

    @Transactional
    public void saveMetric(String metricType, String metricName, String metricKey, Double value, String unit, String category, String tags) {
        MetricDaily metric = new MetricDaily();
        metric.setMetricDate(LocalDate.now());
        metric.setWfTotal(0L);
        metric.setExecTotal(0L);
        metric.setCreateTime(LocalDateTime.now());

        metricRepository.insert(metric);
    }

    public IPage<MetricDTO> queryMetrics(MetricsQuery query) {
        Page<MetricDaily> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<MetricDaily> result = metricRepository.findAll(page);
        return result.convert(this::convertToDTO);
    }

    public List<MetricDTO> getLatestMetrics(int limit) {
        List<MetricDaily> metrics = metricRepository.findLatestMetrics(limit);
        return metrics.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void aggregateDailyMetrics() {
        log.info("Starting daily metrics aggregation");
        log.info("Daily metrics aggregation completed");
    }

    private MetricDTO convertToDTO(MetricDaily metric) {
        return BeanUtil.copyProperties(metric, MetricDTO.class);
    }
}
