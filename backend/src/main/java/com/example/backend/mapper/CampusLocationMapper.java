package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.CampusLocation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 校园常用地点Mapper接口
 */
@Mapper
public interface CampusLocationMapper extends BaseMapper<CampusLocation> {
}
