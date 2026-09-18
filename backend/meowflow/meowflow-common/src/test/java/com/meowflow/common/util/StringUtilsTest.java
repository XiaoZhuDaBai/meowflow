package com.meowflow.common.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {

    @Test
    void isBlank_shouldReturnTrueForNull() {
        assertTrue(StringUtils.isBlank(null));
    }

    @Test
    void isBlank_shouldReturnTrueForEmptyString() {
        assertTrue(StringUtils.isBlank(""));
    }

    @Test
    void isBlank_shouldReturnTrueForWhitespaceOnly() {
        assertTrue(StringUtils.isBlank("   "));
        assertTrue(StringUtils.isBlank("\t"));
        assertTrue(StringUtils.isBlank("\n"));
    }

    @Test
    void isBlank_shouldReturnFalseForNonBlank() {
        assertFalse(StringUtils.isBlank("hello"));
        assertFalse(StringUtils.isBlank("  hello  "));
    }

    @Test
    void isNotBlank_shouldReturnOpposite() {
        assertFalse(StringUtils.isNotBlank(null));
        assertFalse(StringUtils.isNotBlank(""));
        assertFalse(StringUtils.isNotBlank("   "));
        assertTrue(StringUtils.isNotBlank("hello"));
    }

    @Test
    void isEmpty_shouldReturnTrueForNull() {
        assertTrue(StringUtils.isEmpty(null));
    }

    @Test
    void isEmpty_shouldReturnTrueForEmptyString() {
        assertTrue(StringUtils.isEmpty(""));
    }

    @Test
    void isEmpty_shouldReturnFalseForWhitespace() {
        assertFalse(StringUtils.isEmpty("   "));
    }

    @Test
    void isEmpty_shouldReturnFalseForNonEmpty() {
        assertFalse(StringUtils.isEmpty("hello"));
    }

    @Test
    void isNotEmpty_shouldReturnOpposite() {
        assertFalse(StringUtils.isNotEmpty(null));
        assertFalse(StringUtils.isNotEmpty(""));
        assertTrue(StringUtils.isNotEmpty("hello"));
    }

    @Test
    void defaultIfBlank_shouldReturnFallbackForNull() {
        assertEquals("fallback", StringUtils.defaultIfBlank(null, "fallback"));
    }

    @Test
    void defaultIfBlank_shouldReturnFallbackForEmpty() {
        assertEquals("fallback", StringUtils.defaultIfBlank("", "fallback"));
    }

    @Test
    void defaultIfBlank_shouldReturnFallbackForWhitespace() {
        assertEquals("fallback", StringUtils.defaultIfBlank("   ", "fallback"));
    }

    @Test
    void defaultIfBlank_shouldReturnOriginalForNonBlank() {
        assertEquals("original", StringUtils.defaultIfBlank("original", "fallback"));
    }
}
