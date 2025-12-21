package com.hl.hlpicturebackend.controller;

import com.hl.hlpicturebackend.annotation.AuthCheck;
import com.hl.hlpicturebackend.common.BaseResponse;
import com.hl.hlpicturebackend.common.ResultUtils;
import com.hl.hlpicturebackend.constant.UserConstant;
import com.hl.hlpicturebackend.exception.BusinessException;
import com.hl.hlpicturebackend.exception.ErrorCode;
import com.hl.hlpicturebackend.exception.ThrowUtils;
import com.hl.hlpicturebackend.manager.CosManager;
import com.hl.hlpicturebackend.model.dto.picture.PictureUploadRequest;
import com.hl.hlpicturebackend.model.entity.User;
import com.hl.hlpicturebackend.model.vo.PictureVO;
import com.hl.hlpicturebackend.service.PictureService;
import com.hl.hlpicturebackend.service.UserService;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private PictureService pictureService;
    @Autowired
    private UserService userService;

    /**
     * 上传图片（可重新上传）
     *
     * @param multipartFile
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/upload")
    public BaseResponse<PictureVO> UploadPicture(@RequestParam("file") MultipartFile multipartFile,
                                                 PictureUploadRequest uploadPictureRequest,
                                                 HttpServletRequest request) throws BusinessException {
        ThrowUtils.throwIf(multipartFile == null || uploadPictureRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, uploadPictureRequest, loginUser);
        return ResultUtils.success(pictureVO);

    }
}
