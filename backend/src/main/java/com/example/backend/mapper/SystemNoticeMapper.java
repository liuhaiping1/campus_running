package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.SystemNotice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统公告Mapper接口
 */
@Mapper
public interface SystemNoticeMapper extends BaseMapper<SystemNotice> {
}
