package com.meowflow.user.service;

import com.meowflow.user.dto.LoginLogDTO;
import com.meowflow.user.dto.LoginLogQuery;
import com.meowflow.user.entity.LoginLog;
import com.meowflow.user.repository.LoginLogRepository;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final LoginLogRepository loginLogRepository;

    public void logSuccess(Long userId, String username, String ip, String userAgent, String region, String msg) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUserId(userId);
        loginLog.setUsername(username);
        loginLog.setIp(ip);
        loginLog.setUserAgent(userAgent);
        loginLog.setRegion(region != null ? region : "未知");
        loginLog.setStatus("success");
        loginLog.setMessage(msg != null ? msg : "登录成功");
        loginLog.setLoginAt(LocalDateTime.now());
        loginLogRepository.insert(loginLog);
    }

    public void logFail(Long userId, String username, String ip, String userAgent, String region, String msg) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUserId(userId);
        loginLog.setUsername(username);
        loginLog.setIp(ip);
        loginLog.setUserAgent(userAgent);
        loginLog.setRegion(region != null ? region : "未知");
        loginLog.setStatus("fail");
        loginLog.setMessage(msg);
        loginLog.setLoginAt(LocalDateTime.now());
        loginLogRepository.insert(loginLog);
    }

    public List<LoginLogDTO> getLogsByUsername(String username) {
        List<LoginLog> logs = loginLogRepository.findByUsername(username);
        return logs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<LoginLogDTO> getRecentLogs(int limit) {
        List<LoginLog> logs = loginLogRepository.findByStatus("success");
        return logs.stream()
                .limit(limit)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public int countFailedLogins(String username, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return loginLogRepository.countFailedLoginsSince(username, since);
    }

    public IPage<LoginLogDTO> pageQuery(LoginLogQuery query) {
        Page<LoginLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        return loginLogRepository.pageQuery(
                page,
                query.getUsername(),
                query.getStatus(),
                query.getStartTime(),
                query.getEndTime()).convert(this::convertToDTO);
    }

    public void clean() {
        loginLogRepository.deleteAll();
    }

    private LoginLogDTO convertToDTO(LoginLog log) {
        return LoginLogDTO.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .username(log.getUsername())
                .ip(log.getIp())
                .region(log.getRegion())
                .userAgent(log.getUserAgent())
                .os(null)
                .status(log.getStatus())
                .message(log.getMessage())
                .loginAt(log.getLoginAt())
                .build();
    }
}
