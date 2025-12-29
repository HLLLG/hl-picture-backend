package com.hl.hlpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.hl.hlpicturebackend.exception.ErrorCode;
import com.hl.hlpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 文件上传图片
 */
@Service
public class FilePictureUpload extends PictureUploadTemplate {
    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        String filename = multipartFile.getOriginalFilename();
        String fileSuffix = FileUtil.getSuffix(filename);
        // 校验文件后缀
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型不合规");
        // 校验文件大小
        final long ONE_M = 1024 * 1024L;
        ThrowUtils.throwIf(multipartFile.getSize() > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) throws IOException {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }
}
