package com.bilbil.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 前端单页应用（Vue history 路由）回退控制器
 *
 * <p>生产环境把前端构建产物打进 jar 的 static 目录后，由 Spring Boot 直接托管。
 * Vue Router 使用 history 模式，用户刷新或直达 /video/123 等深层路径时后端并无
 * 对应接口，会命中 404。这里把这些前端路由统一 forward 到 /index.html，交回前端
 * 路由处理。</p>
 *
 * <p>只枚举前端实际路由，不会影响 /api/**（接口）、/assets/**（静态资源，带扩展名
 * 不会命中 {@code [^\.]*}）与 /upload/**（视频/封面文件）。</p>
 */
@Controller
public class SpaController {

    @GetMapping(value = {
            "/",                         // 首页
            "/{path:[^\\.]*}",           // 单级路由：login/register/upload/me/message/history/pm/admin
            "/video/{id:[0-9]+}",        // 播放页深链
            "/user/{id:[0-9]+}"          // 他人主页深链
    })
    public String forward() {
        return "forward:/index.html";
    }
}
