package com.review.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 审查操作请求体。
 */
@Data
public class ReviewActionRequest {

    @Size(max = 1000, message = "审查意见不能超过1000个字符")
    private String comment;
}
