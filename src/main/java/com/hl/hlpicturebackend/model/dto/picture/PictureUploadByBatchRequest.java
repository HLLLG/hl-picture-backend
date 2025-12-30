package com.hl.hlpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 批量通过url上传图片请求体
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {

    private static final long serialVersionUID = 7928728027961586797L;

    /**
     * 搜索文本
     */
    private String searchText;

    /**
     * 搜索数量
     */
    private Integer count;

    /**
     * 图片名称前缀
     */
    private String namePrefix;
}
