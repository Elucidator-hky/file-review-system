package com.review.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务详情返回信息。
 */
@Data
public class TaskDetailResponse {

    private Long taskId;

    private String taskName;

    private Integer currentVersion;

    private String currentStatus;

    private String statusLabel;

    private boolean canResubmit;

    private RelationInfo reviewer;

    private RelationInfo creator;

    private VersionDetail currentVersionDetail;

    private List<TaskVersionResponse> versions;

    private List<TaskTimelineNode> timeline;

    @Data
    public static class RelationInfo {
        private Long userId;
        private String username;
        private String realName;
        private String phone;
    }

    @Data
    public static class VersionDetail {
        private Long versionId;
        private Integer versionNumber;
        private String status;
        private String statusLabel;
        private Integer fileCount;
        private Integer filesReady;
        private String submitDesc;
        private String reviewComment;
        private LocalDateTime submitTime;
        private LocalDateTime reviewTime;
        private String reviewerName;
    }
}
