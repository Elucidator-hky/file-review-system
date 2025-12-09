package com.review.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户管理员列表响应
 */
@Data
public class TenantAdminResponse {

    private Long id;

    private String username;

    private String realName;

    private String phone;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime lastLoginTime;
}
