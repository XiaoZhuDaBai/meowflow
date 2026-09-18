package com.meowflow.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    void format_shouldReturnNullForNull() {
        assertNull(DateUtils.format(null));
    }

    @Test
    void format_shouldFormatDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 45);
        String formatted = DateUtils.format(dateTime);
        assertNotNull(formatted);
        assertEquals("2024-01-15 10:30:45", formatted);
    }

    @Test
    void format_withPattern_shouldFormatWithCustomPattern() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 45);
        String formatted = DateUtils.format(dateTime, "yyyy/MM/dd");
        assertEquals("2024/01/15", formatted);
    }

    @Test
    void parse_shouldReturnNullForNull() {
        assertNull(DateUtils.parse(null));
    }

    @Test
    void parse_shouldParseDefaultFormat() {
        LocalDateTime dateTime = DateUtils.parse("2024-01-15 10:30:45");
        assertNotNull(dateTime);
        assertEquals(2024, dateTime.getYear());
        assertEquals(1, dateTime.getMonthValue());
        assertEquals(15, dateTime.getDayOfMonth());
        assertEquals(10, dateTime.getHour());
        assertEquals(30, dateTime.getMinute());
        assertEquals(45, dateTime.getSecond());
    }

    @Test
    void parse_withPattern_shouldParseDateTimeWithTime() {
        LocalDateTime dateTime = DateUtils.parse("2024/01/15 10:30:00", "yyyy/MM/dd HH:mm:ss");
        assertNotNull(dateTime);
        assertEquals(2024, dateTime.getYear());
        assertEquals(1, dateTime.getMonthValue());
        assertEquals(15, dateTime.getDayOfMonth());
    }

    @Test
    void now_shouldReturnCurrentTime() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        LocalDateTime now = DateUtils.now();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertTrue(now.isAfter(before));
        assertTrue(now.isBefore(after) || now.isEqual(after));
    }

    @Test
    void nowStr_shouldReturnFormattedString() {
        String nowStr = DateUtils.nowStr();
        assertNotNull(nowStr);
        assertTrue(nowStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void nowCompact_shouldReturnCompactFormat() {
        String nowCompact = DateUtils.nowCompact();
        assertNotNull(nowCompact);
        assertTrue(nowCompact.matches("\\d{14}"));
    }

    @Test
    void roundTrip_shouldMaintainConsistency() {
        LocalDateTime original = LocalDateTime.of(2024, 6, 20, 14, 30, 0);
        String formatted = DateUtils.format(original);
        LocalDateTime parsed = DateUtils.parse(formatted);
        assertEquals(original, parsed);
    }

    @Test
    void format_shouldHandleDatePattern() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 6, 20, 0, 0, 0);
        String formatted = DateUtils.format(dateTime, DateUtils.DATE_PATTERN);
        assertEquals("2024-06-20", formatted);
    }
}
