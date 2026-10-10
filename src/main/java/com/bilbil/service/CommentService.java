package com.bilbil.service;

import cn.hutool.core.util.StrUtil;
import com.bilbil.common.exception.BusinessException;
import com.bilbil.common.result.PageResult;
import com.bilbil.common.result.ResultCode;
import com.bilbil.dto.CommentPublishDTO;
import com.bilbil.entity.Comment;
import com.bilbil.entity.User;
import com.bilbil.entity.Video;
import com.bilbil.mapper.CommentMapper;
import com.bilbil.mapper.UserMapper;
import com.bilbil.mapper.VideoMapper;
import com.bilbil.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 评论服务（支持一级评论与子评论/回复）
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final VideoMapper videoMapper;
    private final NotifyService notifyService;

    /**
     * 发布评论（支持回复）
     */
    public CommentVO publish(Long userId, CommentPublishDTO dto) {
        Video video = videoMapper.selectById(dto.getVideoId());
        if (video == null) {
            throw new BusinessException(ResultCode.VIDEO_NOT_FOUND);
        }

        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        Long rootId = 0L;
        Long replyUserId = null;

        if (parentId != 0L) {
            // 回复评论：确定根评论ID与被回复人
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null || !parent.getVideoId().equals(dto.getVideoId())) {
                throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
            }
            rootId = parent.getRootId() == 0L ? parent.getId() : parent.getRootId();
            replyUserId = dto.getReplyUserId() != null ? dto.getReplyUserId() : parent.getUserId();
        }

        Comment comment = Comment.builder()
                .videoId(dto.getVideoId())
                .userId(userId)
                .parentId(parentId)
                .rootId(rootId)
                .replyUserId(replyUserId)
                .content(dto.getContent())
                .status(0)
                .build();
        commentMapper.insert(comment);
        videoMapper.increaseCommentCount(dto.getVideoId());

        // 异步通知：一级评论通知视频作者，回复通知被回复人
        if (parentId == 0L) {
            notifyService.notifyComment(video.getUserId(), userId, dto.getVideoId(), dto.getContent());
        } else if (replyUserId != null) {
            notifyService.notifyReply(replyUserId, userId, dto.getVideoId(), dto.getContent());
        }
        return toVO(comment, Map.of(userId, userMapper.selectById(userId)));
    }

    /**
     * 删除评论（仅评论作者本人）
     */
    public void delete(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除他人评论");
        }
        commentMapper.softDelete(commentId);
        videoMapper.decreaseCommentCount(comment.getVideoId());
    }

    /**
     * 后台：按关键字分页查询全部评论
     */
    public PageResult<CommentVO> adminList(String keyword, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 50);
        long total = commentMapper.countAdmin(keyword);
        List<Comment> comments = commentMapper.selectAdminList(keyword, (p - 1) * s, s);
        Map<Long, User> userMap = loadUsers(comments);
        List<CommentVO> list = comments.stream().map(c -> toVO(c, userMap)).toList();
        return PageResult.of(total, p, s, list);
    }

    /**
     * 后台：删除违规评论（软删除，无需归属校验）
     */
    public void adminDelete(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        commentMapper.softDelete(commentId);
        videoMapper.decreaseCommentCount(comment.getVideoId());
    }

    /**
     * 分页查询一级评论
     */
    public PageResult<CommentVO> listTopLevel(Long videoId, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 50);
        long total = commentMapper.countTopLevel(videoId);
        List<Comment> comments = commentMapper.selectTopLevel(videoId, (p - 1) * s, s);
        Map<Long, User> userMap = loadUsers(comments);
        List<CommentVO> list = comments.stream().map(c -> {
            CommentVO vo = toVO(c, userMap);
            vo.setReplyCount(commentMapper.countReplies(videoId, c.getId()));
            return vo;
        }).toList();
        return PageResult.of(total, p, s, list);
    }

    /**
     * 分页查询某根评论下的回复
     */
    public PageResult<CommentVO> listReplies(Long videoId, Long rootId, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 50);
        long total = commentMapper.countReplies(videoId, rootId);
        List<Comment> comments = commentMapper.selectReplies(videoId, rootId, (p - 1) * s, s);
        Map<Long, User> userMap = loadUsers(comments);
        List<CommentVO> list = comments.stream().map(c -> toVO(c, userMap)).toList();
        return PageResult.of(total, p, s, list);
    }

    /** 批量加载评论相关的用户信息 */
    private Map<Long, User> loadUsers(List<Comment> comments) {
        List<Long> ids = comments.stream()
                .flatMap(c -> Stream.of(c.getUserId(), c.getReplyUserId()))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }

    /** Comment 实体转 CommentVO */
    private CommentVO toVO(Comment c, Map<Long, User> userMap) {
        User user = userMap.get(c.getUserId());
        User replyUser = c.getReplyUserId() == null ? null : userMap.get(c.getReplyUserId());
        return CommentVO.builder()
                .id(c.getId())
                .videoId(c.getVideoId())
                .userId(c.getUserId())
                .userName(user == null ? null : StrUtil.blankToDefault(user.getNickname(), user.getUsername()))
                .userAvatar(user == null ? null : user.getAvatar())
                .content(c.getContent())
                .parentId(c.getParentId())
                .rootId(c.getRootId())
                .replyUserId(c.getReplyUserId())
                .replyUserName(replyUser == null ? null : StrUtil.blankToDefault(replyUser.getNickname(), replyUser.getUsername()))
                .createTime(c.getCreateTime())
                .build();
    }
}
