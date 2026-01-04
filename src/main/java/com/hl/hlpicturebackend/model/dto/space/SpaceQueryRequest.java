package com.hl.hlpicturebackend.model.dto.space;

import com.hl.hlpicturebackend.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * @description: 空间查询请求
 */
@Data
public class SpaceQueryRequest extends PageRequest implements Serializable {
    private static final long serialVersionUID = -7448923426471938743L;
    /**
     * id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 创建用户 id
     */
    private Long userId;
}
