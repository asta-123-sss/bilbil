package com.bilbil.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户信息返回对象（不含密码等敏感字段）
 */
@Data
@Builder
public class UserVO implements Serializable {

    /** 用户ID */
    private Long id;
    /** 用户名 */
    private String username;
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
    /** 关注数 */
    private Long followCount;
    /** 粉丝数 */
    private Long fanCount;
    /** 角色：0普通 1管理员 2商家（登录用户本人返回） */
    private Integer role;
    /** 积分余额（仅本人返回，商家投放广告用） */
    private Long balance;
    /** 账号状态：0正常 1封禁 */
    private Integer status;
    /** 注册时间 */
    private LocalDateTime createTime;
    /** 当前登录用户是否已关注该用户（可选，登录后返回） */
    private Boolean followed;
}
