package com.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.review.common.BusinessException;
import com.review.common.UserContext;
import com.review.dto.FileExistsResponse;
import com.review.dto.FileInfoResponse;
import com.review.dto.FileUploadResponse;
import com.review.entity.ReviewFile;
import com.review.entity.ReviewVersion;
import com.review.entity.Tenant;
import com.review.mapper.ReviewFileMapper;
import com.review.mapper.ReviewVersionMapper;
import com.review.mapper.TenantMapper;
import com.review.utils.MinioUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文件上传与管理服务。
 */
@Service
public class FileService {

    private static final long MAX_FILE_SIZE = 200L * 1024 * 1024;
    private static final String ALLOWED_TYPE = "application/pdf";
    private static final long PREVIEW_EXPIRE = TimeUnit.DAYS.toSeconds(7);

    @Autowired
    private ReviewFileMapper reviewFileMapper;

    @Autowired
    private ReviewVersionMapper reviewVersionMapper;

    @Autowired
    private TenantMapper tenantMapper;

    @Autowired
    private MinioUtil minioUtil;

    /**
     * 检查 MD5 是否已存在。
     */
    public FileExistsResponse checkFileExists(String md5) {
        if (!StringUtils.hasText(md5)) {
            throw new BusinessException("MD5 参数不能为空");
        }
        Long tenantId = currentTenantId();
        ReviewFile file = reviewFileMapper.selectOne(new LambdaQueryWrapper<ReviewFile>()
                .eq(ReviewFile::getTenantId, tenantId)
                .eq(ReviewFile::getFileMd5, md5)
                .last("LIMIT 1"));
        if (file == null) {
            return new FileExistsResponse(false, null);
        }
        return new FileExistsResponse(true, file.getMinioObjectName());
    }

    /**
     * 上传或秒传文件。
     */
    public FileUploadResponse uploadFile(MultipartFile file, String md5, Long versionId, String fileName) {
        validateUploadParam(file, md5, versionId, fileName);
        Long tenantId = currentTenantId();
        ReviewVersion version = loadVersion(versionId, tenantId);

        // 检查同版本是否存在同名文件
        Long nameCount = reviewFileMapper.selectCount(new LambdaQueryWrapper<ReviewFile>()
                .eq(ReviewFile::getVersionId, versionId)
                .eq(ReviewFile::getFileName, fileName));
        if (nameCount != null && nameCount > 0) {
            throw new BusinessException("该版本下已存在同名文件");
        }

        ReviewFile existing = reviewFileMapper.selectOne(new LambdaQueryWrapper<ReviewFile>()
                .eq(ReviewFile::getTenantId, tenantId)
                .eq(ReviewFile::getFileMd5, md5)
                .last("LIMIT 1"));

        boolean newUpload = existing == null;
        String objectName = newUpload ? String.format("%d/%s", tenantId, md5) : existing.getMinioObjectName();

        if (newUpload) {
            Tenant tenant = tenantMapper.selectById(tenantId);
            if (tenant == null) {
                throw new BusinessException("租户不存在");
            }
            if (tenant.getStorageUsed() + file.getSize() > tenant.getStorageQuota()) {
                throw new BusinessException("存储空间不足，无法上传");
            }
            try (InputStream inputStream = file.getInputStream()) {
                minioUtil.upload(objectName, inputStream, file.getSize(), file.getContentType());
            } catch (IOException e) {
                throw new BusinessException("文件上传失败，请稍后再试");
            }
            tenantMapper.increaseStorageUsed(tenantId, file.getSize());
        }

        ReviewFile reviewFile = new ReviewFile();
        reviewFile.setTenantId(tenantId);
        reviewFile.setVersionId(versionId);
        reviewFile.setFileName(fileName);
        reviewFile.setMinioObjectName(objectName);
        reviewFile.setFileMd5(md5.toLowerCase());
        reviewFile.setFileSize(file.getSize());
        reviewFile.setFileType(ALLOWED_TYPE);
        reviewFileMapper.insert(reviewFile);

        updateVersionFileCount(version.getId(), 1);

        return new FileUploadResponse(reviewFile.getId(), reviewFile.getFileName(), reviewFile.getFileSize(), newUpload);
    }

