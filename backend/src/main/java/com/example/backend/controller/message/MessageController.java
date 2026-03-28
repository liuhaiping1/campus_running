package com.example.backend.controller.message;

import com.example.backend.common.Result;
import com.example.backend.security.LoginUser;
import com.example.backend.service.MessageService;
import com.example.backend.vo.MessagePageVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内消息控制器
 * <p>
 * 处理当前登录用户的消息查询和已读操作。
 * </p>
 */
@RestController
@RequestMapping("/api/message")
public class MessageController {

    private final MessageService messageService;

    /**
     * 构造函数注入消息服务
     *
     * @param messageService 消息服务
     */
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 分页查询当前用户消息
     *
     * @param loginUser 当前登录用户
     * @param bizType   业务类型，可为空
     * @param isRead    已读状态，可为空
     * @param pageNum   页码
     * @param pageSize  每页数量
     * @return 消息分页结果
     */
    @GetMapping("/list")
    public Result<MessagePageVO> list(@AuthenticationPrincipal LoginUser loginUser,
                                      @RequestParam(required = false) String bizType,
                                      @RequestParam(required = false) Integer isRead,
                                      @RequestParam(defaultValue = "1") int pageNum,
                                      @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(messageService.list(loginUser.getUserId(), bizType, isRead, pageNum, pageSize));
    }

    /**
     * 标记当前用户的一条消息为已读
     *
     * @param loginUser 当前登录用户
     * @param id        消息ID
     * @return 操作结果
     */
    @PostMapping("/{id}/read")
    public Result<Void> markRead(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        messageService.markRead(loginUser.getUserId(), id);
        return Result.success();
    }

    /**
     * 标记当前用户全部消息为已读
     *
     * @param loginUser 当前登录用户
     * @return 操作结果
     */
    @PostMapping("/read-all")
    public Result<Void> markAllRead(@AuthenticationPrincipal LoginUser loginUser) {
        messageService.markAllRead(loginUser.getUserId());
        return Result.success();
    }
}
