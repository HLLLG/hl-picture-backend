package com.hl.hlpicture.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.infrastructure.common.DeleteRequest;
import com.hl.hlpicture.interfaces.dto.user.UserLoginRequest;
import com.hl.hlpicture.interfaces.dto.user.UserQueryRequest;
import com.hl.hlpicture.interfaces.dto.user.UserRegisterRequest;
import com.hl.hlpicture.interfaces.vo.user.LoginUserVO;
import com.hl.hlpicture.interfaces.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
* @author hegl
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-12-16 00:53:25
*/
public interface UserApplicationService {

    /**
     * 注册用户
     * @return 新用户 id
     */
    Long registerUser(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     *
     * @return 脱敏后的用户信息
     */
    LoginUserVO loginUser(UserLoginRequest userLoginRequest, HttpServletRequest request);

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
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取用户查询wrapper
     *
     * @param queryRequest
     * @return
     */
    QueryWrapper<User> getUserQueryWrapper(UserQueryRequest queryRequest);

    User getUserById(long id);

    UserVO getUserVOById(long id);

    boolean deleteUser(DeleteRequest deleteRequest);

    void updateUser(User user);

    Page<UserVO> listUserVoByPage(UserQueryRequest userQueryRequest);

    List<User> listByIds(Set<Long> userIdSet);

    String getEncryptPassword(String userPassword);

    long saveUser(User userEntity);
}
