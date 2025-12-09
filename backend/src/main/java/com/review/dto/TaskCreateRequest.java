package com.review.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 创建审查任务请求。
 */
@Data
public class TaskCreateRequest {

    /** 任务名称 */
    @NotBlank(message = "任务名称不能为空")
    @Size(max = 100, message = "任务名称长度不能超过100个字符")
    private String taskName;

    /** 审查员 ID */
    @NotNull(message = "请选择审查员")
    private Long reviewerId;

    /** 提交说明 */
    @Size(max = 2000, message = "提交说明长度不能超过2000字符")
    private String submitDesc;
}
