package com.meowflow.common.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BeanUtilsTest {

    @Test
    void getNullPropertyNames_shouldReturnAllNullProperties() {
        TestSource source = new TestSource();
        source.setName("test");
        String[] nullNames = BeanUtils.getNullPropertyNames(source);
        assertTrue(java.util.Arrays.asList(nullNames).contains("value"));
        assertFalse(java.util.Arrays.asList(nullNames).contains("name"));
    }

    @Test
    void getNullPropertyNames_shouldExcludeClassProperty() {
        TestSource source = new TestSource();
        source.setName("test");
        String[] nullNames = BeanUtils.getNullPropertyNames(source);
        assertFalse(java.util.Arrays.asList(nullNames).contains("class"));
    }

    @Test
    void copyPropertiesIgnoreNull_shouldCopyNonNullProperties() {
        TestSource source = new TestSource();
        source.setName("test");
        source.setValue(123);

        TestTarget target = new TestTarget();
        BeanUtils.copyPropertiesIgnoreNull(source, target);
        assertEquals("test", target.getName());
        assertEquals(123, target.getValue());
    }

    @Test
    void copyPropertiesIgnoreNull_shouldIgnoreNullProperties() {
        TestSource source = new TestSource();
        source.setName("test");
        // value is null (default)

        TestTarget target = new TestTarget();
        target.setName("original");
        target.setValue(999);

        BeanUtils.copyPropertiesIgnoreNull(source, target);
        assertEquals("test", target.getName());
        assertEquals(999, target.getValue());
    }

    @Test
    void toMap_shouldConvertBeanToMap() {
        TestSource source = new TestSource();
        source.setName("test");
        source.setValue(123);

        Map<String, Object> map = BeanUtils.toMap(source);
        assertNotNull(map);
        assertEquals("test", map.get("name"));
        assertEquals(123, map.get("value"));
    }

    @Test
    void toMap_shouldReturnEmptyMapForNull() {
        Map<String, Object> map = BeanUtils.toMap(null);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    void toMap_shouldExcludeClassProperty() {
        TestSource source = new TestSource();
        source.setName("test");
        Map<String, Object> map = BeanUtils.toMap(source);
        assertFalse(map.containsKey("class"));
    }

    public static class TestSource {
        private String name;
        private Integer value;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getValue() { return value; }
        public void setValue(Integer value) { this.value = value; }
    }

    public static class TestTarget {
        private String name;
        private Integer value;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getValue() { return value; }
        public void setValue(Integer value) { this.value = value; }
    }
}
