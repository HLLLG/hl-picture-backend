package com.hl.hlpicturebackend.controller;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hl.hlpicturebackend.annotation.AuthCheck;
import com.hl.hlpicturebackend.common.BaseResponse;
import com.hl.hlpicturebackend.common.DeleteRequest;
import com.hl.hlpicturebackend.common.ResultUtils;
import com.hl.hlpicturebackend.constant.UserConstant;
import com.hl.hlpicturebackend.exception.ErrorCode;
import com.hl.hlpicturebackend.exception.ThrowUtils;
import com.hl.hlpicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.hl.hlpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.hl.hlpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.hl.hlpicturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.hl.hlpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.hl.hlpicturebackend.model.entity.SpaceUser;
import com.hl.hlpicturebackend.model.entity.User;
import com.hl.hlpicturebackend.model.vo.SpaceUserVO;
import com.hl.hlpicturebackend.service.PictureService;
import com.hl.hlpicturebackend.service.SpaceUserService;
import com.hl.hlpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 空间成员管理
 */
@RestController
@RequestMapping("/spaceUser")
public class SpaceUserController {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    /**
     * 添加成员到空间
     *
     * @param spaceuserAddRequest
     * @return
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceuserAddRequest) {
        // 校验请求参数
        ThrowUtils.throwIf(ObjUtil.isNull(spaceuserAddRequest), ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(spaceUserService.addSpaceUser(spaceuserAddRequest));
    }

    /**
     * 删除空间成员
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest) {
        // 校验请求参数
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Long id = deleteRequest.getId();
        // 判断是否存在
        SpaceUser spaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 执行删除
        boolean result = spaceUserService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "空间成员删除失败");
        return ResultUtils.success(true);
    }

    /**
     * 编辑成员信息(设置权限)
     *
     * @param spaceUserEditRequest
     * @return
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest) {
        // 校验请求参数
        ThrowUtils.throwIf(ObjUtil.isNull(spaceUserEditRequest) || spaceUserEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 将实体类和 DTO 进行转换
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserEditRequest, spaceUser);
        // 数据校验
        spaceUserService.validSpaceUser(spaceUser, false);
        // 判断空间是否存在
        SpaceUser oldSpaceUser = spaceUserService.getById(spaceUser.getId());
        ThrowUtils.throwIf(ObjUtil.isNull(oldSpaceUser), ErrorCode.NOT_FOUND_ERROR);
        // 执行更新
        boolean result = spaceUserService.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询某个成员在某个空间的信息
     *
     * @param spaceUserQueryRequest
     * @return
     */
    @PostMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        // 校验请求参数
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        // 操作数据库
        SpaceUser spaceUser = spaceUserService.getOne(spaceUserService.getSpaceUserQueryWrapper(spaceUserQueryRequest));
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询成员信息列表
     *
     * @param spaceUserQueryRequest
     * @return
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        // 校验请求参数
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 操作数据库
        List<SpaceUser> spaceUserList = spaceUserService.list(spaceUserService.getSpaceUserQueryWrapper(spaceUserQueryRequest));
        return ResultUtils.success(spaceUserService.getSpaceVOList(spaceUserList));
    }

    /**
     * 查询我加入的团队空间列表
     *
     * @param request
     * @return
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(userId);
        // 操作数据库
        List<SpaceUser> spaceUserList = spaceUserService.list(spaceUserService.getSpaceUserQueryWrapper(spaceUserQueryRequest));
        return ResultUtils.success(spaceUserService.getSpaceVOList(spaceUserList));
    }
}
