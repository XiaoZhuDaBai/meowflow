package com.meowflow.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.List;

public final class UserContextHolder {

    private static final TransmittableThreadLocal<UserContext> CONTEXT = new TransmittableThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(UserContext context) {
        CONTEXT.set(context);
    }

    public static UserContext get() {
        return CONTEXT.get();
    }

    public static void remove() {
        CONTEXT.remove();
    }

    public static Long getUserId() {
        UserContext ctx = CONTEXT.get();
        return ctx == null ? null : ctx.getUserId();
    }

    public static String getUsername() {
        UserContext ctx = CONTEXT.get();
        return ctx == null ? null : ctx.getUsername();
    }

    public static Long getOrgId() {
        UserContext ctx = CONTEXT.get();
        return ctx == null ? null : ctx.getOrgId();
    }

    public static List<String> getRoles() {
        UserContext ctx = CONTEXT.get();
        return ctx == null ? null : ctx.getRoles();
    }

    public static boolean isAnonymous() {
        UserContext ctx = CONTEXT.get();
        return ctx == null || ctx.getUserId() == null;
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static boolean isAdmin() {
        UserContext ctx = CONTEXT.get();
        if (ctx == null || ctx.getRoles() == null) {
            return false;
        }
        return ctx.getRoles().contains("admin");
    }
}