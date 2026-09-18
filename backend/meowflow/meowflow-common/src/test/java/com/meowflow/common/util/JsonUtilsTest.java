package com.meowflow.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilsTest {

    @Test
    void toJson_shouldReturnNullForNull() {
        assertNull(JsonUtils.toJson(null));
    }

    @Test
    void toJson_shouldSerializeObject() {
        TestBean bean = new TestBean("test", 123);
        String json = JsonUtils.toJson(bean);
        assertNotNull(json);
        assertTrue(json.contains("name"));
        assertTrue(json.contains("test"));
        assertTrue(json.contains("value"));
        assertTrue(json.contains("123"));
    }

    @Test
    void toJson_shouldSerializeList() {
        java.util.List<TestBean> list = java.util.List.of(
            new TestBean("a", 1),
            new TestBean("b", 2)
        );
        String json = JsonUtils.toJson(list);
        assertNotNull(json);
        assertTrue(json.contains("a") && json.contains("b"));
    }

    @Test
    void fromJson_shouldReturnNullForNull() {
        assertNull(JsonUtils.fromJson(null, TestBean.class));
    }

    @Test
    void fromJson_shouldReturnNullForEmpty() {
        assertNull(JsonUtils.fromJson("", TestBean.class));
    }

    @Test
    void fromJson_shouldDeserializeObject() {
        String json = "{\"name\":\"test\",\"value\":123}";
        TestBean bean = JsonUtils.fromJson(json, TestBean.class);
        assertNotNull(bean);
        assertEquals("test", bean.name);
        assertEquals(123, bean.value);
    }

    @Test
    void fromJson_shouldThrowOnInvalidJson() {
        assertThrows(RuntimeException.class, () -> {
            JsonUtils.fromJson("{invalid}", TestBean.class);
        });
    }

    @Test
    void fromJsonToList_shouldReturnNullForNull() {
        assertNull(JsonUtils.fromJsonToList(null, TestBean.class));
    }

    @Test
    void fromJsonToList_shouldDeserializeList() {
        String json = "[{\"name\":\"a\",\"value\":1},{\"name\":\"b\",\"value\":2}]";
        java.util.List<TestBean> list = JsonUtils.fromJsonToList(json, TestBean.class);
        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("a", list.get(0).name);
        assertEquals(1, list.get(0).value);
        assertEquals("b", list.get(1).name);
        assertEquals(2, list.get(1).value);
    }

    @Test
    void fromJsonToMap_shouldReturnNullForNull() {
        assertNull(JsonUtils.fromJsonToMap(null));
    }

    @Test
    void fromJsonToMap_shouldDeserializeMap() {
        String json = "{\"name\":\"test\",\"value\":123}";
        Map<String, Object> map = JsonUtils.fromJsonToMap(json);
        assertNotNull(map);
        assertEquals("test", map.get("name"));
        assertEquals(123, map.get("value"));
    }

    public static class TestBean {
        public String name;
        public int value;

        public TestBean() {}

        public TestBean(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }
}
