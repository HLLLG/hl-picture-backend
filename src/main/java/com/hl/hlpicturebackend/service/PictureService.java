package com.hl.hlpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hl.hlpicturebackend.model.dto.picture.PictureQueryRequest;
import com.hl.hlpicturebackend.model.dto.picture.PictureReviewRequest;
import com.hl.hlpicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.hl.hlpicturebackend.model.dto.picture.PictureUploadRequest;
import com.hl.hlpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hl.hlpicturebackend.model.entity.User;
import com.hl.hlpicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author 21628
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-12-21 16:25:52
 */
public interface PictureService extends IService<Picture> {

    /**
     * 校验图片
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 上传图片
     *
     * @param inputSource 输入源
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     * @throws IOException
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 构造查询图片QueryWrapper
     *
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getPictureQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片包装类一条
     * @param picture
     * @return
     */
    PictureVO getPictureVO(Picture picture);

    /**
     * 获取图片包装类多条
     * @param picturePage
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage);

    /**
     * 审核图片
     *
     * @param pictureReviewRequest
     * @param loginUser
     * @return
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量上传图片
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);
}
