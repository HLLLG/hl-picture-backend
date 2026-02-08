package com.hl.hlpicture.interfaces.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求参数
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 13603554636914017L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 校验密码
     */
    private String checkPassword;
}
