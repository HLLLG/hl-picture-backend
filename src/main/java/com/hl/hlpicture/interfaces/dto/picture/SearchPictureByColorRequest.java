package com.hl.hlpicture.interfaces.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 颜色搜图请求
 */
@Data
public class SearchPictureByColorRequest implements Serializable {

    private static final long serialVersionUID = 116600376436354859L;
    /**
     * 空间id
     */
    private Long SpaceId;

    /**
     * 颜色值
     */
    private String picColor;
    
}
