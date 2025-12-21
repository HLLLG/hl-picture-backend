package com.hl.hlpicturebackend.service;

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
     * 上传图片
     *
     * @param file
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     * @throws IOException
     */
    PictureVO uploadPicture(MultipartFile file, PictureUploadRequest pictureUploadRequest, User loginUser);
}
