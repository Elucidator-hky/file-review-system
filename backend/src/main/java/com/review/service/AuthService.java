package com.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.common.BusinessException;
import com.review.dto.LoginRequest;
import com.review.dto.LoginResponse;
import com.review.entity.User;
import com.review.mapper.UserMapper;
import com.review.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 认证相关业务
 */
@Service
public class AuthService {

    private static final String LOGIN_FAIL_PREFIX = "login:fail:";
    private static final int MAX_FAIL_COUNT = 5;
    private static final Duration FAIL_WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(30);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 登录并返回JWT
     */
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        checkLoginLock(username);

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            recordLoginFailure(username);
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被停用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordLoginFailure(username);
            throw new BusinessException("用户名或密码错误");
        }

        clearLoginFailure(username);

        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getTenantId(),
                user.getRoles()
        );

        return new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getTenantId(),
                user.getRoles()
        );
    }

    private void checkLoginLock(String username) {
        String key = LOGIN_FAIL_PREFIX + username;
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            return;
        }
        long count = Long.parseLong(value);
        if (count < MAX_FAIL_COUNT) {
            return;
        }
        Long expire = stringRedisTemplate.getExpire(key);
        long minutes = expire == null || expire <= 0 ? LOCK_DURATION.toMinutes() : Math.max(1, expire / 60);
        throw new BusinessException("密码错误次数过多，请在 " + minutes + " 分钟后再试");
    }

    private void recordLoginFailure(String username) {
        String key = LOGIN_FAIL_PREFIX + username;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null) {
            if (count == 1) {
                stringRedisTemplate.expire(key, FAIL_WINDOW);
            }
            if (count >= MAX_FAIL_COUNT) {
                stringRedisTemplate.expire(key, LOCK_DURATION);
            }
        }
    }

    private void clearLoginFailure(String username) {
        stringRedisTemplate.delete(LOGIN_FAIL_PREFIX + username);
    }
}
