package com.review.utils;

import com.review.common.BusinessException;
import com.review.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 工具类，封装上传、下载、删除、预签名 URL 等能力。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private static final int DEFAULT_URL_EXPIRE_SECONDS = (int) TimeUnit.HOURS.toSeconds(2);

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 启动时初始化存储桶，避免业务端重复判断。
     */
    @PostConstruct
    public void initBucket() {
        String bucketName = minioProperties.getBucketName();
        if (!StringUtils.hasText(bucketName)) {
            throw new IllegalStateException("MinIO 配置缺少 bucket-name，请检查 application.yml");
        }
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO 存储桶 {} 不存在，已自动创建", bucketName);
            } else {
                log.info("MinIO 存储桶 {} 已存在，跳过创建", bucketName);
            }
        } catch (Exception e) {
            log.error("初始化 MinIO 存储桶 {} 失败", bucketName, e);
            throw new IllegalStateException("MinIO 存储桶初始化失败", e);
        }
    }

    /**
     * 上传文件到默认桶。
     */
    public void upload(String objectName, InputStream inputStream, long size, String contentType) {
        try {
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectName)
                    .stream(inputStream, size, -1);
            if (StringUtils.hasText(contentType)) {
                builder.contentType(contentType);
            }
            minioClient.putObject(builder.build());
            log.info("MinIO 上传完成，objectName={}", objectName);
        } catch (Exception e) {
            log.error("上传文件到 MinIO 失败，objectName={}", objectName, e);
            throw new BusinessException("文件上传失败，请稍后重试");
        }
    }

    /**
     * 下载。
     */
    public InputStream download(String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error("下载 MinIO 文件失败，objectName={}", objectName, e);
            throw new BusinessException("文件下载失败，请稍后重试");
        }
    }

    /**
     * 生成对象的预签名 GET URL，默认有效期 2 小时。
     * - 若配置了 publicEndpoint，则使用该外部地址做签名，保证浏览器可访问且 Host 与签名一致。
     * - 否则使用内部 endpoint（minioClient）做签名。
     *
     * @param objectName MinIO 对象名（含路径）
     * @param expireSec  过期秒数，空或 <=0 则取默认值
     * @return 预签名可直链下载的 URL
     */
    public String generatePresignedUrl(String objectName, Integer expireSec) {
        try {
            // 1) 计算有效期，默认 2 小时
            int expire = expireSec == null || expireSec <= 0 ? DEFAULT_URL_EXPIRE_SECONDS : expireSec;

            // 2) 读取对外地址（可为空）
            String publicEndpoint = minioProperties.getPublicEndpoint();

            // 3) 选择用于签名的 MinioClient：优先用外部可访问的 endpoint，保证 Host 一致
            MinioClient signer = minioClient;
            if (StringUtils.hasText(publicEndpoint)) {
                signer = MinioClient.builder()
                        .endpoint(publicEndpoint) // 外部可访问地址
                        .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                        .build();
            }

            // 4) 调用预签名接口生成 URL
            return signer.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)                       // 使用 GET 方式
                    .bucket(minioProperties.getBucketName())  // 桶名
                    .object(objectName)                       // 对象名
                    .expiry(expire)                           // 过期时间
                    .build());
        } catch (Exception e) {
            // 5) 失败时记录并抛业务异常
            log.error("生成 MinIO 预签名 URL 失败，objectName={}", objectName, e);
            throw new BusinessException("生成文件链接失败，请稍后重试");
        }
    }

    /**
     * 删除对象。
     */
    public void remove(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectName)
                    .build());
            log.info("删除 MinIO 对象成功，objectName={}", objectName);
        } catch (Exception e) {
            log.error("删除 MinIO 对象失败，objectName={}", objectName, e);
            throw new BusinessException("删除文件失败，请稍后重试");
        }
    }

    /**
     * 列出全部存储桶，便于排查。
     */
    public List<Bucket> listBuckets() {
        try {
            return minioClient.listBuckets();
        } catch (Exception e) {
            log.error("查询 MinIO 存储桶列表失败", e);
            throw new BusinessException("查询存储桶信息失败");
        }
    }
}
