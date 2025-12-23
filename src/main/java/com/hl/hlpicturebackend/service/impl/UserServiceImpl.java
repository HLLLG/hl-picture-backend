package com.hl.hlpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hl.hlpicturebackend.common.DeleteRequest;
import com.hl.hlpicturebackend.constant.UserConstant;
import com.hl.hlpicturebackend.exception.BusinessException;
import com.hl.hlpicturebackend.exception.ErrorCode;
import com.hl.hlpicturebackend.exception.ThrowUtils;
import com.hl.hlpicturebackend.mapper.UserMapper;
import com.hl.hlpicturebackend.model.dto.user.UserQueryRequest;
import com.hl.hlpicturebackend.model.dto.user.UserUpdateRequest;
import com.hl.hlpicturebackend.model.vo.LoginUserVO;
import com.hl.hlpicturebackend.model.entity.User;
import com.hl.hlpicturebackend.model.enums.UserRoleEnum;
import com.hl.hlpicturebackend.model.vo.UserVO;
import com.hl.hlpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author hegl
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-12-16 00:53:25
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Override
    public Long registerUser(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4 ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入密码不一致");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 2. 检查账户是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        Long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 加密密码
        String encryptPassWord = getEncryptPassWord(userPassword);
        // 4， 写入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassWord);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    @Override
    public LoginUserVO loginUser(String userAccount, String userPassword, HttpServletRequest request) {
        // 1 校验参数
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR, "参数为空");
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "账号长度过短");
        ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "密码过短");
        // 2 加密密码
        String encryptPassWord = getEncryptPassWord(userPassword);
        // 3 根据账号获取用户信息
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 不存在，抛异常
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "账号不存在或密码错误");
        log.info("user login failed, userPassword cannot match userAccount");
        // 3 更新用户修改时间
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<User>().eq("editTime", DateTime.now());
        this.update(user, updateWrapper);
        // 4 脱敏
        return this.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 判断用户是否已登录
        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User curentUser = (User) attribute;
        ThrowUtils.throwIf(curentUser == null || curentUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        // 从数据库中查询（不追求性能，保证数据的最新状态）
        Long userId = curentUser.getId();
        curentUser = this.getById(userId);
        ThrowUtils.throwIf(curentUser == null, ErrorCode.NOT_LOGIN_ERROR);
        return curentUser;
    }

    @Override
    public boolean loginOutUser(HttpServletRequest request) {
        // 判断用户是否已登录
        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(attribute == null, ErrorCode.OPERATION_ERROR, "用户未登录");
        // 移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * 获取加密后的密码
     * @param password
     * @return
     */
    @Override
    public String getEncryptPassWord(String password) {
        // 加盐，混淆密码
        final String SALT = "hl";
        return DigestUtils.md5DigestAsHex(password.getBytes());
    }

    /**
     * 脱敏处理
     *
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getUserQueryWrapper(UserQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        Long id = queryRequest.getId();
        String userAccount = queryRequest.getUserAccount();
        String userName = queryRequest.getUserName();
        String userProfile = queryRequest.getUserProfile();
        String userRole = queryRequest.getUserRole();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StrUtil.isNotEmpty(userRole), "userRole", userRole);
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.like(StrUtil.isNotEmpty(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotEmpty(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotEmpty(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }
}




