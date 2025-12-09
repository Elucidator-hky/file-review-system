package com.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 版本状态查询响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VersionStatusResponse {

    private Long versionId;

    private String status;

    private Integer filesReady;

    private Integer fileCount;
}
