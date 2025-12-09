package com.review.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 企业用户响应
 */
@Data
public class UserResponse {

    private Long id;

    private String username;

    private String realName;

    private String phone;

    private List<String> roles;

    private Integer status;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;
}
