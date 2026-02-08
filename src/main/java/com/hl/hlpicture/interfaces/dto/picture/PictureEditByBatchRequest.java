package com.hl.hlpicture.interfaces.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片批量编辑请求
 * @author 21628
 */
@Data
public class PictureEditByBatchRequest implements Serializable {
    private static final long serialVersionUID = 2732314194728777392L;

    /**
     * 图片ID列表
      */
    private List<Long> pictureIdList;

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 分类
     */
    private String category;

    /**
     * 命名规则
     */
    private String nameRule;

    /**
     * 是否查询公共空间图片
     */
    private Boolean NullSpaceId;
}
