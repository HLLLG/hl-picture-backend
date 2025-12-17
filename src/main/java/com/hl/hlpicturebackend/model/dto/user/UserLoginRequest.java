package com.hl.hlpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求参数
 */
@Data
public class UserLoginRequest implements Serializable {


    private static final long serialVersionUID = -4152241161199638021L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;
}
