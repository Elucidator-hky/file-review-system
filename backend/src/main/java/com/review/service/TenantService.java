package com.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.review.common.BusinessException;
import com.review.dto.TenantCreateRequest;
import com.review.dto.TenantQueryRequest;
import com.review.dto.TenantResponse;
import com.review.dto.TenantStatusUpdateRequest;
import com.review.dto.TenantUpdateRequest;
import com.review.entity.Tenant;
import com.review.mapper.TenantMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 租户业务
 */
@Service
public class TenantService {

    private static final long DEFAULT_STORAGE_QUOTA = 107_374_182_400L; // 默认100GB
    private static final int DEFAULT_USER_QUOTA = 50;

    @Autowired
    private TenantMapper tenantMapper;

    /**
     * 创建租户
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createTenant(TenantCreateRequest request) {
        LambdaQueryWrapper<Tenant> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tenant::getTenantName, request.getTenantName());
        Tenant existing = tenantMapper.selectOne(queryWrapper);
        if (existing != null) {
            throw new BusinessException("租户名称已存在");
        }

        Tenant tenant = new Tenant();
        tenant.setTenantName(request.getTenantName());
        tenant.setContactName(request.getContactName());
        tenant.setContactPhone(request.getContactPhone());
        tenant.setStorageQuota(
                request.getStorageQuota() != null ? request.getStorageQuota() : DEFAULT_STORAGE_QUOTA);
        tenant.setStorageUsed(0L);
        tenant.setUserQuota(
                request.getUserQuota() != null ? request.getUserQuota() : DEFAULT_USER_QUOTA);
        tenant.setUserCount(0);
        tenant.setStatus(1);

        tenantMapper.insert(tenant);
        return tenant.getId();
    }

    /**
     * 更新租户联系人与配额
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTenant(Long tenantId, TenantUpdateRequest request) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }
        ensureNotPlatformTenant(tenantId);

        if (request.getStorageQuota() < tenant.getStorageUsed()) {
            throw new BusinessException("存储配额不能小于已使用容量");
        }
        if (request.getUserQuota() < tenant.getUserCount()) {
            throw new BusinessException("用户配额不能小于当前已创建数量");
        }

        tenant.setContactName(request.getContactName());
        tenant.setContactPhone(request.getContactPhone());
        tenant.setStorageQuota(request.getStorageQuota());
        tenant.setUserQuota(request.getUserQuota());

        tenantMapper.updateById(tenant);
    }

    /**
     * 查询租户详情
     */
    public TenantResponse getTenantDetail(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }
        return toResponse(tenant);
    }

    /**
     * 切换租户状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTenantStatus(Long tenantId, TenantStatusUpdateRequest request) {
        ensureNotPlatformTenant(tenantId);
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }
        tenant.setStatus(request.getStatus());
        tenantMapper.updateById(tenant);
    }

    /**
     * 删除租户
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTenant(Long tenantId) {
        ensureNotPlatformTenant(tenantId);
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }
        if (tenant.getUserCount() != null && tenant.getUserCount() > 0) {
            throw new BusinessException("请先删除租户下的用户/管理员后再操作");
        }
        tenantMapper.deleteById(tenantId);
    }

    /**
     * 分页查询租户
     */
    public Page<TenantResponse> queryTenantPage(TenantQueryRequest request) {
        int pageNo = request.getPageNo() == null ? 1 : request.getPageNo();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
        Page<Tenant> page = new Page<>(pageNo, pageSize);

        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(Tenant::getTenantName, request.getKeyword());
        }
        if (request.getStatus() != null) {
            wrapper.eq(Tenant::getStatus, request.getStatus());
        }
        wrapper.orderByDesc(Tenant::getCreateTime);

        Page<Tenant> tenantPage = tenantMapper.selectPage(page, wrapper);
        List<TenantResponse> records = tenantPage.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        Page<TenantResponse> result = new Page<>(pageNo, pageSize, tenantPage.getTotal());
        result.setRecords(records);
        return result;
    }

    private void ensureNotPlatformTenant(Long tenantId) {
        if (tenantId != null && Objects.equals(tenantId, 0L)) {
            throw new BusinessException("平台系统租户禁止修改");
        }
    }

    private TenantResponse toResponse(Tenant tenant) {
        TenantResponse response = new TenantResponse();
        BeanUtils.copyProperties(tenant, response);
        return response;
    }
}
