package com.review.utils;

import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * 角色字符串解析工具
 */
public final class RoleUtil {

    private RoleUtil() {
    }

    /**
     * 判断给定角色串中是否包含指定角色
     *
     * @param roles       逗号分隔或 JSON 数组形式的角色串
     * @param targetRole  目标角色（将自动转为大写后比较）
     * @return 是否包含
     */
    public static boolean hasRole(String roles, String targetRole) {
        if (!StringUtils.hasText(roles) || !StringUtils.hasText(targetRole)) {
            return false;
        }
        // 去除可能存在的中括号、引号等符号
        String normalized = roles.replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .replace("'", "");
        return Arrays.stream(normalized.split(","))
                .map(role -> role == null ? "" : role.trim().toUpperCase())
                .filter(StringUtils::hasText)
                .anyMatch(role -> targetRole.toUpperCase().equals(role));
    }
}
