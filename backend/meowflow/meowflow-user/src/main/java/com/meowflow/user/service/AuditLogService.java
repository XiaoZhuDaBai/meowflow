package com.meowflow.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.user.dto.AuditLogDTO;
import com.meowflow.user.dto.AuditLogQuery;
import com.meowflow.user.entity.AuditLog;
import com.meowflow.user.repository.AuditLogRepository;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public IPage<AuditLogDTO> pageQuery(AuditLogQuery query) {
        Page<AuditLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        return auditLogRepository.pageQuery(
                page,
                query.getUsername(),
                query.getModule(),
                query.getStatus(),
                query.getStartTime(),
                query.getEndTime()
        ).convert(this::convertToDTO);
    }

    public AuditLogDTO getById(Long id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.DATA_NOT_FOUND, "审计日志不存在"));
        return convertToDTO(log);
    }

    public List<AuditLogDTO> getByUsername(String username) {
        return auditLogRepository.findByUsername(username).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private AuditLogDTO convertToDTO(AuditLog log) {
        return AuditLogDTO.builder()
                .id(log.getId())
                .username(log.getUsername())
                .module(log.getModule())
                .method(log.getMethod())
                .requestMethod(log.getRequestMethod())
                .requestUrl(log.getRequestUrl())
                .requestParams(log.getRequestParams())
                .responseResult(log.getResponseResult())
                .ip(log.getIp())
                .costTime(log.getCostTime())
                .status(log.getStatus())
                .errorMsg(log.getErrorMsg())
                .operateTime(log.getOperateTime())
                .build();
    }
}
