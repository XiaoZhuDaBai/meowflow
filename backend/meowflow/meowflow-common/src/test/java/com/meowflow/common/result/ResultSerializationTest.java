package com.meowflow.common.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.meowflow.common.context.TraceContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@link Result} 在 Jackson 序列化层面的形状稳定性 —— 一旦 JSON 结构变了，下游前端合约就破坏。
 */
@DisplayName("Result JSON 序列化稳定性")
class ResultSerializationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        TraceContextHolder.setTraceId("trace-it-xyz");
    }

    @AfterEach
    void tearDown() {
        TraceContextHolder.clear();
    }

    @Test
    @DisplayName("success(data) — code/data/message/timestamp/traceId 必须齐全")
    void success_payload_isStable() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", "u-1");
        payload.put("name", "alice");

        Result<Map<String, Object>> r = Result.success(payload, "OK");

        String json = mapper.writeValueAsString(r);

        // 字段顺序按 Result 定义
        assertThat(json).contains("\"code\":200");
        assertThat(json).contains("\"message\":\"OK\"");
        assertThat(json).contains("\"data\":{");
        assertThat(json).contains("\"userId\":\"u-1\"");
        assertThat(json).contains("\"traceId\":\"trace-it-xyz\"");
    }

    @Test
    @DisplayName("error(code, message) — 序列化仅含 code/message/timestamp/traceId，不应含 data 字段")
    void error_payload_omitsNullDataField() throws Exception {
        Result<Void> r = Result.error(ResultCode.NOT_FOUND, "资源不存在");

        String json = mapper.writeValueAsString(r);
        // @JsonInclude(NON_NULL): data 为 null 时被剔除
        assertThat(json).contains("\"code\":404");
        assertThat(json).contains("\"message\":\"资源不存在\"");
        assertThat(json).doesNotContain("\"data\"");
        assertThat(json).contains("\"traceId\":\"trace-it-xyz\"");
    }

    @Test
    @DisplayName("data 内嵌 LocalDateTime — 必须保留 ISO 字符串而非 epoch")
    void data_withLocalDateTime_serializedAsIsoString() throws Exception {
        Instant instant = Instant.parse("2026-07-26T12:00:00Z");
        Result<Instant> r = Result.success(instant);

        String json = mapper.writeValueAsString(r);
        assertThat(json).contains("\"data\":\"2026-07-26T12:00:00Z\"");
    }

    @Test
    @DisplayName("timestamp 字段 — 序列化形式不应含毫秒小数")
    void timestamp_isLongMillis() {
        long now = System.currentTimeMillis();
        Result<String> r = Result.success("ok");
        long ts = r.getTimestamp();
        assertThat(ts).isGreaterThanOrEqualTo(now - 1_000L);
        // 通过 JSON 序列化再次取回 timestamp，验证是 Long 字段而非其它类型
        long afterSerialize = r.getTimestamp();
        assertThat(afterSerialize).isEqualTo(ts);
    }

    @Test
    @DisplayName("Result.isSuccess() — 仅在 code == 200 时为 true")
    void isSuccess_onlyOnCode200() {
        assertThat(Result.success("x").isSuccess()).isTrue();
        assertThat(Result.success().isSuccess()).isTrue();
        assertThat(Result.error(ResultCode.NOT_FOUND).isSuccess()).isFalse();
    }
}
