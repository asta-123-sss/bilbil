package com.bilbil.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录返回对象
 */
@Data
@Builder
public class LoginVO implements Serializable {

    /** 登录 token（后续请求放入 header 的 token 字段） */
    private String token;
    /** 用户信息 */
    private UserVO user;
}
