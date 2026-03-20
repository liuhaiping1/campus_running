package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.ErrandCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务分类Mapper接口
 */
@Mapper
public interface ErrandCategoryMapper extends BaseMapper<ErrandCategory> {
}
