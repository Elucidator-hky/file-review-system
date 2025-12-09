package com.review.controller;

import com.review.common.Result;
import com.review.dto.ReviewActionRequest;
import com.review.dto.ReviewDetailResponse;
import com.review.service.ReviewService;
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

/**
 * 审查操作接口。
 */
@Api(tags = "审查操作")
@RestController
@RequestMapping("/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @ApiOperation("获取某个版本的审查详情")
    @GetMapping("/{versionId}")
    public Result<ReviewDetailResponse> reviewDetail(@PathVariable Long versionId) {
        return Result.success(reviewService.getReviewDetail(versionId));
    }

    @ApiOperation("审查通过")
    @PostMapping("/{versionId}/approve")
    public Result<Void> approve(@PathVariable Long versionId,
                                @Validated @RequestBody ReviewActionRequest request) {
        reviewService.approve(versionId, request);
        return Result.success();
    }

    @ApiOperation("审查打回")
    @PostMapping("/{versionId}/reject")
    public Result<Void> reject(@PathVariable Long versionId,
                               @Validated @RequestBody ReviewActionRequest request) {
        reviewService.reject(versionId, request);
        return Result.success();
    }
}
