package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志Mapper接口
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
