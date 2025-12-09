package com.review.service;

import com.review.common.BusinessException;
import com.review.common.UserContext;
import com.review.dto.FileInfoResponse;
import com.review.dto.ReviewActionRequest;
import com.review.dto.ReviewDetailResponse;
import com.review.entity.ReviewTask;
import com.review.entity.ReviewVersion;
import com.review.entity.User;
import com.review.mapper.ReviewTaskMapper;
import com.review.mapper.ReviewVersionMapper;
import com.review.mapper.UserMapper;
import com.review.service.support.TaskCacheSupport;
import com.review.utils.RoleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审查操作相关业务。
 */
@Service
public class ReviewService {

    private static final String STATUS_REVIEWING = "REVIEWING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    @Autowired
    private ReviewTaskMapper reviewTaskMapper;

    @Autowired
    private ReviewVersionMapper reviewVersionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    private TaskCacheSupport taskCacheSupport;

    /**
     * 获取审查详情。
     */
    public ReviewDetailResponse getReviewDetail(Long versionId) {
        Long tenantId = ensureTenant();
        ReviewVersion version = loadVersion(versionId, tenantId);
        ReviewTask task = loadTask(version.getTaskId(), tenantId);
        ensureReviewerAccess(task);

        ReviewDetailResponse detail = new ReviewDetailResponse();
        detail.setTaskId(task.getId());
        detail.setTaskName(task.getTaskName());
        detail.setVersionId(version.getId());
        detail.setVersionNumber(version.getVersionNumber());
        detail.setSubmitDesc(version.getSubmitDesc());
        detail.setSubmitTime(version.getCreateTime());
        detail.setStatus(version.getStatus());
        detail.setFileCount(version.getFileCount());
        detail.setFilesReady(version.getFilesReady());
        detail.setCreator(toRelation(loadUser(task.getCreatorId())));
        detail.setReviewer(toRelation(loadUser(task.getReviewerId())));
        detail.setFiles(fileService.listFiles(versionId));
        detail.setPreviousVersion(buildPreviousVersion(task, version));
        return detail;
    }

    /**
     * 审查通过。
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long versionId, ReviewActionRequest request) {
        handleReview(versionId, request, STATUS_APPROVED);
    }

    /**
     * 审查打回。
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long versionId, ReviewActionRequest request) {
        handleReview(versionId, request, STATUS_REJECTED);
    }

    private void handleReview(Long versionId, ReviewActionRequest request, String targetStatus) {
        Long tenantId = ensureTenant();
        ReviewVersion version = loadVersion(versionId, tenantId);
        ReviewTask task = loadTask(version.getTaskId(), tenantId);
        ensureReviewerAccess(task);
        ensureVersionReviewable(version);

        LocalDateTime now = LocalDateTime.now();
        version.setStatus(targetStatus);
        version.setReviewResult(targetStatus);
        version.setReviewComment(formatComment(request));
        version.setReviewTime(now);
        version.setReviewerId(task.getReviewerId());
        reviewVersionMapper.updateById(version);

        task.setCurrentStatus(targetStatus);
        reviewTaskMapper.updateById(task);
        taskCacheSupport.evictStatistics(tenantId, task.getCreatorId(), task.getReviewerId());
    }

    private ReviewDetailResponse.PreviousVersionInfo buildPreviousVersion(ReviewTask task, ReviewVersion current) {
        if (current.getVersionNumber() == null || current.getVersionNumber() <= 1) {
            return null;
        }
        ReviewVersion previous = reviewVersionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewVersion>()
                        .eq(ReviewVersion::getTaskId, task.getId())
                        .eq(ReviewVersion::getVersionNumber, current.getVersionNumber() - 1)
                        .last("LIMIT 1"));
        if (previous == null) {
            return null;
        }
        ReviewDetailResponse.PreviousVersionInfo info = new ReviewDetailResponse.PreviousVersionInfo();
        info.setVersionNumber(previous.getVersionNumber());
        info.setStatus(previous.getStatus());
        info.setReviewComment(previous.getReviewComment());
        info.setReviewTime(previous.getReviewTime());
        return info;
    }

    private ReviewDetailResponse.RelationInfo toRelation(User user) {
        if (user == null) {
            return null;
        }
        ReviewDetailResponse.RelationInfo info = new ReviewDetailResponse.RelationInfo();
        info.setUserId(user.getId());
        info.setUsername(user.getUsername());
        info.setRealName(user.getRealName());
        info.setPhone(user.getPhone());
        return info;
    }

    private String formatComment(ReviewActionRequest request) {
        if (request == null || !StringUtils.hasText(request.getComment())) {
            return null;
        }
        return request.getComment().trim();
    }

    private void ensureVersionReviewable(ReviewVersion version) {
        if (!STATUS_REVIEWING.equalsIgnoreCase(version.getStatus())) {
            throw new BusinessException("当前版本已审查，无需重复操作");
        }
        if (version.getFilesReady() != null && version.getFilesReady() == 0) {
            throw new BusinessException("文件仍在复制中，请稍后再试");
        }
    }

    private void ensureReviewerAccess(ReviewTask task) {
        Long userId = ensureUser();
        if (!userId.equals(task.getReviewerId())) {
            throw new BusinessException("当前账号无权执行该操作");
        }
        if (!RoleUtil.hasRole(UserContext.getCurrentRoles(), "REVIEWER")) {
            throw new BusinessException("仅审查员可以执行该操作");
        }
    }

    private ReviewTask loadTask(Long taskId, Long tenantId) {
        ReviewTask task = reviewTaskMapper.selectById(taskId);
        if (task == null || !tenantId.equals(task.getTenantId())) {
            throw new BusinessException("任务不存在或无权访问");
        }
        return task;
    }

    private ReviewVersion loadVersion(Long versionId, Long tenantId) {
        ReviewVersion version = reviewVersionMapper.selectById(versionId);
        if (version == null || !tenantId.equals(version.getTenantId())) {
            throw new BusinessException("版本不存在或无权访问");
        }
        return version;
    }

    private User loadUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userMapper.selectById(userId);
    }

    private Long ensureTenant() {
        Long tenantId = UserContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new BusinessException("当前账号未绑定租户，无法执行操作");
        }
        return tenantId;
    }

    private Long ensureUser() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("无法识别当前用户，请重新登录");
        }
        return userId;
    }
}
