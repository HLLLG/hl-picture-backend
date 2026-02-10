package com.hl.hlpicture.domain.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.interfaces.dto.user.UserQueryRequest;
import com.hl.hlpicture.interfaces.dto.user.VipCode;
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
public interface UserDomainService {

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
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取用户查询wrapper
     *
     * @param queryRequest
     * @return
     */
    QueryWrapper<User> getUserQueryWrapper(UserQueryRequest queryRequest);

    String getEncryptPassword(String userPassword);

    Long addUser(User user);

    Boolean removeById(Long id);

    boolean updateById(User user);

    User getById(long id);

    Page<User> page(Page<User> userPage, QueryWrapper<User> querywrapper);

    List<User> listByIds(Set<Long> userIdset);

    /**
     * 校验并标记 VIP 码
     *
     * @param vipCode
     * @return
     */
    VipCode validateAndMarkVipCode(String vipCode);

    /**
     * 更新用户 VIP 信息
     * @param user
     * @param code
     */
    void updateUserVipInfo(User user, String code);
}
