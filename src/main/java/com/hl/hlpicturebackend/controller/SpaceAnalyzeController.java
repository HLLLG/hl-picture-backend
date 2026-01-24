package com.hl.hlpicturebackend.controller;

import cn.hutool.core.util.ObjUtil;
import com.hl.hlpicturebackend.common.BaseResponse;
import com.hl.hlpicturebackend.common.ResultUtils;
import com.hl.hlpicturebackend.exception.ErrorCode;
import com.hl.hlpicturebackend.exception.ThrowUtils;
import com.hl.hlpicturebackend.model.dto.space.analyze.*;
import com.hl.hlpicturebackend.model.entity.Space;
import com.hl.hlpicturebackend.model.entity.User;
import com.hl.hlpicturebackend.model.vo.analyze.*;
import com.hl.hlpicturebackend.service.PictureService;
import com.hl.hlpicturebackend.service.SpaceAnalyzeService;
import com.hl.hlpicturebackend.service.SpaceService;
import com.hl.hlpicturebackend.service.UserService;
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
    private UserService userService;

    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;

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
        User loginUser = userService.getLoginUser(request);
        SpaceUsageAnalyzeResponse response = spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, loginUser);
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
        User loginUser = userService.getLoginUser(request);
        List<SpaceCategoryAnalyzeResponse> response = spaceAnalyzeService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser);
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
        User loginUser = userService.getLoginUser(request);
        List<SpaceTagAnalyzeResponse> response = spaceAnalyzeService.getSpaceTagAnalyze(spaceTagAnalyzeRequest, loginUser);
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
        User loginUser = userService.getLoginUser(request);
        List<SpaceSizeAnalyzeResponse> response = spaceAnalyzeService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest,
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
        User loginUser = userService.getLoginUser(httpServletRequest);
        List<SpaceUserAnalyzeResponse> response = spaceAnalyzeService.getSpaceUserAnalyze(spaceUserAnalyzeRequest,
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
        User loginUser = userService.getLoginUser(request);
        List<Space> response = spaceAnalyzeService.getSpaceRankAnalyze(spaceRankAnalyzeRequest, loginUser);
        return ResultUtils.success(response);
    }
}
