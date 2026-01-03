package com.hl.hlpicturebackend.model.dto.file;

import lombok.Data;

import java.io.Serializable;

/**
 * 上传图片的结果
 */
@Data
public class UploadPictureResult implements Serializable {
    private static final long serialVersionUID = 6111893239014610511L;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;
}