package com.review.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.review.common.BusinessException;
import com.review.common.Result;
import com.review.common.UserContext;
import com.review.dto.UserCreateRequest;
import com.review.dto.UserQueryRequest;
import com.review.dto.UserResetPasswordRequest;
import com.review.dto.UserResponse;
import com.review.dto.UserStatusBatchRequest;
import com.review.dto.UserStatusUpdateRequest;
import com.review.dto.UserUpdateRequest;
import com.review.service.TenantUserService;
import com.review.utils.RoleUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业管理员 - 用户管理
 */
@Api(tags = "企业用户管理")
@RestController
@RequestMapping("/admin/users")
public class TenantUserController {

    @Autowired
    private TenantUserService tenantUserService;

    private Long ensureTenantAdmin() {
        if (!RoleUtil.hasRole(UserContext.getCurrentRoles(), "TENANT_ADMIN")) {
            throw new BusinessException("当前账号无权执行该操作");
        }
        Long tenantId = UserContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }
        return tenantId;
    }

    @ApiOperation("分页查询企业用户")
    @GetMapping("/list")
    public Result<Page<UserResponse>> listUsers(@Validated UserQueryRequest request) {
        Long tenantId = ensureTenantAdmin();
        return Result.success(tenantUserService.queryUsers(tenantId, request));
    }

    @ApiOperation("新增企业用户")
    @PostMapping("/create")
    public Result<Void> createUser(@Validated @RequestBody UserCreateRequest request) {
        Long tenantId = ensureTenantAdmin();
        tenantUserService.createUser(tenantId, request);
        return Result.success();
    }

    @ApiOperation("编辑企业用户")
    @PutMapping("/{userId}")
    public Result<Void> updateUser(@PathVariable Long userId,
                                   @Validated @RequestBody UserUpdateRequest request) {
        Long tenantId = ensureTenantAdmin();
        tenantUserService.updateUser(tenantId, userId, request);
        return Result.success();
    }

    @ApiOperation("重置用户密码")
    @PostMapping("/{userId}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long userId,
                                      @Validated @RequestBody UserResetPasswordRequest request) {
        Long tenantId = ensureTenantAdmin();
        tenantUserService.resetPassword(tenantId, userId, request);
        return Result.success();
    }

    @ApiOperation("单个启用/停用用户")
    @PutMapping("/{userId}/status")
    public Result<Void> updateStatus(@PathVariable Long userId,
                                     @Validated @RequestBody UserStatusUpdateRequest request) {
        Long tenantId = ensureTenantAdmin();
        tenantUserService.updateStatus(tenantId, userId, request);
        return Result.success();
    }

    @ApiOperation("批量启用/停用用户")
    @PutMapping("/status/batch")
    public Result<Void> batchUpdateStatus(@Validated @RequestBody UserStatusBatchRequest request) {
        Long tenantId = ensureTenantAdmin();
        tenantUserService.batchUpdateStatus(tenantId, request);
        return Result.success();
    }
}
