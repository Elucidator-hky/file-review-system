package com.review.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件复制异步消息载体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileCopyMessage {

    /**
     * 当前租户 ID，用于校验多租户隔离。
     */
    private Long tenantId;

    /**
     * 任务 ID，便于追踪日志。
     */
    private Long taskId;

    /**
     * 旧版本 ID（被复制来源）。
     */
    private Long oldVersionId;

    /**
     * 新版本 ID（复制目标）。
     */
    private Long newVersionId;

    /**
     * traceId，串联日志。
     */
    private String traceId;
}
