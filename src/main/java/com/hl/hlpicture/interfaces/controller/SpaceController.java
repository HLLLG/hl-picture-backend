package com.hl.hlpicture.interfaces.controller;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hl.hlpicture.infrastructure.annotation.AuthCheck;
import com.hl.hlpicture.infrastructure.common.BaseResponse;
import com.hl.hlpicture.infrastructure.common.DeleteRequest;
import com.hl.hlpicture.infrastructure.common.ResultUtils;
import com.hl.hlpicture.interfaces.assembler.SpaceAssembler;
import com.hl.hlpicture.interfaces.dto.space.SpaceAddRequest;
import com.hl.hlpicture.interfaces.dto.space.SpaceEditRequest;
import com.hl.hlpicture.interfaces.dto.space.SpaceQueryRequest;
import com.hl.hlpicture.interfaces.dto.space.SpaceUpdateRequest;
import com.hl.hlpicture.domain.user.valueobject.UserConstant;
import com.hl.hlpicture.infrastructure.exception.ErrorCode;
import com.hl.hlpicture.infrastructure.exception.ThrowUtils;
import com.hl.hlpicture.share.auth.SpaceUserAuthManager;
import com.hl.hlpicture.domain.space.entity.Space;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.domain.space.valueobject.SpaceLevelEnum;
import com.hl.hlpicture.interfaces.vo.space.SpaceLevel;
import com.hl.hlpicture.interfaces.vo.space.SpaceVO;
import com.hl.hlpicture.domain.picture.service.PictureDomainService;
import com.hl.hlpicture.application.service.SpaceApplicationService;
import com.hl.hlpicture.application.service.UserApplicationService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private SpaceApplicationService spaceApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private PictureDomainService pictureApplicationService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 添加空间
     *
     * @param spaceAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        // 校验请求参数
        ThrowUtils.throwIf(ObjUtil.isNull(spaceAddRequest), ErrorCode.PARAMS_ERROR);
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        return ResultUtils.success(spaceApplicationService.addSpace(spaceAddRequest, loginUser));
    }

    /**
     * 删除空间
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        // 校验请求参数
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Long spaceId = deleteRequest.getId();
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        // 删除空间
        spaceApplicationService.deleteSpace(spaceId, loginUser);
        // 删除空间关联的图片
        pictureApplicationService.removePictureBySpaceId(spaceId);
        return ResultUtils.success(true);
    }

    /**
     * 编辑空间
     *
     * @param spaceEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        // 校验请求参数
        ThrowUtils.throwIf(ObjUtil.isNull(spaceEditRequest) || spaceEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        // 将实体类和 DTO 进行转换
        Space space = SpaceAssembler.toSpaceEntity(spaceEditRequest);
        // 自动填充数据
        spaceApplicationService.fillSpaceBySpaceLevel(space);
        // 补充编辑时间
        space.setEditTime(new Date());
        // 数据校验
        space.validSpace(false);
        // 判断空间是否存在
        Space oldSpace = spaceApplicationService.getById(space.getId());
        ThrowUtils.throwIf(ObjUtil.isNull(oldSpace), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 校验权限
        spaceApplicationService.checkSpaceAuth(oldSpace, loginUser);
        // 执行更新
        boolean result = spaceApplicationService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "空间编辑失败");
        return ResultUtils.success(true);
    }

    /**
     * 编辑空间（仅管理员可用
     *
     * @param spaceUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest) {
        // 校验请求参数
        ThrowUtils.throwIf(ObjUtil.isNull(spaceUpdateRequest) || spaceUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 将实体类和 DTO 进行转换
        Space space = SpaceAssembler.toSpaceEntity(spaceUpdateRequest);
        // 自动填充数据
        spaceApplicationService.fillSpaceBySpaceLevel(space);
        // 数据校验
        space.validSpace( false);
        // 判断空间是否存在
        Space Oldspace = spaceApplicationService.getById(space.getId());
        ThrowUtils.throwIf(ObjUtil.isNull(Oldspace), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 执行更新
        boolean result = spaceApplicationService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "空间编辑失败");
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取空间（仅管理员可用）
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(Long id) {
        // 校验请求参数
        ThrowUtils.throwIf(ObjUtil.isNull(id) || id <= 0, ErrorCode.PARAMS_ERROR);
        // 获取空间
        Space space = spaceApplicationService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isNull(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        return ResultUtils.success(space);
    }

    /**
     * 根据 id 获取空间
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(Long id, HttpServletRequest request) {
        // 校验请求参数
        ThrowUtils.throwIf(ObjUtil.isNull(id) || id <= 0, ErrorCode.PARAMS_ERROR);
        // 获取空间
        Space space = spaceApplicationService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isNull(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 获取当前用户
        User loginUser = userApplicationService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        // 转换为 VO
        SpaceVO spaceVO = spaceApplicationService.getSpaceVO(space);
        spaceVO.setPermissionList(permissionList);
        return ResultUtils.success(spaceVO);
    }

    /**
     * 分页获取空间列表
     *
     * @param spaceQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        // 校验请求参数
        ThrowUtils.throwIf(ObjUtil.isNull(spaceQueryRequest), ErrorCode.PARAMS_ERROR);
        int current = spaceQueryRequest.getCurrent();
        int pageSize = spaceQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "单页请求数量过多");
        // 构造分页查询
        Page<Space> spacePage = new Page<>(current, pageSize);
        // 查询
        Page<Space> SpacePage = spaceApplicationService.page(spacePage, spaceApplicationService.getSpaceQueryWrapper(spaceQueryRequest));
        // 转换为 VO 分页
        Page<SpaceVO> spaceVOPage = spaceApplicationService.getSpaceVOPage(SpacePage);
        return ResultUtils.success(spaceVOPage);
    }

    /**
     * 分页获取空间列表（仅管理员可用）
     *
     * @param spaceQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        // 校验请求参数
        ThrowUtils.throwIf(ObjUtil.isNull(spaceQueryRequest), ErrorCode.PARAMS_ERROR);
        int current = spaceQueryRequest.getCurrent();
        int pageSize = spaceQueryRequest.getPageSize();
        // 构造分页查询
        Page<Space> spacePage = spaceApplicationService.page(new Page<>(current, pageSize),
                spaceApplicationService.getSpaceQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spacePage);
    }

    /**
     * 获取空间等级列表
     * @return
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> getSpaceLevels() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }
}
