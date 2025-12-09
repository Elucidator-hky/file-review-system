package com.review.controller;

import com.review.common.BusinessException;
import com.review.common.Result;
import com.review.common.UserContext;
import com.review.dto.TenantAdminCreateRequest;
import com.review.dto.TenantAdminResetPasswordRequest;
import com.review.dto.TenantAdminResponse;
import com.review.dto.TenantAdminStatusUpdateRequest;
import com.review.dto.TenantAdminUpdateRequest;
import com.review.service.TenantAdminService;
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

import java.util.Arrays;
import java.util.List;

/**
 * 平台租户管理员管理接口
 */
@Api(tags = "租户管理员管理")
@RestController
@RequestMapping("/tenant/{tenantId}/admins")
public class TenantAdminController {

    @Autowired
    private TenantAdminService tenantAdminService;

    private void ensurePlatformAdmin() {
        String roles = UserContext.getCurrentRoles();
        boolean hasRole = roles != null && Arrays.stream(roles.split(","))
                .anyMatch(role -> "PLATFORM_ADMIN".equals(role));
        if (!hasRole) {
            throw new BusinessException("当前账号无权执行该操作");
        }
    }

    @ApiOperation("查询租户管理员列表")
    @GetMapping
    public Result<List<TenantAdminResponse>> listAdmins(@PathVariable Long tenantId) {
        ensurePlatformAdmin();
        return Result.success(tenantAdminService.listAdmins(tenantId));
    }

    @ApiOperation("新增租户管理员")
    @PostMapping
    public Result<Void> createAdmin(@PathVariable Long tenantId,
                                    @Validated @RequestBody TenantAdminCreateRequest request) {
        ensurePlatformAdmin();
        tenantAdminService.createAdmin(tenantId, request);
        return Result.success();
    }

    @ApiOperation("编辑租户管理员")
    @PutMapping("/{adminId}")
    public Result<Void> updateAdmin(@PathVariable Long tenantId,
                                    @PathVariable Long adminId,
                                    @Validated @RequestBody TenantAdminUpdateRequest request) {
        ensurePlatformAdmin();
        tenantAdminService.updateAdmin(tenantId, adminId, request);
        return Result.success();
    }

    @ApiOperation("重置租户管理员密码")
    @PostMapping("/{adminId}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long tenantId,
                                      @PathVariable Long adminId,
                                      @Validated @RequestBody TenantAdminResetPasswordRequest request) {
        ensurePlatformAdmin();
        tenantAdminService.resetPassword(tenantId, adminId, request);
        return Result.success();
    }

    @ApiOperation("启用/停用租户管理员")
    @PutMapping("/{adminId}/status")
    public Result<Void> updateStatus(@PathVariable Long tenantId,
                                     @PathVariable Long adminId,
                                     @Validated @RequestBody TenantAdminStatusUpdateRequest request) {
        ensurePlatformAdmin();
        tenantAdminService.updateStatus(tenantId, adminId, request);
        return Result.success();
    }
}
