package com.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.review.common.BusinessException;
import com.review.common.UserContext;
import com.review.dto.ReviewerOptionResponse;
import com.review.dto.ResubmitInitRequest;
import com.review.dto.ResubmitInitResponse;
import com.review.dto.ResubmitSubmitRequest;
import com.review.dto.TaskCreateRequest;
import com.review.dto.TaskCreateResponse;
import com.review.dto.TaskDetailResponse;
import com.review.dto.TaskListItemResponse;
import com.review.dto.TaskListQueryRequest;
import com.review.dto.TaskStatisticResponse;
import com.review.dto.TaskTimelineNode;
import com.review.dto.TaskVersionResponse;
import com.review.dto.VersionStatusResponse;
import com.review.entity.ReviewTask;
import com.review.entity.ReviewVersion;
import com.review.entity.ReviewFile;
import com.review.entity.User;
import com.review.mapper.ReviewTaskMapper;
import com.review.mapper.ReviewVersionMapper;
import com.review.mapper.UserMapper;
import com.review.mapper.ReviewFileMapper;
import com.review.service.support.TaskCacheSupport;
import com.review.utils.RoleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 审查任务相关业务。
 */
@Service
public class TaskService {

    private static final String STATUS_PREPARING = "PREPARING";
    private static final String STATUS_REVIEWING = "REVIEWING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final Duration REVIEWER_OPTION_TTL = Duration.ofMinutes(10);
    private static final Duration STAT_TTL = Duration.ofSeconds(60);
    private static final Duration MISS_TTL = Duration.ofSeconds(30);

    @Autowired
    private ReviewTaskMapper reviewTaskMapper;

    @Autowired
    private ReviewVersionMapper reviewVersionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ReviewFileMapper reviewFileMapper;

    @Autowired
    private TaskCacheSupport taskCacheSupport;

    private final ExecutorService copyExecutor = Executors.newCachedThreadPool();

