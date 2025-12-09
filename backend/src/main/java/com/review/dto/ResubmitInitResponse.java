package com.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 再次提交初始化响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResubmitInitResponse {

    private Long versionId;

    private Integer versionNumber;
}
