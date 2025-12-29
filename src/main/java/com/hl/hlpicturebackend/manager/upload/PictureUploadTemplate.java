package com.hl.hlpicturebackend.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.hl.hlpicturebackend.config.CosClientConfig;
import com.hl.hlpicturebackend.exception.BusinessException;
import com.hl.hlpicturebackend.exception.ErrorCode;
import com.hl.hlpicturebackend.exception.ThrowUtils;
import com.hl.hlpicturebackend.manager.CosManager;
import com.hl.hlpicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 文件上传模板
 */
@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 校验图片
     * @param inputSource
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     * @param inputSource
     * @return
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     * @param inputSource
     */
    protected abstract void processFile(Object inputSource, File file) throws IOException;

    /**
     * 上传文件
     * @param inputSource
     * @param prefix
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String prefix) {
        // 校验图片
        validPicture(inputSource);
        // 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename  = getOriginalFilename(inputSource);
        // 自己拼接文件上传路劲，增强安全性
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String uploadFilePath = String.format("/%s/%s", prefix, uploadFileName);
        // 文件目录
        File file = null;
        try {
            // 创建临时文件
            file = File.createTempFile(uploadFilePath, null);
            // 处理文件来源
            processFile(inputSource, file);
            // 上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadFilePath, file);
            // 获取图片信息并封装返回结果
            return buildResult(putObjectResult, originalFilename, file, uploadFilePath);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteTempFile(file);
        }

    }

    private UploadPictureResult buildResult(PutObjectResult putObjectResult, String originalFilename, File file, String uploadFilePath) {
        // 封装返回结果
        ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
        String format = imageInfo.getFormat();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();

        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setName(FileUtil.getName(originalFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(format);
        uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadFilePath);

        // 返回可访问的地址
        return uploadPictureResult;
    }

    /**
     * 删除临时文件
     *
     * @param file
     */
    private static void deleteTempFile(File file) {
        if (file != null) {
            boolean delete = file.delete();
            if (!delete) {
                log.error("delete file error, uploadFilePath = {}", file.getAbsoluteFile());
            }
        }
    }
}
