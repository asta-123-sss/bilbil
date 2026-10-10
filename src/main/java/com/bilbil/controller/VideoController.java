package com.bilbil.controller;

import com.bilbil.common.result.PageResult;
import com.bilbil.common.result.Result;
import com.bilbil.common.util.UserContext;
import com.bilbil.dto.VideoMergeDTO;
import com.bilbil.dto.VideoQueryDTO;
import com.bilbil.dto.VideoUpdateDTO;
import com.bilbil.dto.VideoUploadInitDTO;
import com.bilbil.service.VideoService;
import com.bilbil.vo.UploadInitVO;
import com.bilbil.vo.VideoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 视频模块接口
 *
 * <p>核心为分块上传（初始化会话 → 上传分片 → 合并入库，支持断点续传与秒传）。
 * 另含封面上传、视频列表、详情、播放（计数）、我的投稿、修改、删除。</p>
 */
@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    /**
     * 初始化分块上传
     *
     * <p><code>POST /api/video/upload/init</code>，需登录。
     * 返回上传会话 uploadId 与已上传分片序号，客户端可据此实现断点续传；
     * 若整文件 MD5 已存在则返回 alreadyExists=true（秒传）。</p>
     */
    @PostMapping("/upload/init")
    public Result<UploadInitVO> initUpload(@Valid @RequestBody VideoUploadInitDTO dto) {
        return Result.success(videoService.initUpload(dto, UserContext.requireUserId()));
    }

    /**
     * 上传单个分片
     *
     * <p><code>POST /api/video/upload/chunk</code>，需登录，multipart/form-data。
     * 字段：uploadId、chunkIndex（从 0 开始）、chunkMd5（选填）、file（分片文件）。</p>
     */
    @PostMapping("/upload/chunk")
    public Result<Void> uploadChunk(@RequestParam("uploadId") String uploadId,
                                    @RequestParam("chunkIndex") Integer chunkIndex,
                                    @RequestParam(value = "chunkMd5", required = false) String chunkMd5,
                                    @RequestPart("file") MultipartFile file) {
        videoService.uploadChunk(uploadId, chunkIndex, chunkMd5, file);
        return Result.success();
    }

    /**
     * 合并分片并提交视频元数据
     *
     * <p><code>POST /api/video/upload/merge</code>，需登录。合并前会校验分片完整性。</p>
     *
     * @return 生成的视频ID
     */
    @PostMapping("/upload/merge")
    public Result<Long> merge(@Valid @RequestBody VideoMergeDTO dto) {
        return Result.success(videoService.merge(dto, UserContext.requireUserId()));
    }

    /**
     * 上传视频封面图
     *
     * <p><code>POST /api/video/cover</code>，需登录，multipart/form-data，字段名 <code>file</code>。
     * 返回封面 URL，供合并接口的 coverUrl 使用。</p>
     */
    @PostMapping("/cover")
    public Result<String> uploadCover(@RequestPart("file") MultipartFile file) {
        return Result.success(videoService.uploadCover(file));
    }

    /**
     * 视频列表（仅公开且已发布的视频）
     *
     * <p><code>GET /api/video/list</code>，公开接口。
     * 支持 query 参数：categoryId、keyword、orderBy（new 最新 / hot 最热）、page、size。</p>
     */
    @GetMapping("/list")
    public Result<PageResult<VideoVO>> list(VideoQueryDTO query) {
        return Result.success(videoService.list(query, UserContext.getUserId()));
    }

    /**
     * 个性化推荐（基于用户历史行为的分类偏好）
     *
     * <p><code>GET /api/video/recommend</code>，公开接口，query 参数：page、size。
     * 登录用户返回偏好分类下的热门视频，未登录/无行为退化为「最热」。</p>
     */
    @GetMapping("/recommend")
    public Result<PageResult<VideoVO>> recommend(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "12") int size) {
        return Result.success(videoService.recommend(UserContext.getUserId(), page, size));
    }

    /**
     * 视频详情
     *
     * <p><code>GET /api/video/detail/{id}</code>，公开接口。
     * 热点视频基础信息走 Redis 缓存，计数与登录用户互动状态实时计算。</p>
     */
    @GetMapping("/detail/{id}")
    public Result<VideoVO> detail(@PathVariable("id") Long id) {
        return Result.success(videoService.detail(id, UserContext.getUserId()));
    }

    /**
     * 视频播放（播放量 +1）
     *
     * <p><code>GET /api/video/play/{id}</code>，公开接口。播放量走 Redis 原子计数、异步落库。</p>
     */
    @GetMapping("/play/{id}")
    public Result<VideoVO> play(@PathVariable("id") Long id) {
        return Result.success(videoService.play(id, UserContext.getUserId()));
    }

    /**
     * 我投稿的视频列表
     *
     * <p><code>GET /api/video/my</code>，需登录，query 参数：page、size。</p>
     */
    @GetMapping("/my")
    public Result<PageResult<VideoVO>> myVideos(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return Result.success(videoService.myVideos(UserContext.requireUserId(), page, size));
    }

    /**
     * 修改视频信息（仅作者本人）
     *
     * <p><code>PUT /api/video/{id}</code>，需登录。</p>
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id, @Valid @RequestBody VideoUpdateDTO dto) {
        videoService.update(id, dto, UserContext.requireUserId());
        return Result.success();
    }

    /**
     * 下架/删除视频（仅作者本人）
     *
     * <p><code>DELETE /api/video/{id}</code>，需登录。</p>
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        videoService.delete(id, UserContext.requireUserId());
        return Result.success();
    }
}
