package com.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.common.BusinessException;
import com.review.dto.TenantAdminCreateRequest;
import com.review.dto.TenantAdminResetPasswordRequest;
import com.review.dto.TenantAdminResponse;
import com.review.dto.TenantAdminStatusUpdateRequest;
import com.review.dto.TenantAdminUpdateRequest;
import com.review.entity.Tenant;
import com.review.entity.User;
import com.review.mapper.TenantMapper;
import com.review.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 平台侧租户管理员管理业务
 */
@Service
public class TenantAdminService {

    @Autowired
    private TenantMapper tenantMapper;

    @Autowired
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 查询租户管理员列表
     */
    public List<TenantAdminResponse> listAdmins(Long tenantId) {
        Tenant tenant = loadTenant(tenantId);
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getTenantId, tenantId)
                .like(User::getRoles, "TENANT_ADMIN")
                .orderByDesc(User::getCreateTime);
        return userMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 创建租户管理员
     */
    @Transactional(rollbackFor = Exception.class)
    public void createAdmin(Long tenantId, TenantAdminCreateRequest request) {
        Tenant tenant = loadTenant(tenantId);
        ensureTenantAvailable(tenant);

        // 校验用户名唯一
        LambdaQueryWrapper<User> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(User::getTenantId, tenantId)
                .eq(User::getUsername, request.getUsername());
        if (userMapper.selectOne(usernameWrapper) != null) {
            throw new BusinessException("该租户已存在相同用户名");
        }

        // 校验手机号唯一
        LambdaQueryWrapper<User> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(User::getTenantId, tenantId)
                .eq(User::getPhone, request.getPhone());
        if (userMapper.selectOne(phoneWrapper) != null) {
            throw new BusinessException("该手机号已被占用");
        }

        // 校验配额
        int currentCount = tenant.getUserCount() == null ? 0 : tenant.getUserCount();
        if (tenant.getUserQuota() != null && currentCount >= tenant.getUserQuota()) {
            throw new BusinessException("用户配额不足，请先调整配额");
        }

        User user = new User();
        user.setTenantId(tenantId);
        user.setUsername(request.getUsername());
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles("TENANT_ADMIN");
        user.setStatus(1);
        userMapper.insert(user);

        tenant.setUserCount(currentCount + 1);
        tenantMapper.updateById(tenant);
    }

    /**
     * 更新管理员信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateAdmin(Long tenantId, Long adminId, TenantAdminUpdateRequest request) {
        Tenant tenant = loadTenant(tenantId);
        ensureTenantAvailable(tenant);
        User admin = loadAdmin(tenantId, adminId);

        // 手机号唯一校验
        LambdaQueryWrapper<User> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(User::getTenantId, tenantId)
                .eq(User::getPhone, request.getPhone())
                .ne(User::getId, adminId);
        if (userMapper.selectOne(phoneWrapper) != null) {
            throw new BusinessException("该手机号已被占用");
        }

        admin.setRealName(request.getRealName());
        admin.setPhone(request.getPhone());
        userMapper.updateById(admin);
    }

    /**
     * 重置管理员密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long tenantId, Long adminId, TenantAdminResetPasswordRequest request) {
        Tenant tenant = loadTenant(tenantId);
        ensureTenantAvailable(tenant);
        User admin = loadAdmin(tenantId, adminId);
        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(admin);
    }

    /**
     * 启用/停用管理员
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long tenantId, Long adminId, TenantAdminStatusUpdateRequest request) {
        Tenant tenant = loadTenant(tenantId);
        ensureTenantAvailable(tenant);
        User admin = loadAdmin(tenantId, adminId);
        admin.setStatus(request.getStatus());
        userMapper.updateById(admin);
    }

    private Tenant loadTenant(Long tenantId) {
        if (tenantId == null || Objects.equals(tenantId, 0L)) {
            return null;
        }
        return tenantMapper.selectById(tenantId);
    }

    private void ensureTenantAvailable(Tenant tenant) {
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }
        if (Objects.equals(tenant.getId(), 0L)) {
            throw new BusinessException("系统租户不支持此操作");
        }
        if (tenant.getStatus() != null && tenant.getStatus() == 0) {
            throw new BusinessException("租户已停用，无法操作管理员");
        }
    }

    private User loadAdmin(Long tenantId, Long adminId) {
        User admin = userMapper.selectById(adminId);
        if (admin == null || !Objects.equals(admin.getTenantId(), tenantId) || !hasTenantAdminRole(admin)) {
            throw new BusinessException("管理员不存在");
        }
        return admin;
    }

    private boolean hasTenantAdminRole(User admin) {
        if (!StringUtils.hasText(admin.getRoles())) {
            return false;
        }
        return Arrays.stream(admin.getRoles().split(","))
                .anyMatch(role -> "TENANT_ADMIN".equals(role));
    }

    private TenantAdminResponse toResponse(User admin) {
        TenantAdminResponse response = new TenantAdminResponse();
        response.setId(admin.getId());
        response.setUsername(admin.getUsername());
        response.setRealName(admin.getRealName());
        response.setPhone(admin.getPhone());
        response.setStatus(admin.getStatus());
        response.setCreateTime(admin.getCreateTime());
        response.setLastLoginTime(admin.getLastLoginTime());
        return response;
    }
}
