package com.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审查员下拉选项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewerOptionResponse {

    private Long userId;

    private String username;

    private String realName;

    private String phone;

    /** 最近一次登录时间 */
    private LocalDateTime lastLoginTime;

    /** 是否同时具备普通用户角色 */
    private boolean dualRole;
}
