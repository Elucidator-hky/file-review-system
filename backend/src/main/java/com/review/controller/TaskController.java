package com.review.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.review.common.BusinessException;
import com.review.common.Result;
import com.review.common.RateLimiter;
import com.review.common.UserContext;
import com.review.dto.ReviewerOptionResponse;
import com.review.dto.TaskCreateRequest;
import com.review.dto.TaskCreateResponse;
import com.review.dto.TaskDetailResponse;
import com.review.dto.TaskListItemResponse;
import com.review.dto.TaskListQueryRequest;
import com.review.dto.ResubmitInitRequest;
import com.review.dto.ResubmitInitResponse;
import com.review.dto.ResubmitSubmitRequest;
import com.review.dto.TaskStatisticResponse;
import com.review.dto.TaskVersionResponse;
import com.review.dto.VersionStatusResponse;
import com.review.service.TaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.Duration;

/**
 * 任务管理接口。
 */
@Api(tags = "任务管理")
@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private RateLimiter rateLimiter;

    @ApiOperation("获取当前租户可用的审查员")
    @GetMapping("/reviewers")
    public Result<List<ReviewerOptionResponse>> listReviewers() {
        ensureRateLimit("task:reviewers", 20);
        return Result.success(taskService.listReviewers());
    }

    @ApiOperation("创建审查任务")
    @PostMapping
    public Result<TaskCreateResponse> createTask(@Validated @RequestBody TaskCreateRequest request) {
        return Result.success(taskService.createTask(request));
    }

    @ApiOperation("分页查询我的任务")
    @GetMapping("/mine")
    public Result<Page<TaskListItemResponse>> myTasks(@Validated TaskListQueryRequest request) {
        ensureRateLimit("task:mine:list", 30);
        return Result.success(taskService.queryMyTasks(request));
    }

    @ApiOperation("我的任务统计")
    @GetMapping("/mine/statistics")
    public Result<TaskStatisticResponse> myTaskStatistics() {
        ensureRateLimit("task:mine:stat", 20);
        return Result.success(taskService.loadMyStatistics());
    }

    @ApiOperation("分页查询审查员任务")
    @GetMapping("/reviewer")
    public Result<Page<TaskListItemResponse>> reviewerTasks(@Validated TaskListQueryRequest request) {
        ensureRateLimit("task:reviewer:list", 30);
        return Result.success(taskService.queryReviewerTasks(request));
    }

    @ApiOperation("审查员任务统计")
    @GetMapping("/reviewer/statistics")
    public Result<TaskStatisticResponse> reviewerTaskStatistics() {
        ensureRateLimit("task:reviewer:stat", 20);
        return Result.success(taskService.loadReviewerStatistics());
    }

    @ApiOperation("任务详情")
    @GetMapping("/{taskId}")
    public Result<TaskDetailResponse> getTaskDetail(@PathVariable Long taskId) {
        return Result.success(taskService.getTaskDetail(taskId));
    }

    @ApiOperation("任务版本历史")
    @GetMapping("/{taskId}/versions")
    public Result<List<TaskVersionResponse>> getTaskVersions(@PathVariable Long taskId) {
        return Result.success(taskService.listTaskVersions(taskId));
    }

    @ApiOperation("初始化再次提交")
    @PostMapping("/{taskId}/resubmit/start")
    public Result<ResubmitInitResponse> startResubmit(@PathVariable Long taskId,
                                                      @Validated @RequestBody ResubmitInitRequest request) {
        return Result.success(taskService.startResubmit(taskId, request));
    }

    @ApiOperation("提交再次提交版本")
    @PostMapping("/{taskId}/resubmit/submit")
    public Result<Void> submitResubmit(@PathVariable Long taskId,
                                       @Validated @RequestBody ResubmitSubmitRequest request) {
        taskService.submitResubmit(taskId, request);
        return Result.success();
    }

    @ApiOperation("查询版本状态")
    @GetMapping("/version/{versionId}/status")
    public Result<VersionStatusResponse> versionStatus(@PathVariable Long versionId) {
        return Result.success(taskService.getVersionStatus(versionId));
    }

    private void ensureRateLimit(String bizKey, int limitPerMinute) {
        Long userId = UserContext.getCurrentUserId();
        String finalKey = bizKey + ":" + (userId == null ? "guest" : userId);
        boolean allowed = rateLimiter.tryAcquire(finalKey, limitPerMinute, Duration.ofMinutes(1));
        if (!allowed) {
            throw new BusinessException("操作过于频繁，请稍后再试");
        }
    }
}
