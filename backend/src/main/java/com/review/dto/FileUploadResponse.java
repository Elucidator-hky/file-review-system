package com.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    /** 文件记录 ID */
    private Long fileId;

    /** 文件名 */
    private String fileName;

    /** 文件大小 */
    private Long fileSize;

    /** 是否为新上传（false 表示秒传复用） */
    private boolean newUpload;
}
