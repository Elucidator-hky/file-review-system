package com.review.controller;

import com.review.common.Result;
import com.review.dto.CacheMonitorResponse;
import com.review.dto.QueueMonitorResponse;
import com.review.service.MonitorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 队列 / 缓存 监控接口
 */
@Api(tags = "监控面板")
@RestController
@RequestMapping("/monitor")
public class MonitorController {

    @Autowired
    private MonitorService monitorService;

    @ApiOperation("获取队列监控数据")
    @GetMapping("/queues")
    public Result<QueueMonitorResponse> queues() {
        return Result.success(monitorService.loadQueueMetrics());
    }

    @ApiOperation("获取缓存监控数据")
    @GetMapping("/cache")
    public Result<CacheMonitorResponse> cache() {
        return Result.success(monitorService.loadCacheMetrics());
    }
}
