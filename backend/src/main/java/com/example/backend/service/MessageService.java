package com.example.backend.service;

import com.example.backend.vo.MessagePageVO;

/**
 * 站内消息服务接口
 * <p>
 * 提供当前登录用户消息分页查询、单条已读和全部已读能力。
 * </p>
 */
public interface MessageService {

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
    MessagePageVO list(Long userId, String bizType, Integer isRead, int pageNum, int pageSize);

    /**
     * 标记当前用户的一条消息为已读
     *
     * @param userId 当前用户ID
     * @param id     消息ID
     */
    void markRead(Long userId, Long id);

    /**
     * 标记当前用户全部未读消息为已读
     *
     * @param userId 当前用户ID
     */
    void markAllRead(Long userId);
}
