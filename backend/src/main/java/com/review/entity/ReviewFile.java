package com.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审查文件实体，记录版本下的文件元数据。
 */
@Data
@TableName("review_file")
public class ReviewFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户 ID，确保数据隔离 */
    private Long tenantId;

    /** 所属版本 ID */
    private Long versionId;

    /** 原始文件名（例如：项目方案.pdf） */
    private String fileName;

    /** MinIO 对象名，格式：tenantId/md5 */
    private String minioObjectName;

    /** 文件 MD5 值，用于秒传 */
    private String fileMd5;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件类型（Content-Type） */
    private String fileType;

    private LocalDateTime createTime;
}
