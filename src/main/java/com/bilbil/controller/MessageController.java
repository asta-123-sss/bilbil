package com.bilbil.controller;

import com.bilbil.common.result.PageResult;
import com.bilbil.common.result.Result;
import com.bilbil.common.util.UserContext;
import com.bilbil.service.MessageService;
import com.bilbil.vo.MessageNotifyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 消息通知接口
 *
 * <p>消息（点赞/评论/回复/关注/系统）由 NotifyService 生产，通过 RocketMQ 异步投递（未启用时直接落库）。</p>
 */
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 分页查询我的消息列表
     *
     * <p><code>GET /api/message/list</code>，需登录，query 参数：page、size。</p>
     */
    @GetMapping("/list")
    public Result<PageResult<MessageNotifyVO>> list(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return Result.success(messageService.list(UserContext.requireUserId(), page, size));
    }

    /**
     * 未读消息数
     *
     * <p><code>GET /api/message/unread-count</code>，需登录。</p>
     */
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(messageService.unreadCount(UserContext.requireUserId()));
    }

    /**
     * 标记单条消息已读
     *
     * <p><code>PUT /api/message/read/{id}</code>，需登录。</p>
     */
    @PutMapping("/read/{id}")
    public Result<Void> markRead(@PathVariable Long id) {
        messageService.markRead(UserContext.requireUserId(), id);
        return Result.success();
    }

    /**
     * 标记全部消息已读
     *
     * <p><code>PUT /api/message/read-all</code>，需登录。</p>
     */
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        messageService.markAllRead(UserContext.requireUserId());
        return Result.success();
    }
}
