package com.hl.hlpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.hl.hlpicturebackend.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManager {

    @Resource
    private COSClient cosClient;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     * @return
     * @throws CosClientException
     * @throws CosServiceException
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key 唯一键
     * @return
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 获取图片信息
     *
     * @param key 唯一键
     * @return
     */
    public COSObject getPictureObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        String rule = "imageAve";
        getObjectRequest.putCustomQueryParameter(rule, null);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传并解析图片
     *
     * @param key  唯一键
     * @param file 文件
     * @return
     * @throws CosClientException
     * @throws CosServiceException
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 对图片进行处理（获取基本信息也被视为一种图片处理
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        // 图片压缩（转成webp格式）
        String webpKey = FileUtil.mainName(key) + ".webp";
        List<PicOperations.Rule> rules = new ArrayList<>();
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setFileId(webpKey);
        rules.add(compressRule);
        // 生成缩略图（仅对> 20Kb的图片生成缩略图）
        if (file.length() > 20 * 1024) {
            String thumbnailKey = String.format("%s_thumbnail.%s", FileUtil.mainName(key), FileUtil.getSuffix(key));
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
            thumbnailRule.setFileId(thumbnailKey);
            rules.add(thumbnailRule);
        }
        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除图片对象
     *
     * @param key 唯一键
     */
    public void deletePictureObject(String key) {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }

    /**
     * 批量删除图片对象
     *
     * @param key 唯一键
     */
    public void deletePictureObjectBatch(List<DeleteObjectsRequest.KeyVersion> key) {
        DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(cosClientConfig.getBucket());
        deleteObjectsRequest.setKeys(key);
        cosClient.deleteObjects(deleteObjectsRequest);
    }
}
