package com.hl.hlpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureUploadRequest implements Serializable {

    private static final long serialVersionUID = 7928728027961586797L;

    /**
     * 图片id（用于修改）
     */
    private Long id;

    /**
     * 文件url
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private List<String> tags;

    /**
     * 空间 id（为空表示公共空间）
     */
    private Long spaceId;
}
