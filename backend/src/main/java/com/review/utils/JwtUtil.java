package com.review.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 *
 * 作用：用于生成、解析和验证JWT Token
 *
 * JWT（JSON Web Token）是一种无状态的身份认证方式：
 * 1. 用户登录成功后，服务器生成一个Token返回给前端
 * 2. 前端每次请求时，在Header中携带这个Token
 * 3. 服务器验证Token，确认用户身份
 *
 * JWT的优点：
 * - 无状态：服务器不需要存储Session，减轻服务器压力
 * - 分布式友好：适合微服务架构
 * - 包含用户信息：Token中包含用户ID、角色等信息，不需要查数据库
 *
 * @author Claude
 * @date 2025-11-28
 */
@Component  // 标记为Spring组件，可以被自动注入
public class JwtUtil {

    /**
     * JWT密钥
     * 从配置文件中读取：jwt.secret
     * 作用：用于签名Token，确保Token不被篡改
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Token过期时间（毫秒）
     * 从配置文件中读取：jwt.expiration
     * 默认24小时（86400000毫秒）
     */
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 生成密钥对象
     *
     * 作用：将配置文件中的字符串密钥转换为SecretKey对象
     * 为什么需要：JWT库需要SecretKey类型才能进行签名
     *
     * @return SecretKey对象
     */
    private SecretKey getSecretKey() {
        // 将字符串密钥转换为字节数组，使用UTF-8编码
        // 然后通过HMAC-SHA算法生成密钥对象
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成JWT Token
     *
     * 使用场景：用户登录成功后调用此方法生成Token
     *
     * Token包含的信息：
     * - userId：用户ID，用于后续查询用户信息
     * - username：用户名，方便日志记录
     * - tenantId：租户ID，实现多租户数据隔离
     * - roles：角色列表，用于权限控制
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param tenantId 租户ID
     * @param roles 角色列表（逗号分隔，如："TENANT_ADMIN,USER"）
     * @return 生成的JWT Token字符串
     */
    public String generateToken(Long userId, String username, Long tenantId, String roles) {
        // 1. 创建载荷（Payload）：存储用户信息
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);          // 用户ID
        claims.put("username", username);      // 用户名
        claims.put("tenantId", tenantId);      // 租户ID
        claims.put("roles", roles);            // 角色

        // 2. 设置时间
        Date now = new Date();                                    // 当前时间
        Date expirationDate = new Date(now.getTime() + expiration);  // 过期时间 = 当前时间 + 有效期

        // 3. 构建JWT Token
        return Jwts.builder()
                .setClaims(claims)              // 设置载荷（用户信息）
                .setSubject(username)           // 设置主题（通常是用户名）
                .setIssuedAt(now)               // 设置签发时间
                .setExpiration(expirationDate)  // 设置过期时间
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)  // 使用HS256算法签名
                .compact();                     // 生成最终的Token字符串
    }

    /**
     * 从Token中获取Claims（载荷）
     *
     * Claims是JWT的核心部分，包含了我们存储的所有信息
     *
     * 解析过程：
     * 1. 使用密钥验证Token的签名是否正确
     * 2. 如果签名正确，解析出Claims
     * 3. 如果签名错误或Token格式不对，会抛出异常
     *
     * @param token JWT Token字符串
     * @return Claims对象，包含用户信息
     * @throws io.jsonwebtoken.JwtException 如果Token无效或过期
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()          // 创建解析器构建器
                .setSigningKey(getSecretKey())  // 设置密钥（用于验证签名）
                .build()                        // 构建解析器
                .parseClaimsJws(token)          // 解析Token（会自动验证签名）
                .getBody();                     // 获取Claims（载荷部分）
    }

    /**
     * 从Token中获取用户ID
     *
     * 使用场景：
     * - 拦截器解析Token后，获取当前登录用户ID
     * - Controller中需要知道是哪个用户在操作
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);  // 先解析Token获取Claims
        return claims.get("userId", Long.class);    // 从Claims中获取userId字段
    }

    /**
     * 从Token中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();  // Subject字段存储的就是username
    }

    /**
     * 从Token中获取租户ID
     *
     * 使用场景：多租户数据隔离
     * 查询数据时，需要加上 WHERE tenant_id = ? 条件
     *
     * @param token JWT Token
     * @return 租户ID
     */
    public Long getTenantIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("tenantId", Long.class);
    }

    /**
     * 从Token中获取角色列表
     *
     * 使用场景：权限控制
     * 判断用户是否有权限访问某个接口
     *
     * @param token JWT Token
     * @return 角色列表（逗号分隔，如："TENANT_ADMIN,USER"）
     */
    public String getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("roles", String.class);
    }

    /**
     * 验证Token是否过期
     *
     * 判断逻辑：
     * Token中有一个过期时间（expiration），如果这个时间早于当前时间，说明已过期
     *
     * @param token JWT Token
     * @return true-已过期，false-未过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);  // 解析Token
            Date expiration = claims.getExpiration();   // 获取过期时间
            return expiration.before(new Date());       // 判断过期时间是否早于当前时间
        } catch (Exception e) {
            // 如果解析失败（Token格式错误、签名错误等），也视为过期
            return true;
        }
    }

    /**
     * 验证Token是否有效
     *
     * 验证步骤：
     * 1. Token格式是否正确
     * 2. Token签名是否正确（是否被篡改）
     * 3. Token是否过期
     *
     * 使用场景：
     * 拦截器中验证用户请求的Token是否有效
     *
     * @param token JWT Token
     * @return true-有效，false-无效
     */
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);      // 尝试解析Token（会自动验证格式和签名）
            return !isTokenExpired(token);  // 如果解析成功，再检查是否过期
        } catch (Exception e) {
            // 如果解析失败，说明Token无效
            return false;
        }
    }
}
