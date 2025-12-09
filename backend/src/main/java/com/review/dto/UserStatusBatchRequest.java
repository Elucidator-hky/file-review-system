package com.review.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 用户批量状态更新请求
 */
@Data
public class UserStatusBatchRequest {

    @NotEmpty(message = "请选择需要操作的用户")
    private List<Long> userIds;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
