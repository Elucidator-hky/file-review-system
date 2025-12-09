package com.review.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 再次提交最终提交请求。
 */
@Data
public class ResubmitSubmitRequest {

    @NotNull(message = "缺少新版本 ID")
    private Long versionId;

    @Size(max = 2000, message = "提交说明长度不能超过2000字符")
    private String submitDesc;
}
