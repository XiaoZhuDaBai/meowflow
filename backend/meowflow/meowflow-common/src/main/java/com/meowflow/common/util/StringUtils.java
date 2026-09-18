package com.meowflow.common.util;

public final class StringUtils {

    private StringUtils() {
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static boolean isNotBlank(String s) {
        return !isBlank(s);
    }

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isNotEmpty(String s) {
        return !isEmpty(s);
    }

    public static String defaultIfBlank(String s, String fallback) {
        return isBlank(s) ? fallback : s;
    }
}