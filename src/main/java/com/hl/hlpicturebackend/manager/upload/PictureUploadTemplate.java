package com.hl.hlpicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.hl.hlpicturebackend.config.CosClientConfig;
import com.hl.hlpicturebackend.exception.BusinessException;
import com.hl.hlpicturebackend.exception.ErrorCode;
import com.hl.hlpicturebackend.manager.CosManager;
import com.hl.hlpicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
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
     *
     * @param inputSource
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     *
     * @param inputSource
     * @return
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     *
     * @param inputSource
     */
    protected abstract void processFile(Object inputSource, File file) throws IOException;

    /**
     * 上传文件
     *
     * @param inputSource
     * @param prefix
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String prefix) {
        // 校验图片
        validPicture(inputSource);
        // 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginalFilename(inputSource);
        // 自己拼接文件上传路劲，增强安全性
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originalFilename));
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
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            List<CIObject> objectList = putObjectResult.getCiUploadResult().getProcessResults().getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                // 获取压缩图信息
                CIObject compressedCiObject = objectList.get(0);
                // 获取缩略图信息（如果有的话）
                CIObject thumbnailCiObject = compressedCiObject;
                if (objectList.size() > 1) {
                    thumbnailCiObject = objectList.get(1);
                }
                // 封装压缩图返回结果
                return buildResult(originalFilename, compressedCiObject, thumbnailCiObject);
            }
            // 获取图片信息并封装返回结果
            return buildResult(imageInfo, originalFilename, file, uploadFilePath);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteTempFile(file);
        }

    }


    /**
     * 获取图片主色调
     *
     * @param key
     * @return
     */
    private String getImageAve(String key) {
        COSObject cosObject = cosManager.getPictureObject(key);
        // 获取对象内容流
        COSObjectInputStream objectContent = cosObject.getObjectContent();
        // 从对象内容流中获取图片主色调
        try {
            // 将objectContent内容保存到本地字节数组
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int length;
            while ((length = objectContent.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            // 将字节数组转换为字符串数组
            String stringResult = result.toString("UTF-8");
            // 提取主色调信息
            return JSONUtil.parseObj(stringResult).getStr("RGB");
        } catch (Exception e) {
            log.error("获取图片主色调失败, key = {}", key, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取图片主色调失败");
        } finally {
            // 关闭对象内容流
            try {
                objectContent.close();
            } catch (IOException e) {
                log.error("关闭COSObjectInputStream失败, key = {}", key, e);
            }
        }
    }


    private UploadPictureResult buildResult(String originalFilename, CIObject ciObject,
                                            CIObject thumbnailCiObject) {
        // 封装返回结果
        String format = ciObject.getFormat();
        int picWidth = ciObject.getWidth();
        int picHeight = ciObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();

        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setName(FileUtil.getName(originalFilename));
        uploadPictureResult.setPicSize(ciObject.getSize().longValue());
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(format);
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + ciObject.getKey());
        String picColor = this.getImageAve(ciObject.getKey());
        uploadPictureResult.setPicColor(picColor);
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());

        // 返回可访问的地址
        return uploadPictureResult;
    }

    private UploadPictureResult buildResult(ImageInfo imageInfo, String originalFilename, File file,
                                            String uploadFilePath) {
        // 封装返回结果
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
        String picColor = this.getImageAve(uploadFilePath);
        uploadPictureResult.setPicColor(picColor);

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
