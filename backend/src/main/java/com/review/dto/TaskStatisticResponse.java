package com.review.dto;

import lombok.Data;

/**
 * 任务统计信息。
 */
@Data
public class TaskStatisticResponse {

    private Long total = 0L;

    private Long reviewing = 0L;

    private Long approved = 0L;

    private Long rejected = 0L;

    private Long monthTotal = 0L;

    private Long monthApproved = 0L;

    private Double passRate = 0.0;
}
