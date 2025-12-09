package com.review.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审查详情返回体。
 */
@Data
public class ReviewDetailResponse {

    private Long taskId;

    private Long versionId;

    private String taskName;

    private Integer versionNumber;

    private String submitDesc;

    private LocalDateTime submitTime;

    private String status;

    private Integer fileCount;

    private Integer filesReady;

    private RelationInfo creator;

    private RelationInfo reviewer;

    private PreviousVersionInfo previousVersion;

    private List<FileInfoResponse> files;

    @Data
    public static class RelationInfo {
        private Long userId;
        private String username;
        private String realName;
        private String phone;
    }

    @Data
    public static class PreviousVersionInfo {
        private Integer versionNumber;
        private String status;
        private String reviewComment;
        private LocalDateTime reviewTime;
    }
}
