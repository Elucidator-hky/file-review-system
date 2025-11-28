package com.review.controller;

import com.review.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口
 *
 * @author Claude
 * @date 2025-11-28
 */
@Api(tags = "系统健康检查")
@RestController
@RequestMapping("/health")
public class HealthController {

    @ApiOperation("健康检查")
    @GetMapping
    public Result<String> health() {
        return Result.success("系统运行正常");
    }
}
