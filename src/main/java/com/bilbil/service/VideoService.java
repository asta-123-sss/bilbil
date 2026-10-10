package com.bilbil.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.bilbil.common.constant.RedisKeyConstant;
import com.bilbil.common.constant.VideoConstant;
import com.bilbil.common.exception.BusinessException;
import com.bilbil.common.result.PageResult;
import com.bilbil.common.result.ResultCode;
import com.bilbil.common.util.BloomFilterUtil;
import com.bilbil.dto.ModerationResult;
import com.bilbil.dto.VideoMergeDTO;
import com.bilbil.dto.VideoQueryDTO;
import com.bilbil.dto.VideoUpdateDTO;
import com.bilbil.dto.VideoUploadInitDTO;
import com.bilbil.entity.User;
import com.bilbil.entity.Video;
import com.bilbil.entity.VideoCategory;
import com.bilbil.entity.VideoChunk;
import com.bilbil.mapper.UserMapper;
import com.bilbil.mapper.VideoChunkMapper;
import com.bilbil.mapper.VideoMapper;
import com.bilbil.mapper.WatchHistoryMapper;
import com.bilbil.mq.MqConstant;
import com.bilbil.mq.MqProducerService;
import com.bilbil.storage.FileStorageService;
import com.bilbil.vo.CategoryStatVO;
import com.bilbil.vo.UploadInitVO;
import com.bilbil.vo.VideoVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 视频服务（核心：分块上传/断点续传 + 缓存 + 计数）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoMapper videoMapper;
    private final VideoChunkMapper videoChunkMapper;
    private final UserMapper userMapper;
    private final WatchHistoryMapper watchHistoryMapper;
    private final FileStorageService fileStorageService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final BloomFilterUtil bloomFilterUtil;
    private final MqProducerService mqProducerService;
    private final CategoryService categoryService;
    private final LikeService likeService;
    private final CollectService collectService;
    private final CoinService coinService;
    private final FollowService followService;
    private final WatchHistoryService watchHistoryService;
    private final ContentModerationService moderationService;

    // ==================================================================
    // 分块上传 / 断点续传 / 合并
    // ==================================================================

    /**
     * 初始化分片上传：生成上传会话ID，返回已上传分片（断点续传）
     */
    public UploadInitVO initUpload(VideoUploadInitDTO dto, Long userId) {
        // 用 用户ID + 文件标识 生成确定性 uploadId，保证重复初始化拿到同一会话（支持断点续传）
        String identity = StrUtil.isNotBlank(dto.getFileMd5())
                ? dto.getFileMd5()
                : (dto.getFileName() + ":" + dto.getFileSize());
        String uploadId = DigestUtil.md5Hex(userId + "|" + identity);

        // 秒传：整个文件 MD5 已存在，直接返回已存在视频
        if (StrUtil.isNotBlank(dto.getFileMd5())) {
            Video exist = videoMapper.selectByFileMd5(dto.getFileMd5());
            if (exist != null) {
                return UploadInitVO.builder()
                        .uploadId(uploadId)
                        .alreadyExists(true)
                        .videoId(exist.getId())
                        .build();
            }
        }

        // 清理上一次已完成会话遗留的分片（已合并/已清理），避免同文件重新上传时数量对不上
        videoChunkMapper.deleteNonUploaded(uploadId);

        // 保存上传会话元数据到 Redis（合并阶段使用），24 小时有效
        try {
            Map<String, Object> meta = new HashMap<>();
            meta.put("fileName", dto.getFileName());
            meta.put("fileSize", dto.getFileSize());
            meta.put("chunkCount", dto.getChunkCount());
            meta.put("fileMd5", StrUtil.nullToEmpty(dto.getFileMd5()));
            redisTemplate.opsForValue().set(RedisKeyConstant.uploadMetaKey(uploadId),
                    objectMapper.writeValueAsString(meta), Duration.ofHours(24));
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "上传会话初始化失败");
        }

        // 查询已上传分片序号（断点续传：客户端只重传缺失分片）
        List<Integer> uploadedIndexes = videoChunkMapper.selectUploadedIndexes(uploadId);

        return UploadInitVO.builder()
                .uploadId(uploadId)
                .chunkSize(dto.getChunkSize())
                .chunkCount(dto.getChunkCount())
                .uploadedIndexes(uploadedIndexes)
                .alreadyExists(false)
                .build();
    }

    /**
     * 上传单个分片
     */
    public void uploadChunk(String uploadId, Integer chunkIndex, String chunkMd5, MultipartFile file) {
        if (StrUtil.isBlank(uploadId) || chunkIndex == null || chunkIndex < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上传参数不合法");
        }
        if (Boolean.FALSE.equals(redisTemplate.hasKey(RedisKeyConstant.uploadMetaKey(uploadId)))) {
            throw new BusinessException(ResultCode.UPLOAD_TASK_NOT_FOUND);
        }
        // 幂等：分片已上传则直接返回
        if (videoChunkMapper.selectByUploadIdAndIndex(uploadId, chunkIndex) != null) {
            return;
        }
        // 上传分片文件
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR);
        }
        String ext = StrUtil.blankToDefault(FileUtil.extName(file.getOriginalFilename()), "part");
        String fileId = fileStorageService.upload(bytes, ext);

        // 记录分片
        videoChunkMapper.insert(VideoChunk.builder()
                .uploadId(uploadId)
                .chunkIndex(chunkIndex)
                .chunkSize((long) bytes.length)
                .chunkMd5(chunkMd5)
                .fileId(fileId)
                .status(VideoConstant.ChunkStatus.UPLOADED)
                .build());
    }

    /**
     * 合并分片 + 提交视频元数据入库
     *
     * @return 生成的视频ID
     */
    public Long merge(VideoMergeDTO dto, Long userId) {
        // 1. 读取上传会话元数据
        String metaJson = redisTemplate.opsForValue().get(RedisKeyConstant.uploadMetaKey(dto.getUploadId()));
        if (StrUtil.isBlank(metaJson)) {
            throw new BusinessException(ResultCode.UPLOAD_TASK_NOT_FOUND);
        }
        JsonNode meta;
        try {
            meta = objectMapper.readTree(metaJson);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.UPLOAD_TASK_NOT_FOUND);
        }
        String fileName = meta.get("fileName").asText();
        String fileMd5 = meta.hasNonNull("fileMd5") ? meta.get("fileMd5").asText() : "";
        int chunkCount = meta.get("chunkCount").asInt();

        // 2. 查询分片并校验完整性
        List<VideoChunk> chunks = videoChunkMapper.selectByUploadId(dto.getUploadId());
        if (chunks.size() != chunkCount) {
            throw new BusinessException(ResultCode.VIDEO_CHUNK_NOT_COMPLETE);
        }
        for (int i = 0; i < chunks.size(); i++) {
            if (chunks.get(i).getChunkIndex() != i) {
                throw new BusinessException(ResultCode.VIDEO_CHUNK_NOT_COMPLETE);
            }
        }

        // 3. 按序号合并分片（生产环境建议流式合并到临时文件，避免大文件占用内存）
        byte[] mergedBytes = mergeChunks(chunks);

        // 4. 上传合并后的完整视频，得到访问地址
        String ext = StrUtil.blankToDefault(FileUtil.extName(fileName), "mp4");
        String fileId = fileStorageService.upload(mergedBytes, ext);
        String videoUrl = fileStorageService.getAccessUrl(fileId);

        // AI 内容审核（文字 + 封面图 + 视频抽帧）；不通过则清理已上传文件并驳回（未配 key 时内部自动放行）
        ModerationResult moderation = moderationService.moderateVideo(
                dto.getTitle(), dto.getDescription(), dto.getTags(), dto.getCoverUrl(), mergedBytes);
        if (!moderation.passed()) {
            fileStorageService.delete(fileId);
            throw new BusinessException(ResultCode.CONTENT_REJECTED,
                    "内容审核未通过：" + StrUtil.blankToDefault(moderation.reason(), "疑似违规内容"));
        }

        // 5. 视频元数据入库（初始状态：转码中）
        Integer visibility = dto.getVisibility() == null ? VideoConstant.Visibility.PUBLIC : dto.getVisibility();
        Integer duration = dto.getDuration() == null ? 0 : dto.getDuration();
        Video video = Video.builder()
                .userId(userId)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .categoryId(dto.getCategoryId())
                .tags(dto.getTags())
                .coverUrl(dto.getCoverUrl())
                .videoUrl(videoUrl)
                .fileId(fileId)
                .fileMd5(StrUtil.emptyToNull(fileMd5))
                .fileName(fileName)
                .fileSize((long) mergedBytes.length)
                .duration(duration)
                .visibility(visibility)
                .status(VideoConstant.Status.TRANSCODING)
                .playCount(0L)
                .likeCount(0L)
                .collectCount(0L)
                .commentCount(0L)
                .danmakuCount(0L)
                .build();
        videoMapper.insert(video);

        // 6. 标记分片已合并，并清理分片文件
        videoChunkMapper.markMerged(dto.getUploadId(), video.getId());
        chunks.forEach(c -> {
            try {
                fileStorageService.delete(c.getFileId());
            } catch (Exception ignored) {
                // 清理失败不影响主流程
            }
        });

        // 7. 新视频ID加入布隆过滤器（防缓存穿透）
        bloomFilterUtil.add(RedisKeyConstant.VIDEO_BLOOM_KEY, video.getId());

        // 8. 发送转码消息：MQ 启用则异步转码后发布，否则直接发布（无 broker 环境）
        if (mqProducerService.isEnabled()) {
            mqProducerService.send(MqConstant.TOPIC_VIDEO, MqConstant.TAG_TRANSCODE,
                    "{\"videoId\":" + video.getId() + "}");
        } else {
            videoMapper.updateStatus(video.getId(), VideoConstant.Status.PUBLISHED);
        }

        return video.getId();
    }

    /** 按序号合并所有分片字节 */
    private byte[] mergeChunks(List<VideoChunk> chunks) {
        long total = chunks.stream().mapToLong(c -> c.getChunkSize() == null ? 0 : c.getChunkSize()).sum();
        if (total > Integer.MAX_VALUE) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR, "文件过大，暂不支持一次性内存合并");
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream((int) total)) {
            for (VideoChunk chunk : chunks) {
                byte[] data = fileStorageService.download(chunk.getFileId());
                out.write(data, 0, data.length);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR, "分片合并失败");
        }
    }

    /**
     * 上传封面图，返回封面访问 URL
     */
    public String uploadCover(MultipartFile file) {
        try {
            String ext = StrUtil.blankToDefault(FileUtil.extName(file.getOriginalFilename()), "jpg");
            String fileId = fileStorageService.upload(file.getBytes(), ext);
            return fileStorageService.getAccessUrl(fileId);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR);
        }
    }

    // ==================================================================
    // 视频查询 / 播放
    // ==================================================================

    /**
     * 视频列表（仅公开且已发布的视频）
     */
    public PageResult<VideoVO> list(VideoQueryDTO query, Long currentUserId) {
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getSize() == null || query.getSize() < 1 ? 10 : Math.min(query.getSize(), 100);

        Map<String, Object> params = new HashMap<>();
        params.put("categoryId", query.getCategoryId());
        params.put("keyword", query.getKeyword());
        params.put("status", VideoConstant.Status.PUBLISHED);
        params.put("visibility", VideoConstant.Visibility.PUBLIC);
        params.put("orderBy", query.getOrderBy());
        params.put("offset", (page - 1) * size);
        params.put("size", size);

        long total = videoMapper.count(params);
        List<Video> videos = videoMapper.selectList(params);
        return PageResult.of(total, page, size, toVOList(videos, currentUserId));
    }

    /**
     * 个性化推荐：基于用户历史行为（观看/点赞/收藏/投币/关注）的加权分类偏好，
     * 推荐偏好分类下的热门视频。未登录 / 冷启动（无任何行为）退化为「最热」。
     */
    public PageResult<VideoVO> recommend(Long userId, int page, int size) {
        page = page < 1 ? 1 : page;
        size = size < 1 ? 10 : Math.min(size, 100);

        // 未登录 → 热门兜底
        if (userId == null) {
            return hotFallback(page, size, null);
        }

        // 用户分类偏好统计；冷启动（无任何行为）→ 热门兜底
        List<CategoryStatVO> stats = videoMapper.selectUserCategoryStats(userId);
        if (stats == null || stats.isEmpty()) {
            return hotFallback(page, size, userId);
        }

        // 取偏好最高的前 3 个分类，排除已观看视频与自己投稿
        List<Long> categoryIds = stats.stream().limit(3).map(CategoryStatVO::getCategoryId).toList();
        List<Long> watchedIds = watchHistoryMapper.selectVideoIdsByUserId(userId);

        Map<String, Object> params = new HashMap<>();
        params.put("categoryIds", categoryIds);
        params.put("excludeIds", watchedIds == null ? List.of() : watchedIds);
        params.put("excludeUserId", userId);
        params.put("offset", (page - 1) * size);
        params.put("size", size);

        long total = videoMapper.countRecommendCandidates(params);
        // 偏好分类下的视频都已看完 → 热门兜底，避免空页
        if (total == 0) {
            return hotFallback(page, size, userId);
        }

        List<Video> videos = videoMapper.selectRecommendCandidates(params);
        return PageResult.of(total, page, size, toVOList(videos, userId));
    }

    /** 热门列表兜底（复用 list 的 orderBy=hot 逻辑） */
    private PageResult<VideoVO> hotFallback(int page, int size, Long userId) {
        VideoQueryDTO hot = new VideoQueryDTO();
        hot.setOrderBy("hot");
        hot.setPage(page);
        hot.setSize(size);
        return list(hot, userId);
    }

    /** 批量解析分区名与作者信息并组装 VO（列表/推荐共用，避免 N+1） */
    private List<VideoVO> toVOList(List<Video> videos, Long currentUserId) {
        Map<Long, String> categoryMap = categoryService.list().stream()
                .collect(Collectors.toMap(VideoCategory::getId, VideoCategory::getName, (a, b) -> a));
        List<Long> authorIds = videos.stream().map(Video::getUserId).distinct().toList();
        Map<Long, User> authorMap = authorIds.isEmpty() ? Map.of()
                : userMapper.selectByIds(authorIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        return videos.stream()
                .map(v -> toVO(v, currentUserId, categoryMap.get(v.getCategoryId()), authorMap.get(v.getUserId())))
                .toList();
    }

    /**
     * 视频详情（Redis 缓存热点视频基础信息 + 布隆过滤器防穿透 + 实时计数）
     */
    public VideoVO detail(Long videoId, Long currentUserId) {
        // 1. 布隆过滤器：拦截一定不存在的ID，防缓存穿透
        if (!bloomFilterUtil.mightContain(RedisKeyConstant.VIDEO_BLOOM_KEY, videoId)) {
            throw new BusinessException(ResultCode.VIDEO_NOT_FOUND);
        }

        VideoVO vo = null;
        Video video = null;
        String cacheKey = RedisKeyConstant.videoInfoKey(videoId);

        // 2. 查缓存
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
            try {
                vo = objectMapper.readValue(cached, VideoVO.class);
            } catch (Exception e) {
                log.warn("视频缓存反序列化失败，回源数据库 videoId={}", videoId);
            }
        }

        // 3. 缓存未命中：查库并缓存（仅缓存公开已发布视频，避免泄露私密视频）
        if (vo == null) {
            video = videoMapper.selectById(videoId);
            if (video == null) {
                throw new BusinessException(ResultCode.VIDEO_NOT_FOUND);
            }
            checkVisibility(video, currentUserId);
            vo = toVO(video, null);
            if (VideoConstant.Status.PUBLISHED == video.getStatus()
                    && VideoConstant.Visibility.PUBLIC == video.getVisibility()) {
                // 随机过期时间，避免缓存雪崩
                Duration ttl = Duration.ofMinutes(10).plusSeconds(ThreadLocalRandom.current().nextInt(60));
                try {
                    redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(vo), ttl);
                } catch (Exception e) {
                    log.warn("视频缓存写入失败 videoId={}", videoId);
                }
            }
        }

        // 4. 刷新实时计数（点赞/收藏/投币/播放不走基础信息缓存，保证实时准确）
        vo.setLikeCount(likeService.getLikeCount(videoId));
        vo.setCollectCount(collectService.getCollectCount(videoId));
        vo.setCoinCount(coinService.getCoinCount(videoId));
        vo.setPlayCount(resolvePlayCount(videoId, video));

        // 5. 登录用户补充互动状态（不缓存，实时计算）
        if (currentUserId != null) {
            vo.setLiked(likeService.isLiked(videoId, currentUserId));
            vo.setCollected(collectService.isCollected(videoId, currentUserId));
            vo.setCoined(coinService.isCoined(videoId, currentUserId));
            if (vo.getAuthorId() != null) {
                vo.setFollowed(followService.isFollowing(currentUserId, vo.getAuthorId()));
            }
        }
        return vo;
    }

    /**
     * 视频播放：播放量 +1（Redis 原子计数，异步落库），返回播放信息
     */
    public VideoVO play(Long videoId, Long currentUserId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new BusinessException(ResultCode.VIDEO_NOT_FOUND);
        }
        checkVisibility(video, currentUserId);

        // 登录用户记录观看历史（进度 0，后续由前端在暂停/离开时上报真实进度）
        if (currentUserId != null) {
            watchHistoryService.record(currentUserId, videoId, 0);
        }

        // 播放量原子自增（异步落库由定时任务完成）
        String playKey = RedisKeyConstant.videoPlayCountKey(videoId);
        redisTemplate.opsForValue().setIfAbsent(playKey, String.valueOf(video.getPlayCount() == null ? 0 : video.getPlayCount()));
        Long count = redisTemplate.opsForValue().increment(playKey);
        redisTemplate.opsForSet().add(RedisKeyConstant.VIDEO_DIRTY_SET_KEY, String.valueOf(videoId));

        VideoVO vo = toVO(video, currentUserId);
        vo.setPlayCount(count);
        return vo;
    }

    /**
     * 我投稿的视频列表
     */
    public PageResult<VideoVO> myVideos(Long userId, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("offset", (p - 1) * s);
        params.put("size", s);

        long total = videoMapper.count(params);
        List<Video> videos = videoMapper.selectList(params);

        Map<Long, String> categoryMap = categoryService.list().stream()
                .collect(Collectors.toMap(VideoCategory::getId, VideoCategory::getName, (a, b) -> a));
        User author = userMapper.selectById(userId);
        List<VideoVO> voList = videos.stream()
                .map(v -> toVO(v, userId, categoryMap.get(v.getCategoryId()), author))
                .toList();
        return PageResult.of(total, p, s, voList);
    }

    /**
     * 后台：按状态/关键字分页查询视频（不限可见性，用于审核）
     */
    public PageResult<VideoVO> adminList(Integer status, String keyword, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("keyword", keyword);
        params.put("offset", (p - 1) * s);
        params.put("size", s);

        long total = videoMapper.count(params);
        List<Video> videos = videoMapper.selectList(params);

        Map<Long, String> categoryMap = categoryService.list().stream()
                .collect(Collectors.toMap(VideoCategory::getId, VideoCategory::getName, (a, b) -> a));
        List<Long> authorIds = videos.stream().map(Video::getUserId).distinct().toList();
        Map<Long, User> authorMap = authorIds.isEmpty() ? Map.of()
                : userMapper.selectByIds(authorIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        List<VideoVO> voList = videos.stream()
                .map(v -> toVO(v, null, categoryMap.get(v.getCategoryId()), authorMap.get(v.getUserId())))
                .toList();
        return PageResult.of(total, p, s, voList);
    }

    /**
     * 后台：上架/下架视频（status 1上架 2下架），并失效缓存
     */
    public void adminUpdateStatus(Long videoId, Integer status) {
        if (videoMapper.selectById(videoId) == null) {
            throw new BusinessException(ResultCode.VIDEO_NOT_FOUND);
        }
        videoMapper.updateStatus(videoId, status);
        redisTemplate.delete(RedisKeyConstant.videoInfoKey(videoId));
    }

    /**
     * 修改视频元数据（仅作者本人）
     */
    public void update(Long videoId, VideoUpdateDTO dto, Long userId) {
        Video video = requireOwnedVideo(videoId, userId);
        videoMapper.update(Video.builder()
                .id(video.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .categoryId(dto.getCategoryId())
                .tags(dto.getTags())
                .coverUrl(dto.getCoverUrl())
                .visibility(dto.getVisibility())
                .build());
        // 使缓存失效
        redisTemplate.delete(RedisKeyConstant.videoInfoKey(videoId));
    }

    /**
     * 下架/删除视频（仅作者本人）
     */
    public void delete(Long videoId, Long userId) {
        requireOwnedVideo(videoId, userId);
        videoMapper.updateStatus(videoId, VideoConstant.Status.OFF_SHELF);
        redisTemplate.delete(RedisKeyConstant.videoInfoKey(videoId));
    }

    // ==================================================================
    // 内部辅助
    // ==================================================================

    /** 校验视频归属，返回视频（非作者抛异常） */
    private Video requireOwnedVideo(Long videoId, Long userId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new BusinessException(ResultCode.VIDEO_NOT_FOUND);
        }
        if (!video.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作他人视频");
        }
        return video;
    }

    /** 校验视频可见权限与状态 */
    private void checkVisibility(Video video, Long currentUserId) {
        boolean isOwner = video.getUserId().equals(currentUserId);
        // 非作者只能看已发布视频
        if (!isOwner && video.getStatus() != VideoConstant.Status.PUBLISHED) {
            throw new BusinessException(ResultCode.VIDEO_NOT_FOUND);
        }
        if (isOwner) {
            return;
        }
        // 私密视频仅作者可见
        if (video.getVisibility() == VideoConstant.Visibility.PRIVATE) {
            throw new BusinessException(ResultCode.VIDEO_NOT_FOUND);
        }
        // 仅粉丝可见
        if (video.getVisibility() == VideoConstant.Visibility.FANS_ONLY
                && (currentUserId == null || !followService.isFollowing(currentUserId, video.getUserId()))) {
            throw new BusinessException(ResultCode.VIDEO_NOT_FOUND);
        }
    }

    /** 组装 VideoVO（自动解析分区名与作者信息） */
    private VideoVO toVO(Video v, Long currentUserId) {
        VideoCategory category = categoryService.getById(v.getCategoryId());
        User author = userMapper.selectById(v.getUserId());
        return toVO(v, currentUserId, category == null ? null : category.getName(), author);
    }

    /** 组装 VideoVO（实时计数 + 作者信息 + 可选互动状态） */
    private VideoVO toVO(Video v, Long currentUserId, String categoryName, User author) {
        return VideoVO.builder()
                .id(v.getId())
                .title(v.getTitle())
                .description(v.getDescription())
                .categoryId(v.getCategoryId())
                .categoryName(categoryName)
                .tags(v.getTags())
                .coverUrl(v.getCoverUrl())
                .videoUrl(v.getVideoUrl())
                .duration(v.getDuration())
                .visibility(v.getVisibility())
                .status(v.getStatus())
                .playCount(resolvePlayCount(v.getId(), v))
                .likeCount(likeService.getLikeCount(v.getId()))
                .collectCount(collectService.getCollectCount(v.getId()))
                .coinCount(coinService.getCoinCount(v.getId()))
                .commentCount(v.getCommentCount())
                .danmakuCount(v.getDanmakuCount())
                .createTime(v.getCreateTime())
                .authorId(author == null ? null : author.getId())
                .authorName(author == null ? null : StrUtil.blankToDefault(author.getNickname(), author.getUsername()))
                .authorAvatar(author == null ? null : author.getAvatar())
                .liked(currentUserId == null ? null : likeService.isLiked(v.getId(), currentUserId))
                .collected(currentUserId == null ? null : collectService.isCollected(v.getId(), currentUserId))
                .coined(currentUserId == null ? null : coinService.isCoined(v.getId(), currentUserId))
                .followed(currentUserId == null || author == null ? null
                        : followService.isFollowing(currentUserId, author.getId()))
                .build();
    }

    /** 读取播放量：优先 Redis 计数器，缺失则用 DB 值回填 */
    private long resolvePlayCount(Long videoId, Video video) {
        String key = RedisKeyConstant.videoPlayCountKey(videoId);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return Long.parseLong(cached);
        }
        Video v = video != null ? video : videoMapper.selectById(videoId);
        long db = v == null || v.getPlayCount() == null ? 0 : v.getPlayCount();
        redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(db));
        return db;
    }
}
