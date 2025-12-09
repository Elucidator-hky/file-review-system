package com.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.review.entity.ReviewFile;
import com.review.entity.ReviewVersion;
import com.review.mapper.ReviewFileMapper;
import com.review.mapper.ReviewVersionMapper;
import com.review.mq.FileCopyMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 处理文件异步复制的业务逻辑（消费端调用）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileAsyncService {

    private final ReviewFileMapper reviewFileMapper;
    private final ReviewVersionMapper reviewVersionMapper;

    /**
     * 复制旧版本文件记录到新版本，复制完成后将 filesReady 标记为 1。
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleCopy(FileCopyMessage message) {
        log.info("start copy old files, traceId={}, taskId={}, oldVersion={}, newVersion={}",
                message.getTraceId(), message.getTaskId(), message.getOldVersionId(), message.getNewVersionId());
        List<ReviewFile> oldFiles = reviewFileMapper.selectList(new LambdaQueryWrapper<ReviewFile>()
                .eq(ReviewFile::getVersionId, message.getOldVersionId()));
        if (CollectionUtils.isEmpty(oldFiles)) {
            reviewVersionMapper.update(null, new LambdaUpdateWrapper<ReviewVersion>()
                    .eq(ReviewVersion::getId, message.getNewVersionId())
                    .set(ReviewVersion::getFilesReady, 1)
                    .set(ReviewVersion::getFileCount, 0)
                    .set(ReviewVersion::getUpdateTime, LocalDateTime.now()));
            log.info("no history files to copy, version={} traceId={}", message.getNewVersionId(), message.getTraceId());
            return;
        }
        for (ReviewFile file : oldFiles) {
            ReviewFile copy = new ReviewFile();
            copy.setTenantId(message.getTenantId());
            copy.setVersionId(message.getNewVersionId());
            copy.setFileName(file.getFileName());
            copy.setMinioObjectName(file.getMinioObjectName());
            copy.setFileMd5(file.getFileMd5());
            copy.setFileSize(file.getFileSize());
            copy.setFileType(file.getFileType());
            reviewFileMapper.insert(copy);
        }
        reviewVersionMapper.update(null, new LambdaUpdateWrapper<ReviewVersion>()
                .eq(ReviewVersion::getId, message.getNewVersionId())
                .set(ReviewVersion::getFilesReady, 1)
                .set(ReviewVersion::getFileCount, oldFiles.size())
                .set(ReviewVersion::getUpdateTime, LocalDateTime.now()));
        log.info("copy finished, newVersion={}, filesCount={}, traceId={}", message.getNewVersionId(),
                oldFiles.size(), message.getTraceId());
    }

    /**
     * 复制失败时更新状态，供前端提示。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markCopyFailed(Long versionId) {
        reviewVersionMapper.update(null, new LambdaUpdateWrapper<ReviewVersion>()
                .eq(ReviewVersion::getId, versionId)
                .set(ReviewVersion::getFilesReady, -1)
                .set(ReviewVersion::getUpdateTime, LocalDateTime.now()));
    }
}
