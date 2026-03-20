package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.RunnerIncomeRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 跑腿员收益记录Mapper接口
 */
@Mapper
public interface RunnerIncomeRecordMapper extends BaseMapper<RunnerIncomeRecord> {
}
