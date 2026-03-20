package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.StationMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站内消息Mapper接口
 */
@Mapper
public interface StationMessageMapper extends BaseMapper<StationMessage> {
}
