package com.bilbil.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 用户信息修改请求参数
 */
@Data
public class UpdateUserDTO {

    /** 昵称 */
    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;

    /** 性别：0未知 1男 2女 */
    @Min(value = 0, message = "性别取值不合法")
    @Max(value = 2, message = "性别取值不合法")
    private Integer gender;

    /** 生日 */
    private LocalDate birthday;

    /** 个性签名 */
    @Size(max = 200, message = "签名长度不能超过200")
    private String signature;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 手机号 */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
