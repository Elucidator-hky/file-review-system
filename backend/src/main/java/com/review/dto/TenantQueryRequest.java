package com.review.dto;

import lombok.Data;

import javax.validation.constraints.Min;

/**
 * 租户列表查询请求
 */
@Data
public class TenantQueryRequest {

    /** 页码（从1开始） */
    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNo = 1;

    /** 每页数量 */
    @Min(value = 1, message = "每页数量必须大于0")
    private Integer pageSize = 10;

    /** 状态：1-正常，0-停用 */
    private Integer status;

    /** 企业名称关键字 */
    private String keyword;
}
