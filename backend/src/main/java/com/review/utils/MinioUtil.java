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
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 工具类，封装上传、下载、删除、预签名 URL 等常用能力。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private static final int DEFAULT_URL_EXPIRE_SECONDS = (int) TimeUnit.HOURS.toSeconds(2);

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 启动时自动初始化存储桶，避免业务端重复判断。
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
     * 上传文件到默认桶，未来文件模块可以直接复用。
     *
     * @param objectName  对象名称（包含路径）
     * @param inputStream 文件流
     * @param size        文件大小
     * @param contentType MIME 类型
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
     * 获取对象输入流，调用方记得关闭流。
     *
     * @param objectName 对象名称
     * @return MinIO 对象流
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
     * 根据对象名称生成预签名 URL，默认 2 小时有效。
     *
     * @param objectName 对象名称
     * @param expireSec  有效期（秒），可为空
     * @return 预签名地址
     */
    public String generatePresignedUrl(String objectName, Integer expireSec) {
        try {
            int expire = expireSec == null || expireSec <= 0 ? DEFAULT_URL_EXPIRE_SECONDS : expireSec;
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioProperties.getBucketName())
                    .object(objectName)
                    .expiry(expire)
                    .build());
        } catch (Exception e) {
            log.error("生成 MinIO 预签名 URL 失败，objectName={}", objectName, e);
            throw new BusinessException("生成文件链接失败，请稍后重试");
        }
    }

    /**
     * 删除已经上传的对象。
     *
     * @param objectName 对象名称
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
     * 查询当前集群全部桶信息，便于排障。
     *
     * @return Bucket 列表
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
