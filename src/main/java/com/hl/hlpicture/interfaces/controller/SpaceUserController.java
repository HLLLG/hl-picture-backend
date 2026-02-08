package com.hl.hlpicture.interfaces.controller;

import cn.hutool.core.util.ObjUtil;
import com.hl.hlpicture.application.service.SpaceUserApplicationService;
import com.hl.hlpicture.application.service.UserApplicationService;
import com.hl.hlpicture.domain.space.entity.SpaceUser;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.infrastructure.common.BaseResponse;
import com.hl.hlpicture.infrastructure.common.DeleteRequest;
import com.hl.hlpicture.infrastructure.common.ResultUtils;
import com.hl.hlpicture.infrastructure.exception.ErrorCode;
import com.hl.hlpicture.infrastructure.exception.ThrowUtils;
import com.hl.hlpicture.interfaces.assembler.SpaceUserAssembler;
import com.hl.hlpicture.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.hl.hlpicture.interfaces.dto.spaceuser.SpaceUserEditRequest;
import com.hl.hlpicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.hl.hlpicture.interfaces.vo.space.SpaceUserVO;
import com.hl.hlpicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.hl.hlpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 空间成员管理
 */
@RestController
@RequestMapping("/spaceUser")
public class SpaceUserController {

    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

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
        return ResultUtils.success(spaceUserApplicationService.addSpaceUser(spaceuserAddRequest));
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
        SpaceUser spaceUser = spaceUserApplicationService.getById(id);
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 执行删除
        boolean result = spaceUserApplicationService.removeById(id);
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
        SpaceUser spaceUser = SpaceUserAssembler.toSpaceUserEntity(spaceUserEditRequest);
        // 数据校验
        spaceUserApplicationService.validSpaceUser(spaceUser, false);
        // 判断空间是否存在
        SpaceUser oldSpaceUser = spaceUserApplicationService.getById(spaceUser.getId());
        ThrowUtils.throwIf(ObjUtil.isNull(oldSpaceUser), ErrorCode.NOT_FOUND_ERROR);
        // 执行更新
        boolean result = spaceUserApplicationService.updateById(spaceUser);
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
        SpaceUser spaceUser = spaceUserApplicationService.getOne(spaceUserApplicationService.getSpaceUserQueryWrapper(spaceUserQueryRequest));
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
        List<SpaceUser> spaceUserList = spaceUserApplicationService.list(spaceUserApplicationService.getSpaceUserQueryWrapper(spaceUserQueryRequest));
        return ResultUtils.success(spaceUserApplicationService.getSpaceVOList(spaceUserList));
    }

    /**
     * 查询我加入的团队空间列表
     *
     * @param request
     * @return
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        Long userId = loginUser.getId();
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(userId);
        // 操作数据库
        List<SpaceUser> spaceUserList = spaceUserApplicationService.list(spaceUserApplicationService.getSpaceUserQueryWrapper(spaceUserQueryRequest));
        return ResultUtils.success(spaceUserApplicationService.getSpaceVOList(spaceUserList));
    }
}
