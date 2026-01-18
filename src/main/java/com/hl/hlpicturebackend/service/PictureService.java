package com.hl.hlpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hl.hlpicturebackend.model.dto.picture.*;
import com.hl.hlpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hl.hlpicturebackend.model.entity.User;
import com.hl.hlpicturebackend.model.vo.PictureVO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @author 21628
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-12-21 16:25:52
 */
public interface PictureService extends IService<Picture> {

    /**
     * 校验图片
     *
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 删除图片
     * @param pictureId
     * @param loginUser
     * @return
     */
    void deletePicture(Long pictureId, User loginUser);

    /**
     * 编辑图片
     * @param pictureEditRequest
     * @param loginUser
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 上传图片
     *
     * @param inputSource          输入源
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     * @throws IOException
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 批量上传图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 构造查询图片QueryWrapper
     *
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getPictureQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片包装类一条
     *
     * @param picture
     * @return
     */
    PictureVO getPictureVO(Picture picture);

    /**
     * 获取图片包装类多条
     *
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
     *
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 清理图片文件
     * @param oldPicture
     */
    @Async
    void clearPictureFile(Picture oldPicture);

    /**
     * 批量清理图片文件
     * @param pictureList
     */
    @Async
    void clearPictureFileBatch(List<Picture> pictureList);

    /**
     * 校验空间图片的权限
     * @param picture
     * @param loginUser
     */
    void validPictureAuth(Picture picture, User loginUser);

    /**
     * 根据空间 id 删除图片
     * @param spaceId
     */
    void removePictureBySpaceId(Long spaceId);

    /**
     * 根据颜色搜索图片
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    /**
     * 根据颜色获取图片分页
     * @param pictureList
     * @param picColor
     */
    void validPicturePageSortByColor(Page<Picture> pictureList, String picColor);

    /**
     * 根据颜色值排序图片
     * @param picColor
     * @param pictureList
     */
    List<Picture> sortPictureByColor(String picColor, List<Picture> pictureList);

    /**
     * 图片批量编辑
     *
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);
}
