package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.RunnerAuth;
import org.apache.ibatis.annotations.Mapper;

/**
 * 跑腿员认证Mapper接口
 */
@Mapper
public interface RunnerAuthMapper extends BaseMapper<RunnerAuth> {
}
