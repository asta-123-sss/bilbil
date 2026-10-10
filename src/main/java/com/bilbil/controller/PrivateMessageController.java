package com.bilbil.controller;

import com.bilbil.common.result.Result;
import com.bilbil.common.util.UserContext;
import com.bilbil.dto.SendPmDTO;
import com.bilbil.service.PrivateMessageService;
import com.bilbil.vo.ConversationVO;
import com.bilbil.vo.PrivateMessageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 私信接口（点对点）
 */
@RestController
@RequestMapping("/api/pm")
@RequiredArgsConstructor
public class PrivateMessageController {

    private final PrivateMessageService privateMessageService;

    /**
     * 发送私信
     *
     * <p><code>POST /api/pm/send</code>，需登录，body：{ toUserId, content }。</p>
     */
    @PostMapping("/send")
    public Result<PrivateMessageVO> send(@Valid @RequestBody SendPmDTO dto) {
        return Result.success(privateMessageService.send(UserContext.requireUserId(), dto));
    }

    /**
     * 会话列表
     *
     * <p><code>GET /api/pm/conversations</code>，需登录。</p>
     */
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> conversations() {
        return Result.success(privateMessageService.conversations(UserContext.requireUserId()));
    }

    /**
     * 与某人的聊天记录（时间正序，自动标记已读）
     *
     * <p><code>GET /api/pm/chat/{otherUserId}</code>，需登录。</p>
     */
    @GetMapping("/chat/{otherUserId}")
    public Result<List<PrivateMessageVO>> chat(@PathVariable Long otherUserId) {
        return Result.success(privateMessageService.chat(UserContext.requireUserId(), otherUserId));
    }

    /**
     * 未读私信总数
     *
     * <p><code>GET /api/pm/unread-count</code>，需登录。</p>
     */
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(privateMessageService.unreadCount(UserContext.requireUserId()));
    }

    /**
     * 将某会话标记为已读
     *
     * <p><code>PUT /api/pm/read/{otherUserId}</code>，需登录。</p>
     */
    @PutMapping("/read/{otherUserId}")
    public Result<Void> markRead(@PathVariable Long otherUserId) {
        privateMessageService.markRead(UserContext.requireUserId(), otherUserId);
        return Result.success();
    }
}
