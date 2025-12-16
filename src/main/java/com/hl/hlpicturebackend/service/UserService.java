package com.hl.hlpicturebackend.service;

import com.hl.hlpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author hegl
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-12-16 00:53:25
*/
public interface UserService extends IService<User> {

    /**
     *
     * @param userAccount 账户
     * @param userPassword 密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    Long registerUser(String userAccount, String userPassword, String checkPassword);

    String getEncryptPassWord(String password);
}
