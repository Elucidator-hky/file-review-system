package com.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件列表响应模型。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileInfoResponse {

    private Long id;

    private String fileName;

    private Long fileSize;

    private String fileType;

    private LocalDateTime uploadTime;
}
