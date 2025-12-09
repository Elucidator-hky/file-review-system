package com.review.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 租户管理员创建请求
 */
@Data
public class TenantAdminCreateRequest {

    /** 用户名（企业唯一） */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需在3~20个字符之间")
    private String username;

    /** 真实姓名 */
    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名不能超过50个字符")
    private String realName;

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    @Size(max = 20, message = "手机号不能超过20个字符")
    private String phone;

    /** 初始密码 */
    @NotBlank(message = "初始密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在6~32个字符之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,32}$", message = "密码需同时包含字母和数字")
    private String password;
}
