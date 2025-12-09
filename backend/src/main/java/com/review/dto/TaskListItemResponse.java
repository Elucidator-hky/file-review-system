package com.review.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务列表返回数据。
 */
@Data
public class TaskListItemResponse {

    private Long taskId;

    private String taskName;

    private Integer currentVersion;

    private Long currentVersionId;

    private String currentStatus;

    private String statusLabel;

    private String statusTagType;

    private LocalDateTime createTime;

    private LocalDateTime lastUpdateTime;

    private Integer fileCount;

    private boolean canResubmit;

    private Long reviewerId;

    private String reviewerName;

    private String reviewerPhone;

    private Long creatorId;

    private String creatorName;

    private String creatorPhone;

    /**
     * 审查员视角：待处理天数
     */
    private Integer pendingDays;
}
