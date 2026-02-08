package com.hl.hlpicture.infrastructure.api.imagesearch.model;

import lombok.Data;

/**
 * 以图搜图结果
 */
@Data
public class ImageSearchResult {

    /**
     * 来源地址
     */
    private String fromUrl;

    /**
     * 缩略图地址
     */
    private String thumbUrl;
}
