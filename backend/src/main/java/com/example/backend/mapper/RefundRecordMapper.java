package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.RefundRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款记录Mapper接口
 */
@Mapper
public interface RefundRecordMapper extends BaseMapper<RefundRecord> {
}
