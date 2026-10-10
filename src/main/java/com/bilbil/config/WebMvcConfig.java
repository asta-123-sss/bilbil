package com.bilbil.config;

import com.bilbil.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 配置：全局跨域、登录拦截器、本地静态资源映射
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final BilbilProperties properties;

    /**
     * 全局跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")          // 允许所有来源（生产环境应限定具体域名）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 注册登录拦截器（拦截所有 /api/** 路径，白名单在拦截器内部判断）
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor).addPathPatterns("/api/**");
    }

    /**
     * 本地文件存储的静态资源映射
     * <p>将 /upload/** 映射到本地存储目录，供视频/图片/封面直接访问（开发环境替代 Nginx）</p>
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String localPath = properties.getStorage().getLocalPath();
        // 规范化为绝对路径 URI，例如 file:///E:/SpringBoot-bilbil/upload/
        String uri = Paths.get(localPath).toAbsolutePath().normalize().toUri().toString();
        if (!uri.endsWith("/")) {
            uri += "/";
        }
        registry.addResourceHandler("/upload/**").addResourceLocations(uri);
    }
}
