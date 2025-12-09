package com.review.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO 连接参数，全部来源于 application.yml，统一通过 UTF-8 保存。
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * MinIO 访问地址，例如：http://localhost:9000
     */
    private String endpoint;

    /**
     * 鉴权用 Access Key。
     */
    private String accessKey;

    /**
     * 鉴权用 Secret Key。
     */
    private String secretKey;

    /**
     * 默认存储桶名称。
     */
    private String bucketName;
}
