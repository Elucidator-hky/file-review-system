package com.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建任务响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateResponse {

    private Long taskId;

    private Long versionId;
}
