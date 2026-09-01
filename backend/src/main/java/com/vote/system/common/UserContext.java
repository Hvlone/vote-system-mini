package com.vote.system.common;

/**
 * 当前登录用户上下文（ThreadLocal，请求结束由拦截器清理）
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> OPENID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    public static void set(Long userId, String openid, String role) {
        USER_ID.set(userId);
        OPENID.set(openid);
        ROLE.set(role);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static String getOpenid() {
        return OPENID.get();
    }

    public static String getRole() {
        return ROLE.get();
    }

    public static void clear() {
        USER_ID.remove();
        OPENID.remove();
        ROLE.remove();
    }
}
