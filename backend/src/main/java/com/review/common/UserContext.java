package com.review.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 使用 ThreadLocal 存储当前登录用户上下文
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {

    private Long userId;

    private String username;

    private Long tenantId;

    private String roles;

    private static final ThreadLocal<UserContext> CONTEXT_HOLDER = new ThreadLocal<>();

    public static void setContext(UserContext context) {
        CONTEXT_HOLDER.set(context);
    }

    public static UserContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    public static Long getCurrentUserId() {
        UserContext context = getContext();
        return context != null ? context.getUserId() : null;
    }

    public static String getCurrentUsername() {
        UserContext context = getContext();
        return context != null ? context.getUsername() : null;
    }

    public static Long getCurrentTenantId() {
        UserContext context = getContext();
        return context != null ? context.getTenantId() : null;
    }

    public static String getCurrentRoles() {
        UserContext context = getContext();
        return context != null ? context.getRoles() : null;
    }
}
