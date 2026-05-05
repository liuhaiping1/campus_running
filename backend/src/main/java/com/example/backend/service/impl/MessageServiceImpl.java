package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.StationMessage;
import com.example.backend.mapper.StationMessageMapper;
import com.example.backend.service.MessageService;
import com.example.backend.vo.MessagePageVO;
import com.example.backend.vo.MessageVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 站内消息服务实现类
 * <p>
 * 实现消息分页查询、未读数量统计和已读状态更新。
 * </p>
 */
@Service
public class MessageServiceImpl implements MessageService {

    private static final int READ = 1;
    private static final int UNREAD = 0;

    private final StationMessageMapper stationMessageMapper;

    /**
     * 构造函数注入站内消息Mapper
     *
     * @param stationMessageMapper 站内消息Mapper
     */
    public MessageServiceImpl(StationMessageMapper stationMessageMapper) {
        this.stationMessageMapper = stationMessageMapper;
    }

    /**
     * 分页查询当前用户站内消息
     *
     * @param userId   当前用户ID
     * @param bizType  业务类型，可为空
     * @param isRead   已读状态，可为空
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 消息分页结果，包含未读数
     */
    @Override
    public MessagePageVO list(Long userId, String bizType, Integer isRead, int pageNum, int pageSize) {
        LambdaQueryWrapper<StationMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StationMessage::getReceiverUserId, userId);
        if (bizType != null && !bizType.isBlank()) {
            wrapper.eq(StationMessage::getBizType, bizType);
        }
        if (isRead != null) {
            wrapper.eq(StationMessage::getIsRead, isRead);
        }
        wrapper.orderByDesc(StationMessage::getSendTime);

        Page<StationMessage> result = stationMessageMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        LambdaQueryWrapper<StationMessage> unreadWrapper = new LambdaQueryWrapper<>();
        unreadWrapper.eq(StationMessage::getReceiverUserId, userId)
                .eq(StationMessage::getIsRead, UNREAD);

        return MessagePageVO.builder()
                .total(result.getTotal())
                .unreadCount(stationMessageMapper.selectCount(unreadWrapper))
                .records(result.getRecords().stream().map(MessageVO::from).collect(Collectors.toList()))
                .build();
    }

    /**
     * 标记当前用户的一条消息为已读
     *
     * @param userId 当前用户ID
     * @param id     消息ID
     */
    @Override
    public void markRead(Long userId, Long id) {
        StationMessage message = stationMessageMapper.selectById(id);
        if (message == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!userId.equals(message.getReceiverUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!Integer.valueOf(READ).equals(message.getIsRead())) {
            message.setIsRead(READ);
            message.setReadTime(LocalDateTime.now());
            message.setUpdateTime(message.getReadTime());
            stationMessageMapper.updateById(message);
        }
    }

    /**
     * 标记当前用户全部未读消息为已读
     *
     * @param userId 当前用户ID
     */
    @Override
    public void markAllRead(Long userId) {
        LambdaUpdateWrapper<StationMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(StationMessage::getReceiverUserId, userId)
                .eq(StationMessage::getIsRead, UNREAD)
                .set(StationMessage::getIsRead, READ)
                .set(StationMessage::getReadTime, LocalDateTime.now())
                .set(StationMessage::getUpdateTime, LocalDateTime.now());
        stationMessageMapper.update(null, wrapper);
    }
}
