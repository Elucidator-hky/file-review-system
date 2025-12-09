package com.review.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户信息响应
 */
@Data
public class TenantResponse {

    private Long id;

    private String tenantName;

    private String contactName;

    private String contactPhone;

    private Long storageQuota;

    private Long storageUsed;

    private Integer userQuota;

    private Integer userCount;

    /** 状态：1-正常，0-停用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
