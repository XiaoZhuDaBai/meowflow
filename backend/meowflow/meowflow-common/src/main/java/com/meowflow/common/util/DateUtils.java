package com.meowflow.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    public static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String COMPACT_PATTERN = "yyyyMMddHHmmss";

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_PATTERN);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    private static final DateTimeFormatter COMPACT_FORMATTER = DateTimeFormatter.ofPattern(COMPACT_PATTERN);

    private DateUtils() {
    }

    public static String format(LocalDateTime time) {
        return time == null ? null : time.format(DEFAULT_FORMATTER);
    }

    public static String format(LocalDateTime time, String pattern) {
        return time == null ? null : time.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDateTime parse(String text) {
        return text == null ? null : LocalDateTime.parse(text, DEFAULT_FORMATTER);
    }

    public static LocalDateTime parse(String text, String pattern) {
        return text == null ? null : LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.systemDefault());
    }

    public static String nowStr() {
        return LocalDateTime.now().format(DEFAULT_FORMATTER);
    }

    public static String nowCompact() {
        return LocalDateTime.now().format(COMPACT_FORMATTER);
    }
}