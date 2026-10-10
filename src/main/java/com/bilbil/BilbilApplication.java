package com.bilbil;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 仿B站视频平台 启动类
 *
 * @author bilbil
 */
@SpringBootApplication
@MapperScan("com.bilbil.mapper")   // 扫描 MyBatis Mapper 接口
@EnableScheduling                 // 开启定时任务（计数异步落库等）
public class BilbilApplication {

    public static void main(String[] args) {
        SpringApplication.run(BilbilApplication.class, args);
    }
}
