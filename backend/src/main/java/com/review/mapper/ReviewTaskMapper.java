package com.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.review.entity.ReviewTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审查任务 Mapper。
 */
@Mapper
public interface ReviewTaskMapper extends BaseMapper<ReviewTask> {
}
