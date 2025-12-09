package com.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户实体
 */
@Data
@TableName("tenant")
public class Tenant {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 企业名称 */
    private String tenantName;

    /** 联系人姓名 */
    private String contactName;

    /** 联系电话 */
    private String contactPhone;

    /** 存储配额（字节） */
    private Long storageQuota;

    /** 已用存储（字节） */
    private Long storageUsed;

    /** 用户数量配额 */
    private Integer userQuota;

    /** 已创建用户数 */
    private Integer userCount;

    /** 状态：1-启用，0-停用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
