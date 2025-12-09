package com.review.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务版本信息返回体。
 */
@Data
public class TaskVersionResponse {

    private Long versionId;

    private Integer versionNumber;

    private String status;

    private String statusLabel;

    private Integer filesReady;

    private Integer fileCount;

    private String submitDesc;

    private String reviewComment;

    private LocalDateTime submitTime;

    private LocalDateTime reviewTime;

    private String reviewerName;

    private boolean current;

    private String progressMsg;
}
