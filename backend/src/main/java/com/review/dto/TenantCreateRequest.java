package com.review.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 租户创建请求
 */
@Data
public class TenantCreateRequest {

    /** 企业名称 */
    @NotBlank(message = "企业名称不能为空")
    @Size(max = 100, message = "企业名称不能超过100个字符")
    private String tenantName;

    /** 联系人姓名 */
    @NotBlank(message = "联系人姓名不能为空")
    @Size(max = 50, message = "联系人姓名不能超过50个字符")
    private String contactName;

    /** 联系电话 */
    @NotBlank(message = "联系电话不能为空")
    @Size(max = 20, message = "联系电话不能超过20个字符")
    private String contactPhone;

    /** 存储配额（字节） */
    @NotNull(message = "存储配额不能为空")
    @Min(value = 10737418240L, message = "存储配额至少10GB")
    private Long storageQuota;

    /** 用户数量配额 */
    @NotNull(message = "用户配额不能为空")
    @Min(value = 1, message = "用户配额必须大于0")
    private Integer userQuota;
}
