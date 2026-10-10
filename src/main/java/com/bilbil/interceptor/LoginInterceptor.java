package com.bilbil.interceptor;

import cn.hutool.core.util.StrUtil;
import com.bilbil.common.constant.RedisKeyConstant;
import com.bilbil.common.exception.BusinessException;
import com.bilbil.common.result.ResultCode;
import com.bilbil.common.util.UserContext;
import com.bilbil.config.BilbilProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.List;

/**
 * 登录拦截器
 * <p>基于 Redis token 会话校验：
 * 1. 白名单路径（公开接口）—— 若携带有效 token 则解析出用户ID放入上下文（可选登录），不强制登录；
 * 2. 其他路径 —— 必须登录，否则返回 401。</p>
 */
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final BilbilProperties properties;

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** 公开接口白名单（无需登录） */
    private static final List<String> WHITE_LIST = List.of(
            "/api/user/register",      // 注册
            "/api/user/login",         // 登录
            "/api/user/profile/**",    // 查看他人主页
            "/api/category/**",        // 分区列表
            "/api/video/list",         // 视频列表
            "/api/video/recommend",    // 个性化推荐（未登录退化为热门）
            "/api/video/detail/**",    // 视频详情
            "/api/video/play/**",      // 视频播放信息
            "/api/comment/list",       // 评论列表
            "/api/comment/replies",    // 评论回复列表
            "/api/danmaku/list",       // 弹幕列表
            "/api/ad/feed",            // 信息流广告（兴趣定向投放）
            "/api/ad/*/click"          // 广告点击
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // CORS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        String token = request.getHeader("token");

        // 尝试解析 token（可能为空）
        Long userId = resolveUserId(token);

        if (userId != null) {
            // 有效登录，写入上下文，并滑动续期
            UserContext.setUserId(userId);
            redisTemplate.expire(RedisKeyConstant.tokenKey(token), Duration.ofDays(properties.getToken().getExpireDays()));
            return true;
        }

        // 未登录：白名单放行，否则拒绝
        if (isWhiteList(path)) {
            return true;
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理 ThreadLocal，防止线程池复用导致数据串号
        UserContext.clear();
    }

    /** 根据 token 解析用户ID，无效返回 null */
    private Long resolveUserId(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        String userId = redisTemplate.opsForValue().get(RedisKeyConstant.tokenKey(token));
        return StrUtil.isBlank(userId) ? null : Long.valueOf(userId);
    }

    /** 判断是否为公开白名单路径 */
    private boolean isWhiteList(String path) {
        for (String pattern : WHITE_LIST) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
