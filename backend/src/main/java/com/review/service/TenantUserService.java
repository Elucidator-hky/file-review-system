package com.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.review.common.BusinessException;
import com.review.dto.UserCreateRequest;
import com.review.dto.UserQueryRequest;
import com.review.dto.UserResetPasswordRequest;
import com.review.dto.UserResponse;
import com.review.dto.UserStatusBatchRequest;
import com.review.dto.UserStatusUpdateRequest;
import com.review.dto.UserUpdateRequest;
import com.review.entity.Tenant;
import com.review.entity.User;
import com.review.mapper.TenantMapper;
import com.review.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 企业用户管理
 */
@Service
public class TenantUserService {

    private static final Set<String> ALLOWED_ROLES = new HashSet<>(Arrays.asList("TENANT_ADMIN", "REVIEWER", "USER"));

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TenantMapper tenantMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Page<UserResponse> queryUsers(Long tenantId, UserQueryRequest request) {
        if (tenantId == null) {
            throw new BusinessException("无权查询其他租户数据");
        }
        int pageNo = request.getPageNo() == null ? 1 : request.getPageNo();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getTenantId, tenantId);
        if (request.getStatus() != null) {
            wrapper.eq(User::getStatus, request.getStatus());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(q -> q.like(User::getUsername, request.getKeyword())
                    .or().like(User::getRealName, request.getKeyword())
                    .or().like(User::getPhone, request.getKeyword()));
        }
        if (!CollectionUtils.isEmpty(request.getRoles())) {
            wrapper.and(q -> {
                for (String role : request.getRoles()) {
                    String roleUpper = role.trim().toUpperCase();
                    q.or().like(User::getRoles, roleUpper);
                }
            });
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> page = userMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<UserResponse> records = page.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        Page<UserResponse> responsePage = new Page<>(pageNo, pageSize, page.getTotal());
        responsePage.setRecords(records);
        return responsePage;
    }

    @Transactional(rollbackFor = Exception.class)
    public void createUser(Long tenantId, UserCreateRequest request) {
        Tenant tenant = ensureTenantAvailable(tenantId);
        validateRoles(request.getRoles());
        ensureUsernameUnique(tenantId, request.getUsername());
        ensurePhoneUnique(tenantId, request.getPhone(), null);
        ensureQuotaAvailable(tenantId, tenant);

        User user = new User();
        user.setTenantId(tenantId);
        user.setUsername(request.getUsername());
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(joinRoles(request.getRoles()));
        user.setStatus(1);
        userMapper.insert(user);

        tenant.setUserCount((tenant.getUserCount() == null ? 0 : tenant.getUserCount()) + 1);
        tenantMapper.updateById(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long tenantId, Long userId, UserUpdateRequest request) {
        validateRoles(request.getRoles());
        User user = loadTenantUser(tenantId, userId);
        ensurePhoneUnique(tenantId, request.getPhone(), userId);
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setRoles(joinRoles(request.getRoles()));
        userMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long tenantId, Long userId, UserResetPasswordRequest request) {
        User user = loadTenantUser(tenantId, userId);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long tenantId, Long userId, UserStatusUpdateRequest request) {
        User user = loadTenantUser(tenantId, userId);
        if (user.getStatus() != null && user.getStatus().equals(request.getStatus())) {
            return;
        }
        if (request.getStatus() == 1) {
            Tenant tenant = ensureTenantAvailable(tenantId);
            ensureActiveCountWithinQuota(tenantId, tenant);
        }
        user.setStatus(request.getStatus());
        userMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStatus(Long tenantId, UserStatusBatchRequest request) {
        for (Long userId : request.getUserIds()) {
            updateStatus(tenantId, userId, toStatusRequest(request.getStatus()));
        }
    }

    private UserStatusUpdateRequest toStatusRequest(Integer status) {
        UserStatusUpdateRequest request = new UserStatusUpdateRequest();
        request.setStatus(status);
        return request;
    }

    private Tenant ensureTenantAvailable(Long tenantId) {
        if (tenantId == null) {
            throw new BusinessException("租户不存在");
        }
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }
        if (tenant.getStatus() != null && tenant.getStatus() == 0) {
            throw new BusinessException("租户已停用，无法操作用户");
        }
        return tenant;
    }

    private void ensureQuotaAvailable(Long tenantId, Tenant tenant) {
        Integer quota = tenant.getUserQuota();
        Integer used = tenant.getUserCount();
        if (quota != null && used != null && used >= quota) {
            throw new BusinessException("用户配额不足，请先调整配额");
        }
    }

    private void ensureActiveCountWithinQuota(Long tenantId, Tenant tenant) {
        Integer quota = tenant.getUserQuota();
        if (quota == null) {
            return;
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getTenantId, tenantId)
                .eq(User::getStatus, 1);
        Long activeCount = userMapper.selectCount(wrapper);
        if (activeCount >= quota) {
            throw new BusinessException("启用失败：已达到用户配额上限");
        }
    }

    private void ensureUsernameUnique(Long tenantId, String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getTenantId, tenantId)
                .eq(User::getUsername, username);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该用户名已存在");
        }
    }

    private void ensurePhoneUnique(Long tenantId, String phone, Long excludeUserId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getTenantId, tenantId)
                .eq(User::getPhone, phone);
        if (excludeUserId != null) {
            wrapper.ne(User::getId, excludeUserId);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该手机号已被占用");
        }
    }

    private User loadTenantUser(Long tenantId, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !tenantId.equals(user.getTenantId())) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private void validateRoles(List<String> roles) {
        if (CollectionUtils.isEmpty(roles)) {
            throw new BusinessException("至少选择一个角色");
        }
        Set<String> normalized = roles.stream()
                .map(role -> role == null ? "" : role.trim().toUpperCase())
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (normalized.isEmpty()) {
            throw new BusinessException("至少选择一个角色");
        }
        if (!ALLOWED_ROLES.containsAll(normalized)) {
            throw new BusinessException("存在不支持的角色");
        }
        if (normalized.contains("TENANT_ADMIN") && normalized.size() > 1) {
            throw new BusinessException("企业管理员角色不能与其他角色同时存在");
        }
    }

    private String joinRoles(List<String> roles) {
        return roles.stream()
                .map(role -> role == null ? "" : role.trim().toUpperCase())
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        BeanUtils.copyProperties(user, response);
        response.setRoles(splitRoles(user.getRoles()));
        return response;
    }

    private List<String> splitRoles(String roles) {
        if (!StringUtils.hasText(roles)) {
            return Collections.emptyList();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }
}
