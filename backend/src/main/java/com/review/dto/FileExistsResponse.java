package com.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件秒传校验响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileExistsResponse {

    /** 是否已存在同 MD5 的文件 */
    private boolean exists;

    /** 如果存在，返回 MinIO 对象名供排查 */
    private String minioObjectName;
}
