package com.review.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 再次提交初始化请求。
 */
@Data
public class ResubmitInitRequest {

    @NotNull(message = "需要指定要沿用的版本")
    private Long oldVersionId;

    /**
     * 是否沿用旧文件
     */
    private Boolean reuseOldFiles = Boolean.FALSE;
}
