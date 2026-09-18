package com.meowflow.monitor.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.common.context.UserContext;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.common.util.JsonUtils;
import com.meowflow.monitor.dto.AlertRecordDTO;
import com.meowflow.monitor.dto.AlertRuleDTO;
import com.meowflow.monitor.dto.AlertSilenceCreateDTO;
import com.meowflow.monitor.entity.AlertRecord;
import com.meowflow.monitor.entity.AlertRule;
import com.meowflow.monitor.entity.AlertSilence;
import com.meowflow.monitor.notifier.AlertNotifier;
import com.meowflow.monitor.repository.AlertRecordRepository;
import com.meowflow.monitor.repository.AlertRuleRepository;
import com.meowflow.monitor.repository.AlertSilenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class AlertService {

    private final AlertRuleRepository ruleRepository;
    private final AlertRecordRepository recordRepository;
    private final AlertSilenceRepository silenceRepository;
    private Map<String, AlertNotifier> notifiers;

    @Autowired
    public AlertService(AlertRuleRepository ruleRepository,
                        AlertRecordRepository recordRepository,
                        AlertSilenceRepository silenceRepository) {
        this.ruleRepository = ruleRepository;
        this.recordRepository = recordRepository;
        this.silenceRepository = silenceRepository;
    }

    @Autowired
    public void setNotifiers(List<AlertNotifier> notifierList) {
        this.notifiers = new HashMap<>();
        for (AlertNotifier notifier : notifierList) {
            this.notifiers.put(notifier.getChannel(), notifier);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public AlertRule createRule(AlertRuleDTO dto) {
        AlertRule rule = new AlertRule();
        rule.setName(dto.getName());
        rule.setCode(generateRuleCode(dto.getName()));
        rule.setDescription(dto.getDescription());
        rule.setAlertType("METRIC"); // 默认为指标告警
        rule.setMetricName(dto.getMetricName());
        rule.setConditionType(convertOperator(dto.getOperator()));
        rule.setThresholdValue(dto.getThreshold() != null ? BigDecimal.valueOf(dto.getThreshold()) : null);
        rule.setTimeWindowSeconds(dto.getDuration() != null ? dto.getDuration() : 300);
        rule.setEvaluationInterval(60);
        rule.setSeverity("WARNING");
        rule.setNotificationChannels(toChannelsJson(dto.getAlertChannel()));
        rule.setWebhook(dto.getWebhook());
        rule.setEnabled(true);
        rule.setCooldownSeconds(300);
        rule.setAutoResolve(0);
        rule.setOwnerId(getCurrentUserIdLong());
        rule.setCreateBy(getCurrentUserIdLong());
        rule.setCreateTime(LocalDateTime.now());

        ruleRepository.insert(rule);
        log.info("Created alert rule: {} by user: {}", rule.getId(), getCurrentUserId());

        return rule;
    }

    @Transactional(rollbackFor = Exception.class)
    public AlertRule updateRule(Long id, AlertRuleDTO dto) {
        AlertRule rule = ruleRepository.findById(id);
        if (rule == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "告警规则不存在");
        }

        if (dto.getName() != null) rule.setName(dto.getName());
        if (dto.getDescription() != null) rule.setDescription(dto.getDescription());
        if (dto.getMetricName() != null) rule.setMetricName(dto.getMetricName());
        if (dto.getOperator() != null) rule.setConditionType(convertOperator(dto.getOperator()));
        if (dto.getThreshold() != null) rule.setThresholdValue(BigDecimal.valueOf(dto.getThreshold()));
        if (dto.getDuration() != null) rule.setTimeWindowSeconds(dto.getDuration());
        if (dto.getAlertChannel() != null) rule.setNotificationChannels(toChannelsJson(dto.getAlertChannel()));
        if (dto.getWebhook() != null) rule.setWebhook(dto.getWebhook());
        if (dto.getStatus() != null) rule.setEnabled("active".equals(dto.getStatus()));

        rule.setUpdateBy(getCurrentUserIdLong());
        rule.setUpdateTime(LocalDateTime.now());
        ruleRepository.updateById(rule);
        log.info("Updated alert rule: {}", id);

        return rule;
    }

    public void deleteRule(Long id) {
        AlertRule rule = ruleRepository.findById(id);
        if (rule == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "告警规则不存在");
        }

        ruleRepository.deleteById(id);
        log.info("Deleted alert rule: {}", id);
    }

    public AlertRule getRule(Long id) {
        AlertRule rule = ruleRepository.findById(id);
        if (rule == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "告警规则不存在");
        }
        return rule;
    }

    public IPage<AlertRule> getAllRules(Integer pageNum, Integer pageSize) {
        Page<AlertRule> page = new Page<>(pageNum, pageSize);
        return ruleRepository.findAll(page);
    }

    @Transactional(rollbackFor = Exception.class)
    public void evaluateRule(AlertRule rule, Double currentValue) {
        if (rule.getEnabled() == null || !Boolean.TRUE.equals(rule.getEnabled())) {
            return;
        }

        List<AlertSilence> activeSilences = silenceRepository.findActiveByRuleId(rule.getId());
        if (!activeSilences.isEmpty()) {
            log.debug("Alert rule {} is silenced, skipping evaluation", rule.getId());
            return;
        }

        boolean isAlert = evaluateCondition(rule, currentValue);

        if (isAlert) {
            createAlert(rule, currentValue);
        }
    }

    private boolean evaluateCondition(AlertRule rule, Double currentValue) {
        if (currentValue == null || rule.getConditionType() == null || rule.getThresholdValue() == null) {
            return false;
        }

        String operator = rule.getConditionType();
        Double threshold = rule.getThresholdValue().doubleValue();

        return switch (operator) {
            case "gt" -> currentValue > threshold;
            case "lt" -> currentValue < threshold;
            case "eq" -> currentValue.equals(threshold);
            case "gte" -> currentValue >= threshold;
            case "lte" -> currentValue <= threshold;
            default -> false;
        };
    }

    private String convertOperator(String operator) {
        if (operator == null) return null;
        return switch (operator) {
            case ">" -> "gt";
            case "<" -> "lt";
            case ">=" -> "gte";
            case "<=" -> "lte";
            case "=" -> "eq";
            default -> operator;
        };
    }

    @Transactional(rollbackFor = Exception.class)
    public void createAlert(AlertRule rule, Double currentValue) {
        List<AlertRecord> existingAlerts = recordRepository.findByRuleId(rule.getId());
        boolean hasFiring = existingAlerts.stream()
                .anyMatch(a -> "FIRING".equals(a.getStatus()));
        if (hasFiring) {
            return;
        }

        AlertRecord alert = new AlertRecord();
        alert.setAlertRuleId(rule.getId());
        alert.setAlertCode(rule.getCode());
        alert.setAlertType(rule.getAlertType());
        alert.setTitle("告警: " + rule.getName());
        alert.setMessage(String.format("指标 %s 当前值 %.2f 超过阈值 %s", 
            rule.getMetricName(), currentValue, rule.getThresholdValue()));
        alert.setSeverity(rule.getSeverity());
        alert.setStatus("FIRING");
        alert.setMetric(rule.getMetricName());
        alert.setValue(BigDecimal.valueOf(currentValue));
        alert.setTriggerValue(String.valueOf(currentValue));
        alert.setThresholdValue(rule.getThresholdValue());
        alert.setFiredAt(LocalDateTime.now());
        alert.setNotificationSent(0);
        alert.setCreateTime(LocalDateTime.now());

        recordRepository.insert(alert);
        log.warn("Alert created: rule={}, value={}, threshold={}", rule.getName(), currentValue, rule.getThresholdValue());

        notifyAlert(alert);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resolveAlert(Long alertId, String comment) {
        AlertRecord alert = recordRepository.findById(alertId);
        if (alert == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "告警记录不存在");
        }

        alert.setStatus("RESOLVED");
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolvedBy(getCurrentUserId());
        alert.setResolutionNote(comment);
        alert.setUpdateTime(LocalDateTime.now());

        recordRepository.updateById(alert);
        log.info("Alert resolved: alertId={}, comment={}", alertId, comment);
    }

    public IPage<AlertRecordDTO> getAlerts(String status, Integer pageNum, Integer pageSize) {
        Page<AlertRecord> page = new Page<>(pageNum, pageSize);
        IPage<AlertRecord> result = status != null
                ? recordRepository.findByStatus(page, status)
                : recordRepository.findAll(page);
        return result.convert(this::toDto);
    }

    public List<AlertRecordDTO> getFiringAlerts() {
        return recordRepository.findFiringAlerts().stream().map(this::toDto).toList();
    }

    public int getFiringAlertCount() {
        return recordRepository.countFiringAlerts();
    }

    @Transactional(rollbackFor = Exception.class)
    public AlertSilence createSilence(AlertSilenceCreateDTO dto) {
        AlertSilence silence = new AlertSilence();
        silence.setName(dto.getReason());
        silence.setMatchType("RULE");
        silence.setAlertRuleId(dto.getRuleId() != null ? Long.parseLong(dto.getRuleId()) : null);
        silence.setStartTime(dto.getStartTime());
        silence.setEndTime(dto.getEndTime());
        silence.setReason(dto.getReason());
        silence.setCreateBy(getCurrentUserId());
        silence.setCreateTime(LocalDateTime.now());

        silenceRepository.insert(silence);
        log.info("Created silence: ruleId={}, start={}, end={}", dto.getRuleId(), dto.getStartTime(), dto.getEndTime());

        return silence;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSilence(Long id) {
        AlertSilence silence = silenceRepository.findById(id);
        if (silence == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "沉默记录不存在");
        }

        silenceRepository.deleteById(id);
        log.info("Deleted silence: {}", id);
    }

    public IPage<AlertSilence> getSilences(String status, Integer pageNum, Integer pageSize) {
        Page<AlertSilence> page = new Page<>(pageNum, pageSize);
        return silenceRepository.findAll(page);
    }

    private void notifyAlert(AlertRecord alert) {
        if (alert.getAlertRuleId() == null) {
            return;
        }

        AlertRule rule = ruleRepository.findById(alert.getAlertRuleId());
        if (rule == null || StrUtil.isBlank(rule.getNotificationChannels())) {
            return;
        }

        List<Map<String, Object>> logs = parseNotificationLog(alert.getNotifyLog());
        java.util.Set<String> sentChannels = logs.stream()
                .filter(item -> "SUCCESS".equals(item.get("status")))
                .map(item -> String.valueOf(item.get("channel")))
                .collect(java.util.stream.Collectors.toSet());
        boolean changed = false;
        for (String channel : parseChannels(rule.getNotificationChannels())) {
            String normalized = channel.trim();
            if (normalized.isEmpty() || sentChannels.contains(normalized)) {
                continue;
            }

            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("channel", normalized);
            item.put("time", LocalDateTime.now().toString());
            AlertNotifier notifier = notifiers.get(normalized);
            if (notifier == null) {
                item.put("status", "SKIPPED");
                item.put("message", "渠道未配置或未实现");
            } else {
                try {
                    notifier.send(alert);
                    item.put("status", "SUCCESS");
                    item.put("message", "发送成功");
                } catch (Exception e) {
                    item.put("status", "FAILED");
                    item.put("message", e.getMessage());
                    log.error("Failed to send alert notification: channel={}", channel, e);
                }
            }
            logs.add(item);
            changed = true;
        }

        if (changed) {
            alert.setNotifyLog(JsonUtils.toJson(logs));
            recordRepository.updateById(alert);
        }
    }

    private AlertRecordDTO toDto(AlertRecord alert) {
        List<String> channels = parseChannels(alert.getAlertChannel());
        List<Map<String, Object>> logs = parseNotificationLog(alert.getNotifyLog());
        Map<String, Object> latest = logs.isEmpty() ? Map.of() : logs.get(logs.size() - 1);
        String notifyStatus = notificationStatus(logs);
        LocalDateTime notifyTime = parseDateTime(latest.get("time"));

        return AlertRecordDTO.builder()
                .id(alert.getId())
                .ruleId(alert.getAlertRuleId())
                .ruleName(alert.getRuleName() != null ? alert.getRuleName() : alert.getTitle())
                .metricType(alert.getAlertType())
                .metricName(alert.getMetric())
                .currentValue(alert.getValue() != null ? alert.getValue().doubleValue() : null)
                .threshold(alert.getThresholdValue() != null ? alert.getThresholdValue().doubleValue() : null)
                .operator(alert.getOperator())
                .severity(alert.getSeverity())
                .status(upper(alert.getStatus()))
                .alertChannel(channels.isEmpty() ? null : String.join(",", channels))
                .alertMessage(alert.getMessage())
                .notifyStatus(notifyStatus)
                .notifyCount(logs.size())
                .notifyTime(notifyTime)
                .firedAt(alert.getFiredAt())
                .resolveTime(alert.getResolvedAt())
                .resolveComment(alert.getResolutionNote())
                .createTime(alert.getCreateTime())
                .build();
    }

    private List<Map<String, Object>> parseNotificationLog(String json) {
        if (StrUtil.isBlank(json)) {
            return new java.util.ArrayList<>();
        }
        try {
            List<Map<String, Object>> parsed = JsonUtils.fromJson(
                    json,
                    new com.fasterxml.jackson.core.type.TypeReference<>() {
                    });
            return parsed != null ? new java.util.ArrayList<>(parsed) : new java.util.ArrayList<>();
        } catch (RuntimeException ignored) {
            return new java.util.ArrayList<>();
        }
    }

    private String notificationStatus(List<Map<String, Object>> logs) {
        if (logs.isEmpty()) {
            return "NOT_SENT";
        }
        boolean failed = logs.stream().anyMatch(item -> "FAILED".equals(item.get("status")));
        boolean success = logs.stream().anyMatch(item -> "SUCCESS".equals(item.get("status")));
        if (success && !failed) {
            return "SUCCESS";
        }
        if (success) {
            return "PARTIAL";
        }
        if (failed) {
            return "FAILED";
        }
        return "SKIPPED";
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(String.valueOf(value));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String upper(String value) {
        return value == null ? null : value.toUpperCase(java.util.Locale.ROOT);
    }

    @Scheduled(fixedRate = 300000)
    public void retryFailedNotifications() {
        List<AlertRecord> firingAlerts = recordRepository.findFiringAlerts();
        for (AlertRecord alert : firingAlerts) {
            notifyAlert(alert);
        }
    }

    private String toChannelsJson(String channels) {
        if (StrUtil.isBlank(channels)) {
            return null;
        }
        String trimmed = channels.trim();
        if (trimmed.startsWith("[")) {
            return trimmed;
        }
        return JsonUtils.toJson(Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .toList());
    }

    private List<String> parseChannels(String channels) {
        if (StrUtil.isBlank(channels)) {
            return List.of();
        }
        String trimmed = channels.trim();
        if (trimmed.startsWith("[")) {
            try {
                List<String> parsed = JsonUtils.fromJsonToList(trimmed, String.class);
                return parsed != null ? parsed : List.of();
            } catch (RuntimeException ignored) {
                log.warn("Invalid alert notification channels JSON: {}", channels);
            }
        }
        return Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    private String getCurrentUserId() {
        return Optional.ofNullable(UserContextHolder.get())
                .map(ctx -> String.valueOf(ctx.getUserId()))
                .orElse("system");
    }

    private Long getCurrentUserIdLong() {
        return Optional.ofNullable(UserContextHolder.get())
                .map(UserContext::getUserId)
                .orElse(0L);
    }

    private String generateRuleCode(String name) {
        if (StrUtil.isBlank(name)) {
            return "RULE_" + System.currentTimeMillis();
        }
        return "RULE_" + name.replaceAll("[^a-zA-Z0-9]", "_").toUpperCase() + "_" + System.currentTimeMillis();
    }
}
