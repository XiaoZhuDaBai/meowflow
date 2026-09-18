package com.meowflow.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.findAndRegisterModules();
        // 持久化的定义 JSON 可能由前端 DSL 写入（例如 edge.data.type / edge.data.category），
        // 这些字段在服务端模型里并不存在。反序列化时必须忽略未知字段，
        // 否则发布 / 执行时会因 UnrecognizedPropertyException 直接 500。
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private JsonUtils() {
    }

    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON serialize error: " + e.getMessage(), e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON deserialize error: " + e.getMessage(), e);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> ref) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, ref);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON deserialize error: " + e.getMessage(), e);
        }
    }

    public static <T> List<T> fromJsonToList(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON deserialize error: " + e.getMessage(), e);
        }
    }

    public static Map<String, Object> fromJsonToMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON deserialize error: " + e.getMessage(), e);
        }
    }
}