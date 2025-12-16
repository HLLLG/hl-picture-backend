package com.hl.hlpicturebackend.model.dto;

import com.hl.hlpicturebackend.model.entity.User;

import java.io.Serializable;

public class LoginUserVO extends User implements Serializable {

    private static final long serialVersionUID = 6480359053773666735L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;
}
