package com.hl.hlpicture.interfaces.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hl.hlpicture.infrastructure.annotation.AuthCheck;
import com.hl.hlpicture.infrastructure.common.BaseResponse;
import com.hl.hlpicture.infrastructure.common.DeleteRequest;
import com.hl.hlpicture.infrastructure.common.ResultUtils;
import com.hl.hlpicture.interfaces.assembler.UserAssembler;
import com.hl.hlpicture.interfaces.dto.user.*;
import com.hl.hlpicture.domain.user.valueobject.UserConstant;
import com.hl.hlpicture.infrastructure.exception.ErrorCode;
import com.hl.hlpicture.infrastructure.exception.ThrowUtils;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.interfaces.vo.user.LoginUserVO;
import com.hl.hlpicture.interfaces.vo.user.UserVO;
import com.hl.hlpicture.application.service.UserApplicationService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = userApplicationService.registerUser(userRegisterRequest);
        return ResultUtils.success(id);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUserVO = userApplicationService.loginUser(userLoginRequest, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @GetMapping("/getLoginUser")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userApplicationService.getLoginUser(request);
        return ResultUtils.success(userApplicationService.getLoginUserVO(user));
    }

    /**
     * 注销用户
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> logoutUser(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userApplicationService.loginOutUser(request);
        return ResultUtils.success(result);
    }

    /**
     * 创建用户
     *
     * @param userAddRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/add")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User userEntity = UserAssembler.toUserEntity(userAddRequest);
        // 插入数据库
        return ResultUtils.success( userApplicationService.saveUser(userEntity));
    }

    /**
     * 删除用户
     *
     * @param deleteRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        boolean b = userApplicationService.deleteUser(deleteRequest);
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(userUpdateRequest == null || userUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User userEntity = UserAssembler.toUserEntity(userUpdateRequest);
        userApplicationService.updateUser(userEntity);
        return ResultUtils.success(true);
    }

    /**
     * 根据id获取用户
     *
     * @param id
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/get")
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userApplicationService.getUserById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据id获取脱敏用户信息
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userApplicationService.getUserVO(user));
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     * @param userQueryRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<UserVO> userVOPage = userApplicationService.listUserVoByPage(userQueryRequest);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 用户兑换vip
     */
    @PostMapping("/exchange/vip")
    public BaseResponse<Boolean> exchangeVip(@RequestBody VipExchangeRequest vipExchangeRequest,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(vipExchangeRequest == null, ErrorCode.PARAMS_ERROR);
        String vipCode = vipExchangeRequest.getVipCode();
        User user = userApplicationService.getLoginUser(request);
        boolean result = userApplicationService.exchangeVip(user, vipCode);
        return ResultUtils.success(result);
    }
}
