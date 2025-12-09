package com.review.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置，仅负责创建 MinioClient Bean，便于统一注入。
 */
@Configuration
public class MinioConfig {

    /**
     * 构建 MinIO 官方提供的 MinioClient。
     *
     * @param properties MinIO 配置参数
     * @return MinioClient Bean
     */
    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
}
