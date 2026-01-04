package com.hl.hlpicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * @description: 编辑空间请求
 */
@Data
public class SpaceEditRequest implements Serializable {
    private static final long serialVersionUID = -5917151203541028533L;
    /**
     * id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;
}
