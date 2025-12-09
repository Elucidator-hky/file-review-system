package com.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审查版本实体（简化字段，后续章节会继续扩展）。
 */
@Data
@TableName("review_version")
public class ReviewVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 任务 ID，后续审查流程会使用 */
    private Long taskId;

    /** 版本号（v1/v2...） */
    private Integer versionNumber;

    /** 提交说明 */
    private String submitDesc;

    /** 当前状态：REVIEWING/APPROVED/REJECTED */
    private String status;

    /** 审查结果 */
    private String reviewResult;

    /** 审查意见 */
    private String reviewComment;

    /** 审查人 ID */
    private Long reviewerId;

    /** 审查时间 */
    private LocalDateTime reviewTime;

    /** 文件是否就绪：0-复制中，1-就绪 */
    private Integer filesReady;

    /** 文件数量 */
    private Integer fileCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
