package com.review.common;

/**
 * Redis 缓存 Key 统一前缀，用于避免重复字符串散落在业务代码里。
 */
public interface CacheConstants {

    /** 普通用户任务统计缓存前缀，格式 stat:user:{tenantId}:{userId} */
    String STAT_USER_PREFIX = "stat:user:";

    /** 审查员任务统计缓存前缀，格式 stat:reviewer:{tenantId}:{reviewerId} */
    String STAT_REVIEWER_PREFIX = "stat:reviewer:";

    /** 审查员下拉选项缓存前缀，格式 options:reviewers:{tenantId} */
    String REVIEWER_OPTION_PREFIX = "options:reviewers:";

    /** 平台端租户列表缓存前缀（可附带查询条件） */
    String TENANT_LIST_PREFIX = "tenant:list:";

    /** 任务详情防穿透空值缓存前缀 */
    String TASK_MISS_PREFIX = "task:miss:";

    /** 版本详情防穿透空值缓存前缀 */
    String VERSION_MISS_PREFIX = "version:miss:";
}
