package com.review.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.time.LocalDate;

/**
 * 任务列表查询条件。
 */
@Data
public class TaskListQueryRequest {

    @Min(value = 1, message = "页码至少为 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "每页数量至少为 1")
    @Max(value = 200, message = "每页数量不能超过 200 条")
    private Integer pageSize = 10;

    /**
     * 状态筛选：REVIEWING/APPROVED/REJECTED
     */
    private String status;

    /**
     * 关键字（任务名称模糊查询）
     */
    private String keyword;

    /**
     * 开始日期（含）
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 结束日期（含）
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 审查员列表专用：仅查看待审查
     */
    private Boolean pendingOnly;
}