    /**
     * 返回指定版本下的文件列表。
     */
    public List<FileInfoResponse> listFiles(Long versionId) {
        if (versionId == null) {
            throw new BusinessException("版本 ID 不能为空");
        }
        Long tenantId = currentTenantId();
        loadVersion(versionId, tenantId);
        return reviewFileMapper.selectList(new LambdaQueryWrapper<ReviewFile>()
                        .eq(ReviewFile::getVersionId, versionId)
                        .orderByDesc(ReviewFile::getCreateTime))
                .stream()
                .map(file -> new FileInfoResponse(
                        file.getId(),
                        file.getFileName(),
                        file.getFileSize(),
                        file.getFileType(),
                        file.getCreateTime()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 获取文件实体并校验租户归属。
     */
    public ReviewFile loadFile(Long fileId) {
        if (fileId == null) {
            throw new BusinessException("文件 ID 不能为空");
        }
        Long tenantId = currentTenantId();
        ReviewFile file = reviewFileMapper.selectById(fileId);
        if (file == null || !tenantId.equals(file.getTenantId())) {
            throw new BusinessException("文件不存在或无权访问");
        }
        return file;
    }

    /**
     * 生成预览链接。
     */
    public String generatePreviewUrl(Long fileId) {
        ReviewFile file = loadFile(fileId);
        return minioUtil.generatePresignedUrl(file.getMinioObjectName(), (int) PREVIEW_EXPIRE);
    }

    /**
     * 删除文件记录，并在必要时删除 MinIO 对象。
     */
    public void deleteFile(Long fileId) {
        ReviewFile file = loadFile(fileId);
        reviewFileMapper.deleteById(fileId);
        updateVersionFileCount(file.getVersionId(), -1);

        Long refCount = reviewFileMapper.selectCount(new LambdaQueryWrapper<ReviewFile>()
                .eq(ReviewFile::getTenantId, file.getTenantId())
                .eq(ReviewFile::getMinioObjectName, file.getMinioObjectName()));
        if (refCount != null && refCount == 0) {
            minioUtil.remove(file.getMinioObjectName());
            tenantMapper.decreaseStorageUsed(file.getTenantId(), file.getFileSize());
        }
    }

    /**
     * 下载文件。
     */
    public Map<String, Object> downloadFile(Long fileId) {
        ReviewFile file = loadFile(fileId);
        InputStream inputStream = minioUtil.download(file.getMinioObjectName());
        String encodedName;
        try {
            encodedName = URLEncoder.encode(file.getFileName(), StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new BusinessException("文件名编码失败，请尝试更换名称");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("stream", inputStream);
        result.put("fileName", file.getFileName());
        result.put("encodedName", encodedName);
        result.put("contentType", file.getFileType());
        result.put("size", file.getFileSize());
        return result;
    }

    private void validateUploadParam(MultipartFile file, String md5, Long versionId, String fileName) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请先选择需要上传的文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPE.equalsIgnoreCase(contentType)) {
            throw new BusinessException("仅支持上传 PDF 文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过200MB");
        }
        if (!StringUtils.hasText(md5) || md5.length() != 32) {
            throw new BusinessException("MD5 参数不正确");
        }
        if (versionId == null) {
            throw new BusinessException("版本 ID 不能为空");
        }
        if (!StringUtils.hasText(fileName)) {
            throw new BusinessException("文件名不能为空");
        }
    }

    private ReviewVersion loadVersion(Long versionId, Long tenantId) {
        ReviewVersion version = reviewVersionMapper.selectById(versionId);
        if (version == null || !tenantId.equals(version.getTenantId())) {
            throw new BusinessException("版本不存在或无权访问");
        }
        return version;
    }

    private void updateVersionFileCount(Long versionId, int delta) {
        LambdaUpdateWrapper<ReviewVersion> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ReviewVersion::getId, versionId)
                .setSql("file_count = GREATEST(COALESCE(file_count,0) + (" + delta + "), 0)");
        reviewVersionMapper.update(null, updateWrapper);
    }

    private Long currentTenantId() {
        Long tenantId = UserContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new BusinessException("当前账号未绑定租户，无法操作文件");
        }
        return tenantId;
    }
}
