package com.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审查任务实体。
 */
@Data
@TableName("review_task")
public class ReviewTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 任务名称 */
    private String taskName;

    /** 发起人 ID */
    private Long creatorId;

    /** 审查员 ID */
    private Long reviewerId;

    /** 当前版本号 */
    private Integer currentVersion;

    /** 当前状态：REVIEWING / APPROVED / REJECTED */
    private String currentStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
