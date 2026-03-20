package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.AppealRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 申诉记录Mapper接口
 */
@Mapper
public interface AppealRecordMapper extends BaseMapper<AppealRecord> {
}
