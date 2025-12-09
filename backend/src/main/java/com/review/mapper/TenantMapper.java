package com.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.review.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 租户 Mapper
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {

    /**
     * 增加租户已用存储（字节）。
     */
    @Update("UPDATE tenant SET storage_used = storage_used + #{size} WHERE id = #{tenantId}")
    void increaseStorageUsed(@Param("tenantId") Long tenantId, @Param("size") Long size);

    /**
     * 减少租户已用存储（字节）。
     */
    @Update("UPDATE tenant SET storage_used = GREATEST(storage_used - #{size}, 0) WHERE id = #{tenantId}")
    void decreaseStorageUsed(@Param("tenantId") Long tenantId, @Param("size") Long size);
}
