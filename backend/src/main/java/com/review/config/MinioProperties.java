package com.review.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO 连接参数配置，通过 application.yml 统一注入。
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * MinIO 访问地址，如：http://localhost:9000
     */
    private String endpoint;

    /**
     * 访问用 Access Key。
     */
    private String accessKey;

    /**
     * 访问用 Secret Key。
     */
    private String secretKey;

    /**
     * 默认存储桶名称。
     */
    private String bucketName;

    /**
     * （可选）生成预签名链接时对外暴露的访问地址，如：http://your.domain:9000。
     * 留空则使用 endpoint 原值。
     */
    private String publicEndpoint;
}
