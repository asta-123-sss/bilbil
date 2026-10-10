package com.bilbil.controller;

import com.bilbil.common.result.PageResult;
import com.bilbil.common.result.Result;
import com.bilbil.common.util.UserContext;
import com.bilbil.dto.CommentPublishDTO;
import com.bilbil.service.CommentService;
import com.bilbil.vo.CommentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 评论模块接口
 *
 * <p>支持一级评论与子评论（回复）。回复通过 parentId 定位，rootId 记录所属根评论。</p>
 */
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 发布评论 / 回复
     *
     * <p><code>POST /api/comment/publish</code>，需登录。
     * 一级评论 parentId=0；回复他人时传 parentId 与 replyUserId（被回复人）。</p>
     */
    @PostMapping("/publish")
    public Result<CommentVO> publish(@Valid @RequestBody CommentPublishDTO dto) {
        return Result.success(commentService.publish(UserContext.requireUserId(), dto));
    }

    /**
     * 删除评论（仅评论作者本人）
     *
     * <p><code>DELETE /api/comment/{id}</code>，需登录。</p>
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id, UserContext.requireUserId());
        return Result.success();
    }

    /**
     * 一级评论列表（分页）
     *
     * <p><code>GET /api/comment/list</code>，公开接口，query 参数：videoId、page、size。</p>
     */
    @GetMapping("/list")
    public Result<PageResult<CommentVO>> list(@RequestParam Long videoId,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return Result.success(commentService.listTopLevel(videoId, page, size));
    }

    /**
     * 某根评论下的回复列表（分页）
     *
     * <p><code>GET /api/comment/replies</code>，公开接口，query 参数：videoId、rootId、page、size。</p>
     */
    @GetMapping("/replies")
    public Result<PageResult<CommentVO>> replies(@RequestParam Long videoId,
                                                 @RequestParam Long rootId,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return Result.success(commentService.listReplies(videoId, rootId, page, size));
    }
}
