package com.bilbil.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    /** 用户ID */
    private Long id;
    /** 用户名（登录账号） */
    private String username;
    /** 密码（BCrypt 加密） */
    private String password;
    /** 昵称 */
    private String nickname;
    /** 头像 URL */
    private String avatar;
    /** 性别：0未知 1男 2女 */
    private Integer gender;
    /** 生日 */
    private LocalDate birthday;
    /** 个性签名 */
    private String signature;
    /** 邮箱 */
    private String email;
    /** 手机号 */
    private String phone;
    /** 账号状态：0正常 1封禁 */
    private Integer status;
    /** 角色：0普通 1管理员 2商家 */
    private Integer role;
    /** 积分余额（商家投放广告用，管理员可充值） */
    private Long balance;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
