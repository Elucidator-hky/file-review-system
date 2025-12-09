package com.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务时间线节点。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskTimelineNode {

    private String title;

    private String description;

    private LocalDateTime time;
}
