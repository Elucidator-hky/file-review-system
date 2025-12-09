package com.review.config;

import com.review.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.time.Duration;

/**
 * Spring MVC配置
 *
 * @author Claude
 * @date 2025-11-28
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private CurrentUserArgumentResolver currentUserArgumentResolver;

    @Autowired
    private JwtInterceptor jwtInterceptor;

    /**
     * 添加自定义参数解析器
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    /**
     * 添加拦截器
     * 配置JWT拦截器，拦截所有请求（除了登录等白名单接口）
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")  // 拦截所有请求
                .excludePathPatterns(
                        "/auth/login",              // 登录接口
                        "/auth/register",           // 注册接口（如果有）
                        "/doc.html",                // Knife4j文档页面
                        "/swagger-resources/**",    // Swagger资源
                        "/v2/api-docs/**",          // API文档
                        "/webjars/**",              // 静态资源
                        "/favicon.ico",             // 网站图标
                        "/error"                    // 错误页面
                );
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }
}
