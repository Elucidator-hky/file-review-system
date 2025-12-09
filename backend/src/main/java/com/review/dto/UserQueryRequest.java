package com.review.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import java.util.List;

/**
 * 企业管理员用户列表查询请求
 */
@Data
public class UserQueryRequest {

    /** 页码 */
    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNo = 1;

    /** 每页数量 */
    @Min(value = 1, message = "每页数量必须大于0")
    private Integer pageSize = 10;

    /** 状态：1-启用 0-停用 */
    private Integer status;

    /** 角色筛选（可多选：TENANT_ADMIN、REVIEWER、USER） */
    private List<String> roles;

    /** 关键字（支持用户名/真实姓名/手机号） */
    private String keyword;
}