    /**
     * 创建审查任务（默认生成版本 v1）。
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskCreateResponse createTask(TaskCreateRequest request) {
        Long tenantId = ensureTenant();
        Long creatorId = ensureUser();
        ensureUserRole();

        String trimmedName = request.getTaskName().trim();
        ensureTaskNameUnique(tenantId, trimmedName, null);
        User reviewer = loadReviewer(tenantId, request.getReviewerId());

        ReviewTask task = new ReviewTask();
        task.setTenantId(tenantId);
        task.setTaskName(trimmedName);
        task.setCreatorId(creatorId);
        task.setReviewerId(request.getReviewerId());
        task.setCurrentVersion(1);
        task.setCurrentStatus(STATUS_REVIEWING);
        reviewTaskMapper.insert(task);

        ReviewVersion version = new ReviewVersion();
        version.setTenantId(tenantId);
        version.setTaskId(task.getId());
        version.setVersionNumber(1);
        version.setSubmitDesc(formatSubmitDesc(request.getSubmitDesc()));
        version.setStatus(STATUS_REVIEWING);
        version.setReviewerId(reviewer.getId());
        version.setFilesReady(1);
        version.setFileCount(0);
        reviewVersionMapper.insert(version);
        taskCacheSupport.evictStatistics(tenantId, creatorId, reviewer.getId());

        return new TaskCreateResponse(task.getId(), version.getId());
    }

    /**
     * 查询当前租户可用的审查员。
     */
    public List<ReviewerOptionResponse> listReviewers() {
        Long tenantId = ensureTenant();
        String cacheKey = taskCacheSupport.reviewerOptionKey(tenantId);
        @SuppressWarnings("unchecked")
        List<ReviewerOptionResponse> cached = (List<ReviewerOptionResponse>) taskCacheSupport.read(cacheKey);
        if (cached != null) {
            return cached;
        }
        List<User> candidates = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getTenantId, tenantId)
                .eq(User::getStatus, 1)
                .orderByDesc(User::getLastLoginTime)
                .orderByAsc(User::getId));
        List<ReviewerOptionResponse> responses = candidates.stream()
                .filter(user -> RoleUtil.hasRole(user.getRoles(), "REVIEWER"))
                .map(user -> new ReviewerOptionResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getRealName(),
                        user.getPhone(),
                        user.getLastLoginTime(),
                        isDualRole(user.getRoles())
                ))
                .collect(Collectors.toList());
        taskCacheSupport.cache(cacheKey, responses, REVIEWER_OPTION_TTL);
        return responses;
    }

    /**
     * 普通用户查看我的任务。
     */
    public Page<TaskListItemResponse> queryMyTasks(TaskListQueryRequest request) {
        Long tenantId = ensureTenant();
        Long userId = ensureUser();
        Page<ReviewTask> taskPage = selectTaskPage(tenantId, userId, null, request, false);
        return buildTaskListPage(taskPage, true, false, false);
    }

    /**
     * 审查员查看指派任务。
     */
    public Page<TaskListItemResponse> queryReviewerTasks(TaskListQueryRequest request) {
        Long tenantId = ensureTenant();
        Long userId = ensureUser();
        boolean forcePending = Boolean.TRUE.equals(request.getPendingOnly());
        Page<ReviewTask> taskPage = selectTaskPage(tenantId, null, userId, request, forcePending);
        return buildTaskListPage(taskPage, true, true, true);
    }

    /**
     * 普通用户任务统计。
     */
    public TaskStatisticResponse loadMyStatistics() {
        Long tenantId = ensureTenant();
        Long userId = ensureUser();
        String cacheKey = taskCacheSupport.userStatKey(tenantId, userId);
        TaskStatisticResponse cached = readStatisticCache(cacheKey);
        if (cached != null) {
            return cached;
        }
        TaskStatisticResponse response = buildStatistics(tenantId, userId, null);
        taskCacheSupport.cache(cacheKey, response, STAT_TTL);
        return response;
    }

    /**
     * 审查员任务统计。
     */
    public TaskStatisticResponse loadReviewerStatistics() {
        Long tenantId = ensureTenant();
        Long userId = ensureUser();
        String cacheKey = taskCacheSupport.reviewerStatKey(tenantId, userId);
        TaskStatisticResponse cached = readStatisticCache(cacheKey);
        if (cached != null) {
            return cached;
        }
        TaskStatisticResponse response = buildStatistics(tenantId, null, userId);
        taskCacheSupport.cache(cacheKey, response, STAT_TTL);
        return response;
    }

    /**
     * 任务详情。
     */
    public TaskDetailResponse getTaskDetail(Long taskId) {
        Long tenantId = ensureTenant();
        ReviewTask task = loadTask(taskId, tenantId);
        ensureTaskReadable(task);

        List<ReviewVersion> versions = loadTaskVersions(tenantId, task.getId());
        Map<Long, User> reviewerMap = loadUsers(versions.stream()
                .map(ReviewVersion::getReviewerId)
                .collect(Collectors.toSet()));

        TaskDetailResponse response = new TaskDetailResponse();
        response.setTaskId(task.getId());
        response.setTaskName(task.getTaskName());
        response.setCurrentVersion(task.getCurrentVersion());
        response.setCurrentStatus(task.getCurrentStatus());
        response.setStatusLabel(statusLabel(task.getCurrentStatus()));
        response.setCanResubmit(STATUS_REJECTED.equals(task.getCurrentStatus()));
        response.setReviewer(toRelation(loadUser(task.getReviewerId())));
        response.setCreator(toRelation(loadUser(task.getCreatorId())));

        List<TaskVersionResponse> versionResponses = buildVersionResponses(task, versions, reviewerMap);
        response.setVersions(versionResponses);
        ReviewVersion currentVersion = findCurrentVersion(task, versions);
        response.setCurrentVersionDetail(toVersionDetail(currentVersion, reviewerMap));
        response.setTimeline(buildTimeline(versions, response.getCreator()));
        return response;
    }

    /**
     * 任务版本列表。
     */
    public List<TaskVersionResponse> listTaskVersions(Long taskId) {
        Long tenantId = ensureTenant();
        ReviewTask task = loadTask(taskId, tenantId);
        ensureTaskReadable(task);
        List<ReviewVersion> versions = loadTaskVersions(tenantId, taskId);
        Map<Long, User> reviewerMap = loadUsers(versions.stream()
                .map(ReviewVersion::getReviewerId)
                .collect(Collectors.toSet()));
        return buildVersionResponses(task, versions, reviewerMap);
    }

    /**
     * 初始化再次提交，创建准备中的版本。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResubmitInitResponse startResubmit(Long taskId, ResubmitInitRequest request) {
        Long tenantId = ensureTenant();
        Long userId = ensureUser();
        ReviewTask task = loadTask(taskId, tenantId);
        ensureCreator(task, userId);
        if (!STATUS_REJECTED.equals(task.getCurrentStatus())) {
            throw new BusinessException("仅在任务被打回后才能再次提交");
        }
        ReviewVersion oldVersion = loadVersion(request.getOldVersionId(), tenantId);
        if (!taskId.equals(oldVersion.getTaskId())) {
            throw new BusinessException("指定的旧版本不属于当前任务");
        }
        ReviewVersion preparing = reviewVersionMapper.selectOne(new LambdaQueryWrapper<ReviewVersion>()
                .eq(ReviewVersion::getTaskId, taskId)
                .eq(ReviewVersion::getStatus, STATUS_PREPARING)
                .last("LIMIT 1"));
        if (preparing != null) {
            return new ResubmitInitResponse(preparing.getId(), preparing.getVersionNumber());
        }
        Integer nextVersion = getNextVersionNumber(taskId);
        ReviewVersion newVersion = new ReviewVersion();
        newVersion.setTenantId(tenantId);
        newVersion.setTaskId(taskId);
        newVersion.setVersionNumber(nextVersion);
        newVersion.setStatus(STATUS_PREPARING);
        newVersion.setReviewerId(task.getReviewerId());
        newVersion.setFilesReady(Boolean.TRUE.equals(request.getReuseOldFiles()) ? 0 : 1);
        newVersion.setFileCount(0);
        newVersion.setCreateTime(LocalDateTime.now());
        reviewVersionMapper.insert(newVersion);

        if (Boolean.TRUE.equals(request.getReuseOldFiles())) {
            copyOldFilesAsync(oldVersion.getId(), newVersion.getId(), tenantId);
        }
        return new ResubmitInitResponse(newVersion.getId(), newVersion.getVersionNumber());
    }

    /**
     * 再次提交最终提交。
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitResubmit(Long taskId, ResubmitSubmitRequest request) {
        Long tenantId = ensureTenant();
        Long userId = ensureUser();
        ReviewTask task = loadTask(taskId, tenantId);
        ensureCreator(task, userId);
        ReviewVersion version = loadVersion(request.getVersionId(), tenantId);
        if (!taskId.equals(version.getTaskId())) {
            throw new BusinessException("版本不属于该任务");
        }
        if (!STATUS_PREPARING.equals(version.getStatus())) {
            throw new BusinessException("当前版本状态不是准备中");
        }
        if (version.getFilesReady() == null || version.getFilesReady() == 0) {
            throw new BusinessException("文件仍在复制中，请稍后");
        }
        int fileCount = countFiles(version.getId());
        if (fileCount == 0) {
            throw new BusinessException("请先上传至少一个文件");
        }
        version.setStatus(STATUS_REVIEWING);
        version.setSubmitDesc(formatSubmitDesc(request.getSubmitDesc()));
        version.setFileCount(fileCount);
        version.setCreateTime(LocalDateTime.now());
        version.setReviewComment(null);
        version.setReviewResult(null);
        version.setReviewTime(null);
        reviewVersionMapper.updateById(version);

        task.setCurrentVersion(version.getVersionNumber());
        task.setCurrentStatus(STATUS_REVIEWING);
        reviewTaskMapper.updateById(task);
        taskCacheSupport.evictStatistics(tenantId, task.getCreatorId(), task.getReviewerId());
    }

    public VersionStatusResponse getVersionStatus(Long versionId) {
        Long tenantId = ensureTenant();
        ReviewVersion version = loadVersion(versionId, tenantId);
        return new VersionStatusResponse(version.getId(), version.getStatus(), version.getFilesReady(), version.getFileCount());
    }

    private Page<TaskListItemResponse> buildTaskListPage(Page<ReviewTask> taskPage,
                                                         boolean includeReviewer,
                                                         boolean includeCreator,
                                                         boolean reviewerView) {
        Page<TaskListItemResponse> result = new Page<>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        result.setRecords(convertToResponses(taskPage.getRecords(), includeReviewer, includeCreator, reviewerView));
        return result;
    }

    private Page<ReviewTask> selectTaskPage(Long tenantId,
                                            Long creatorId,
                                            Long reviewerId,
                                            TaskListQueryRequest request,
                                            boolean forcePending) {
        int pageNo = request.getPageNo() == null ? 1 : request.getPageNo();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
        LambdaQueryWrapper<ReviewTask> wrapper = baseWrapper(tenantId, creatorId, reviewerId);
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(ReviewTask::getCurrentStatus, request.getStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(ReviewTask::getTaskName, request.getKeyword().trim());
        }
        applyDateRange(wrapper, request);
        if (forcePending) {
            wrapper.eq(ReviewTask::getCurrentStatus, STATUS_REVIEWING);
        }
        wrapper.orderByDesc(ReviewTask::getUpdateTime);
        return reviewTaskMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
    }

    private void applyDateRange(LambdaQueryWrapper<ReviewTask> wrapper, TaskListQueryRequest request) {
        if (request.getStartDate() != null) {
            wrapper.ge(ReviewTask::getCreateTime, request.getStartDate().atStartOfDay());
        }
        if (request.getEndDate() != null) {
            wrapper.le(ReviewTask::getCreateTime, request.getEndDate().plusDays(1).atStartOfDay().minusSeconds(1));
        }
    }

    private List<TaskListItemResponse> convertToResponses(List<ReviewTask> tasks,
                                                          boolean includeReviewer,
                                                          boolean includeCreator,
                                                          boolean reviewerView) {
        if (CollectionUtils.isEmpty(tasks)) {
            return Collections.emptyList();
        }
        Map<Long, ReviewVersion> versionMap = loadCurrentVersions(tasks);
        Map<Long, User> reviewerMap = includeReviewer
                ? loadUsers(tasks.stream().map(ReviewTask::getReviewerId).collect(Collectors.toSet()))
                : Collections.emptyMap();
        Map<Long, User> creatorMap = includeCreator
                ? loadUsers(tasks.stream().map(ReviewTask::getCreatorId).collect(Collectors.toSet()))
                : Collections.emptyMap();
        LocalDateTime now = LocalDateTime.now();
        List<TaskListItemResponse> responses = new ArrayList<>();
        for (ReviewTask task : tasks) {
            TaskListItemResponse resp = new TaskListItemResponse();
            resp.setTaskId(task.getId());
            resp.setTaskName(task.getTaskName());
            resp.setCurrentVersion(task.getCurrentVersion());
            resp.setCurrentStatus(task.getCurrentStatus());
            resp.setStatusLabel(statusLabel(task.getCurrentStatus()));
            resp.setStatusTagType(statusTagType(task.getCurrentStatus()));
            resp.setCreateTime(task.getCreateTime());
            resp.setCanResubmit(STATUS_REJECTED.equals(task.getCurrentStatus()));

            ReviewVersion version = versionMap.get(task.getId());
            if (version != null) {
                resp.setCurrentVersionId(version.getId());
                resp.setFileCount(version.getFileCount());
                resp.setLastUpdateTime(version.getUpdateTime());
            } else {
                resp.setCurrentVersionId(null);
                resp.setFileCount(0);
                resp.setLastUpdateTime(task.getUpdateTime());
            }

            if (includeReviewer) {
                User reviewer = reviewerMap.get(task.getReviewerId());
                if (reviewer != null) {
                    resp.setReviewerId(reviewer.getId());
                    resp.setReviewerName(reviewer.getRealName());
                    resp.setReviewerPhone(reviewer.getPhone());
                }
            }
            if (includeCreator) {
                User creator = creatorMap.get(task.getCreatorId());
                if (creator != null) {
                    resp.setCreatorId(creator.getId());
                    resp.setCreatorName(creator.getRealName());
                    resp.setCreatorPhone(creator.getPhone());
                }
            }
            if (reviewerView && STATUS_REVIEWING.equals(task.getCurrentStatus()) && version != null) {
                long days = Duration.between(version.getCreateTime(), now).toDays();
                resp.setPendingDays((int) Math.max(days, 0));
            }
            responses.add(resp);
        }
        return responses;
    }

    private Map<Long, ReviewVersion> loadCurrentVersions(List<ReviewTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> target = tasks.stream()
                .collect(Collectors.toMap(ReviewTask::getId, ReviewTask::getCurrentVersion));
        List<Long> taskIds = new ArrayList<>(target.keySet());
        List<ReviewVersion> versions = reviewVersionMapper.selectList(new LambdaQueryWrapper<ReviewVersion>()
                .in(ReviewVersion::getTaskId, taskIds)
                .orderByDesc(ReviewVersion::getVersionNumber));
        Map<Long, ReviewVersion> map = new HashMap<>();
        for (ReviewVersion version : versions) {
            Integer need = target.get(version.getTaskId());
            if (need != null && version.getVersionNumber() != null
                    && version.getVersionNumber().equals(need) && !map.containsKey(version.getTaskId())) {
                map.put(version.getTaskId(), version);
            }
        }
        return map;
    }

    private ReviewVersion findCurrentVersion(ReviewTask task, List<ReviewVersion> versions) {
        if (CollectionUtils.isEmpty(versions)) {
            throw new BusinessException("任务版本信息不存在");
        }
        for (ReviewVersion version : versions) {
            if (version.getVersionNumber() != null
                    && version.getVersionNumber().equals(task.getCurrentVersion())) {
                return version;
            }
        }
        return versions.get(0);
    }

    private Map<Long, User> loadUsers(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        Set<Long> filtered = ids.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (filtered.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(filtered);
        return users.stream().collect(Collectors.toMap(User::getId, user -> user));
    }

    private TaskStatisticResponse buildStatistics(Long tenantId, Long creatorId, Long reviewerId) {
        TaskStatisticResponse response = new TaskStatisticResponse();
        long total = reviewTaskMapper.selectCount(baseWrapper(tenantId, creatorId, reviewerId));
        long reviewing = reviewTaskMapper.selectCount(baseWrapper(tenantId, creatorId, reviewerId)
                .eq(ReviewTask::getCurrentStatus, STATUS_REVIEWING));
        long approved = reviewTaskMapper.selectCount(baseWrapper(tenantId, creatorId, reviewerId)
                .eq(ReviewTask::getCurrentStatus, STATUS_APPROVED));
        long rejected = reviewTaskMapper.selectCount(baseWrapper(tenantId, creatorId, reviewerId)
                .eq(ReviewTask::getCurrentStatus, STATUS_REJECTED));
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long monthTotal = reviewTaskMapper.selectCount(baseWrapper(tenantId, creatorId, reviewerId)
                .ge(ReviewTask::getCreateTime, monthStart));
        long monthApproved = reviewTaskMapper.selectCount(baseWrapper(tenantId, creatorId, reviewerId)
                .ge(ReviewTask::getCreateTime, monthStart)
                .eq(ReviewTask::getCurrentStatus, STATUS_APPROVED));

        response.setTotal(total);
        response.setReviewing(reviewing);
        response.setApproved(approved);
        response.setRejected(rejected);
        response.setMonthTotal(monthTotal);
        response.setMonthApproved(monthApproved);
        response.setPassRate(total == 0 ? 0.0 : Math.round((approved * 1.0 / total) * 1000.0) / 1000.0);
        return response;
    }

    private LambdaQueryWrapper<ReviewTask> baseWrapper(Long tenantId, Long creatorId, Long reviewerId) {
        LambdaQueryWrapper<ReviewTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewTask::getTenantId, tenantId);
        if (creatorId != null) {
            wrapper.eq(ReviewTask::getCreatorId, creatorId);
        }
        if (reviewerId != null) {
            wrapper.eq(ReviewTask::getReviewerId, reviewerId);
        }
        return wrapper;
    }

    private List<TaskVersionResponse> buildVersionResponses(ReviewTask task,
                                                            List<ReviewVersion> versions,
                                                            Map<Long, User> reviewerMap) {
        if (CollectionUtils.isEmpty(versions)) {
            return Collections.emptyList();
        }
        List<TaskVersionResponse> responses = new ArrayList<>();
        for (ReviewVersion version : versions) {
            TaskVersionResponse resp = new TaskVersionResponse();
            resp.setVersionId(version.getId());
            resp.setVersionNumber(version.getVersionNumber());
            resp.setStatus(version.getStatus());
            resp.setStatusLabel(statusLabel(version.getStatus()));
            resp.setFilesReady(version.getFilesReady());
            resp.setFileCount(version.getFileCount());
            resp.setSubmitDesc(version.getSubmitDesc());
            resp.setReviewComment(version.getReviewComment());
            resp.setSubmitTime(version.getCreateTime());
            resp.setReviewTime(version.getReviewTime());
            User reviewer = reviewerMap.get(version.getReviewerId());
            resp.setReviewerName(reviewer != null ? reviewer.getRealName() : null);
            resp.setCurrent(version.getVersionNumber() != null
                    && version.getVersionNumber().equals(task.getCurrentVersion()));
            if (version.getFilesReady() != null && version.getFilesReady() == 0) {
                resp.setProgressMsg("文件复制中，请稍候");
            }
            responses.add(resp);
        }
        return responses;
    }

    private TaskDetailResponse.VersionDetail toVersionDetail(ReviewVersion version,
                                                             Map<Long, User> reviewerMap) {
        if (version == null) {
            return null;
        }
        TaskDetailResponse.VersionDetail detail = new TaskDetailResponse.VersionDetail();
        detail.setVersionId(version.getId());
        detail.setVersionNumber(version.getVersionNumber());
        detail.setStatus(version.getStatus());
        detail.setStatusLabel(statusLabel(version.getStatus()));
        detail.setFileCount(version.getFileCount());
        detail.setFilesReady(version.getFilesReady());
        detail.setSubmitDesc(version.getSubmitDesc());
        detail.setReviewComment(version.getReviewComment());
        detail.setSubmitTime(version.getCreateTime());
        detail.setReviewTime(version.getReviewTime());
        User reviewer = reviewerMap.get(version.getReviewerId());
        detail.setReviewerName(reviewer != null ? reviewer.getRealName() : null);
        return detail;
    }

    private List<TaskTimelineNode> buildTimeline(List<ReviewVersion> versions,
                                                 TaskDetailResponse.RelationInfo creator) {
        if (CollectionUtils.isEmpty(versions)) {
            return Collections.emptyList();
        }
        List<ReviewVersion> sorted = new ArrayList<>(versions);
        sorted.sort((a, b) -> {
            if (a.getVersionNumber() == null || b.getVersionNumber() == null) {
                return 0;
            }
            return Integer.compare(a.getVersionNumber(), b.getVersionNumber());
        });
        List<TaskTimelineNode> nodes = new ArrayList<>();
        String creatorName = creator != null && StringUtils.hasText(creator.getRealName())
                ? creator.getRealName()
                : "提交人";
        for (ReviewVersion version : sorted) {
            nodes.add(new TaskTimelineNode(
                    "提交 v" + version.getVersionNumber(),
                    creatorName + " 提交了版本 v" + version.getVersionNumber(),
                    version.getCreateTime()
            ));
            if (version.getReviewTime() != null) {
                String title = "审查结果 - " + statusLabel(version.getStatus());
                String desc = StringUtils.hasText(version.getReviewComment())
                        ? version.getReviewComment()
                        : "无审查意见";
                nodes.add(new TaskTimelineNode(title, desc, version.getReviewTime()));
            }
        }
        return nodes;
    }

    private TaskDetailResponse.RelationInfo toRelation(User user) {
        if (user == null) {
            return null;
        }
        TaskDetailResponse.RelationInfo info = new TaskDetailResponse.RelationInfo();
        info.setUserId(user.getId());
        info.setUsername(user.getUsername());
        info.setRealName(user.getRealName());
        info.setPhone(user.getPhone());
        return info;
    }

    private User loadUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userMapper.selectById(userId);
    }

    private ReviewTask loadTask(Long taskId, Long tenantId) {
        String missKey = taskCacheSupport.taskMissKey(taskId);
        if (isMissMarked(missKey)) {
            throw new BusinessException("??????????");
        }
        ReviewTask task = reviewTaskMapper.selectById(taskId);
        if (task == null) {
            markMiss(missKey);
            throw new BusinessException("??????????");
        }
        if (!tenantId.equals(task.getTenantId())) {
            throw new BusinessException("??????????");
        }
        return task;
    }

    private List<ReviewVersion> loadTaskVersions(Long tenantId, Long taskId) {
        return reviewVersionMapper.selectList(new LambdaQueryWrapper<ReviewVersion>()
                .eq(ReviewVersion::getTenantId, tenantId)
                .eq(ReviewVersion::getTaskId, taskId)
                .orderByDesc(ReviewVersion::getVersionNumber));
    }

    private String statusLabel(String status) {
        if (STATUS_APPROVED.equalsIgnoreCase(status)) {
            return "已通过";
        }
        if (STATUS_REJECTED.equalsIgnoreCase(status)) {
            return "已打回";
        }
        return "审查中";
    }

    private String statusTagType(String status) {
        if (STATUS_PREPARING.equalsIgnoreCase(status)) {
            return "info";
        }
        if (STATUS_APPROVED.equalsIgnoreCase(status)) {
            return "success";
        }
        if (STATUS_REJECTED.equalsIgnoreCase(status)) {
            return "danger";
        }
        return "warning";
    }

    private ReviewVersion loadVersion(Long versionId, Long tenantId) {
        String missKey = taskCacheSupport.versionMissKey(versionId);
        if (isMissMarked(missKey)) {
            throw new BusinessException("??????????");
        }
        ReviewVersion version = reviewVersionMapper.selectById(versionId);
        if (version == null) {
            markMiss(missKey);
            throw new BusinessException("??????????");
        }
        if (!tenantId.equals(version.getTenantId())) {
            throw new BusinessException("??????????");
        }
        return version;
    }

    private Integer getNextVersionNumber(Long taskId) {
        ReviewVersion maxVersion = reviewVersionMapper.selectOne(new LambdaQueryWrapper<ReviewVersion>()
                .select(ReviewVersion::getVersionNumber)
                .eq(ReviewVersion::getTaskId, taskId)
                .orderByDesc(ReviewVersion::getVersionNumber)
                .last("LIMIT 1"));
        Integer max = maxVersion == null ? null : maxVersion.getVersionNumber();
        if (max == null) {
            return 1;
        }
        return max + 1;
    }

    private void copyOldFilesAsync(Long oldVersionId, Long newVersionId, Long tenantId) {
        CompletableFuture.runAsync(() -> {
            List<ReviewFile> oldFiles = reviewFileMapper.selectList(new LambdaQueryWrapper<ReviewFile>()
                    .eq(ReviewFile::getVersionId, oldVersionId));
            if (CollectionUtils.isEmpty(oldFiles)) {
                reviewVersionMapper.update(null, new LambdaUpdateWrapper<ReviewVersion>()
                        .eq(ReviewVersion::getId, newVersionId)
                        .set(ReviewVersion::getFilesReady, 1)
                        .set(ReviewVersion::getFileCount, 0));
                return;
            }
            for (ReviewFile file : oldFiles) {
                ReviewFile copy = new ReviewFile();
                copy.setTenantId(tenantId);
                copy.setVersionId(newVersionId);
                copy.setFileName(file.getFileName());
                copy.setMinioObjectName(file.getMinioObjectName());
                copy.setFileMd5(file.getFileMd5());
                copy.setFileSize(file.getFileSize());
                copy.setFileType(file.getFileType());
                reviewFileMapper.insert(copy);
            }
            reviewVersionMapper.update(null, new LambdaUpdateWrapper<ReviewVersion>()
                    .eq(ReviewVersion::getId, newVersionId)
                    .set(ReviewVersion::getFilesReady, 1)
                    .set(ReviewVersion::getFileCount, oldFiles.size()));
        }, copyExecutor);
    }

    private int countFiles(Long versionId) {
        return Math.toIntExact(reviewFileMapper.selectCount(new LambdaQueryWrapper<ReviewFile>()
                .eq(ReviewFile::getVersionId, versionId)));
    }

    private void ensureTaskNameUnique(Long tenantId, String taskName, Long excludeTaskId) {
        LambdaQueryWrapper<ReviewTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewTask::getTenantId, tenantId)
                .eq(ReviewTask::getTaskName, taskName);
        if (excludeTaskId != null) {
            wrapper.ne(ReviewTask::getId, excludeTaskId);
        }
        Long count = reviewTaskMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException("任务名称已存在，请更换");
        }
    }

    private User loadReviewer(Long tenantId, Long reviewerId) {
        User reviewer = userMapper.selectById(reviewerId);
        if (reviewer == null || !tenantId.equals(reviewer.getTenantId())) {
            throw new BusinessException("审查员不存在或不属于当前租户");
        }
        if (reviewer.getStatus() == null || reviewer.getStatus() != 1) {
            throw new BusinessException("该审查员已停用，无法指派");
        }
        if (!RoleUtil.hasRole(reviewer.getRoles(), "REVIEWER")) {
            throw new BusinessException("该用户不是审查员");
        }
        return reviewer;
    }

    private boolean isDualRole(String roles) {
        if (!StringUtils.hasText(roles)) {
            return false;
        }
        return RoleUtil.hasRole(roles, "REVIEWER") && RoleUtil.hasRole(roles, "USER");
    }

    private String formatSubmitDesc(String submitDesc) {
        if (!StringUtils.hasText(submitDesc)) {
            return null;
        }
        return submitDesc.trim();
    }

    private Long ensureTenant() {
        Long tenantId = UserContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new BusinessException("当前账号未绑定租户，无法操作任务");
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

    private void ensureUserRole() {
        if (!RoleUtil.hasRole(UserContext.getCurrentRoles(), "USER")) {
            throw new BusinessException("仅普通用户可以创建审查任务");
        }
    }

    private void ensureTaskReadable(ReviewTask task) {
        Long currentUser = ensureUser();
        if (!currentUser.equals(task.getCreatorId()) && !currentUser.equals(task.getReviewerId())) {
            throw new BusinessException("当前账号无权查看该任务");
        }
    }

    private void ensureCreator(ReviewTask task, Long userId) {
        if (!userId.equals(task.getCreatorId())) {
            throw new BusinessException("仅任务创建者可以执行该操作");
        }
    }

    private TaskStatisticResponse readStatisticCache(String cacheKey) {
        if (!StringUtils.hasText(cacheKey)) {
            return null;
        }
        Object cached = taskCacheSupport.read(cacheKey);
        if (cached instanceof TaskStatisticResponse) {
            return (TaskStatisticResponse) cached;
        }
        return null;
    }

    private boolean isMissMarked(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        return taskCacheSupport.read(key) != null;
    }

    private void markMiss(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        taskCacheSupport.cache(key, Boolean.TRUE, MISS_TTL);
    }

}
