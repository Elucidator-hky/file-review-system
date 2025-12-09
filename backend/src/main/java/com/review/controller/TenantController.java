package com.review.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.review.common.BusinessException;
import com.review.common.Result;
import com.review.common.UserContext;
import com.review.dto.TenantCreateRequest;
import com.review.dto.TenantQueryRequest;
import com.review.dto.TenantResponse;
import com.review.dto.TenantStatusUpdateRequest;
import com.review.dto.TenantUpdateRequest;
import com.review.service.TenantService;
import com.review.utils.RoleUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户管理控制器
 */
@Api(tags = "租户管理")
@RestController
@RequestMapping("/tenant")
public class TenantController {

    @Autowired
    private TenantService tenantService;

    private boolean isPlatformAdmin() {
        return RoleUtil.hasRole(UserContext.getCurrentRoles(), "PLATFORM_ADMIN");
    }

    private boolean isTenantAdmin(Long tenantId) {
        return RoleUtil.hasRole(UserContext.getCurrentRoles(), "TENANT_ADMIN")
                && tenantId != null
                && tenantId.equals(UserContext.getCurrentTenantId());
    }

    private void ensurePlatformAdmin() {
        if (!isPlatformAdmin()) {
            throw new BusinessException("当前账号无权执行该操作");
        }
    }

    private void ensureTenantReadable(Long tenantId) {
        if (isPlatformAdmin() || isTenantAdmin(tenantId)) {
            return;
        }
        throw new BusinessException("当前账号无权查看该租户信息");
    }

    @ApiOperation("创建租户")
    @PostMapping
    public Result<Long> createTenant(@Validated @RequestBody TenantCreateRequest request) {
        ensurePlatformAdmin();
        return Result.success(tenantService.createTenant(request));
    }

    @ApiOperation("分页查询租户")
    @GetMapping("/list")
    public Result<Page<TenantResponse>> listTenants(@Validated TenantQueryRequest request) {
        ensurePlatformAdmin();
        return Result.success(tenantService.queryTenantPage(request));
    }

    @ApiOperation("查询租户详情")
    @GetMapping("/{tenantId}")
    public Result<TenantResponse> getTenant(@PathVariable Long tenantId) {
        ensureTenantReadable(tenantId);
        return Result.success(tenantService.getTenantDetail(tenantId));
    }

    @ApiOperation("更新租户基础信息与配额")
    @PutMapping("/{tenantId}")
    public Result<Void> updateTenant(@PathVariable Long tenantId,
                                     @Validated @RequestBody TenantUpdateRequest request) {
        ensurePlatformAdmin();
        tenantService.updateTenant(tenantId, request);
        return Result.success();
    }

    @ApiOperation("切换租户状态（启用/停用）")
    @PutMapping("/{tenantId}/status")
    public Result<Void> updateStatus(@PathVariable Long tenantId,
                                     @Validated @RequestBody TenantStatusUpdateRequest request) {
        ensurePlatformAdmin();
        tenantService.updateTenantStatus(tenantId, request);
        return Result.success();
    }

    @ApiOperation("删除租户")
    @DeleteMapping("/{tenantId}")
    public Result<Void> deleteTenant(@PathVariable Long tenantId) {
        ensurePlatformAdmin();
        tenantService.deleteTenant(tenantId);
        return Result.success();
    }
}
