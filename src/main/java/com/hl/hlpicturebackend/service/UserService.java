package com.hl.hlpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hl.hlpicturebackend.common.DeleteRequest;
import com.hl.hlpicturebackend.model.dto.user.UserQueryRequest;
import com.hl.hlpicturebackend.model.dto.user.UserUpdateRequest;
import com.hl.hlpicturebackend.model.vo.LoginUserVO;
import com.hl.hlpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hl.hlpicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author hegl
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-12-16 00:53:25
*/
public interface UserService extends IService<User> {

    /**
     * 注册用户
     * @param userAccount 账户
     * @param userPassword 密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    Long registerUser(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  账户
     * @param userPassword 密码
     * @return 脱敏后的用户信息
     */
    LoginUserVO loginUser(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 注销用户
     *
     * @param request
     */
    boolean loginOutUser(HttpServletRequest request);


    /**
     * 加密密码
     *
     * @param password
     * @return
     */
    String getEncryptPassWord(String password);


    /**
     * 获取脱敏后的用户信息
     *
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏后的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户信息列表
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVO(List<User> userList);

    /**
     * 获取用户查询wrapper
     *
     * @param queryRequest
     * @return
     */
    QueryWrapper<User> getUserQueryWrapper(UserQueryRequest queryRequest);

}
