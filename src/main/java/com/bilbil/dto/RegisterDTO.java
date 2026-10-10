package com.bilbil.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求参数
 */
@Data
public class RegisterDTO {

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需在3-20之间")
    private String username;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在6-32之间")
    private String password;

    /** 昵称（选填） */
    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;

    /** 邮箱（选填） */
    @Email(message = "邮箱格式不正确")
    private String email;
}
