package com.meowflow.common.web;

import com.meowflow.common.context.UserContext;
import com.meowflow.common.context.UserContextHolder;

public final class WebContext {

    private WebContext() {
    }

    public static UserContext currentUser() {
        return UserContextHolder.get();
    }

    public static String currentUserId() {
        Long userId = UserContextHolder.getUserId();
        return userId == null ? null : String.valueOf(userId);
    }

    public static boolean isLoggedIn() {
        return !UserContextHolder.isAnonymous();
    }
}