package com.hl.hlpicture.interfaces.controller;

import cn.hutool.core.util.ObjUtil;
import com.hl.hlpicture.infrastructure.common.BaseResponse;
import com.hl.hlpicture.infrastructure.common.ResultUtils;
import com.hl.hlpicture.infrastructure.exception.ErrorCode;
import com.hl.hlpicture.infrastructure.exception.ThrowUtils;
import com.hl.hlpicture.interfaces.dto.space.analyze.*;
import com.hl.hlpicture.domain.space.entity.Space;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.interfaces.vo.space.analyze.*;
import com.hl.hlpicture.application.service.SpaceAnalyzeApplicationService;
import com.hl.hlpicture.application.service.UserApplicationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {


    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private SpaceAnalyzeApplicationService spaceAnalyzeApplicationService;

    /**
     * 分析空间使用情况
     *
     * @param spaceUsageAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyze(@RequestBody SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
                                                                HttpServletRequest request) {
        // 校验请求参数
        ThrowUtils.throwIf(ObjUtil.isNull(spaceUsageAnalyzeRequest), ErrorCode.PARAMS_ERROR);
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        SpaceUsageAnalyzeResponse response = spaceAnalyzeApplicationService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 分析空间分类使用情况
     *
     * @param spaceCategoryAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyze(@RequestBody SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
                                                                 HttpServletRequest request) {
        // 校验请求参数
        ThrowUtils.throwIf(ObjUtil.isNull(spaceCategoryAnalyzeRequest), ErrorCode.PARAMS_ERROR);
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        List<SpaceCategoryAnalyzeResponse> response = spaceAnalyzeApplicationService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 分析空间标签使用情况
     *
     * @param spaceTagAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceTagAnalyze(@RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest,
                                                                          HttpServletRequest request) {
        // 校验请求参数
        ThrowUtils.throwIf(ObjUtil.isNull(spaceTagAnalyzeRequest), ErrorCode.PARAMS_ERROR);
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        List<SpaceTagAnalyzeResponse> response = spaceAnalyzeApplicationService.getSpaceTagAnalyze(spaceTagAnalyzeRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 分析空间图片大小使用情况
     *
     * @param spaceSizeAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/size")
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> getSpaceSizeAnalyze(@RequestBody SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest,
                                                                            HttpServletRequest request) {
        // 校验请求参数
        ThrowUtils.throwIf(ObjUtil.isNull(spaceSizeAnalyzeRequest), ErrorCode.PARAMS_ERROR);
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        List<SpaceSizeAnalyzeResponse> response = spaceAnalyzeApplicationService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest,
                loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 分析空间用户使用情况
     *
     * @param spaceUserAnalyzeRequest
     * @return
     */
    @PostMapping("/user")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyze(@RequestBody SpaceUserAnalyzeRequest spaceUserAnalyzeRequest,
                                                                            HttpServletRequest httpServletRequest) {
        // 校验请求参数
        ThrowUtils.throwIf(ObjUtil.isNull(spaceUserAnalyzeRequest), ErrorCode.PARAMS_ERROR);
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(httpServletRequest);
        List<SpaceUserAnalyzeResponse> response = spaceAnalyzeApplicationService.getSpaceUserAnalyze(spaceUserAnalyzeRequest,
                loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 分析空间排名情况(仅管理员可用)
     *
     * @param spaceRankAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/rank")
    public BaseResponse<List<Space>> getSpaceRankAnalyze(@RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest,
                                                                            HttpServletRequest request) {
        // 校验请求参数
        ThrowUtils.throwIf(ObjUtil.isNull(spaceRankAnalyzeRequest), ErrorCode.PARAMS_ERROR);
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        List<Space> response = spaceAnalyzeApplicationService.getSpaceRankAnalyze(spaceRankAnalyzeRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 分析空间等级使用情况
     *
     * @param spaceLevelAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/level")
    public BaseResponse<List<SpaceLevelAnalyzeResponse>> getSpaceLevelAnalyze(@RequestBody SpaceLevelAnalyzeRequest spaceLevelAnalyzeRequest,
                                                                            HttpServletRequest request) {
        // 校验请求参数
        ThrowUtils.throwIf(ObjUtil.isNull(spaceLevelAnalyzeRequest), ErrorCode.PARAMS_ERROR);
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        List<SpaceLevelAnalyzeResponse> response = spaceAnalyzeApplicationService.getSpaceLevelAnalyze(spaceLevelAnalyzeRequest, loginUser);
        return ResultUtils.success(response);
    }
}
