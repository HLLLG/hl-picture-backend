package com.hl.hlpicturebackend.model.dto.picture;

import com.hl.hlpicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 图片查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PictureQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 7928728027961586797L;

    /**
     * 图片id（用于修改）
     */
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private List<String> tags;

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

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人 ID
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private Date reviewTime;

    /**
     * 搜索词（同时搜名称、简介等）
     */
    private String searchText;

    /**
     * 空间 id（为空表示公共空间）
     */
    private Long spaceId;

    /**
     * 是否查询公共空间图片
     */
    private Boolean NullSpaceId;

    /**
     * 编辑开始时间
     */
    private Date startEditTime;

    /**
     * 编辑结束时间
     */
    private Date endEditTime;

    /**
     * 图片尺寸
     */
    private Integer imgSize;

    /**
     * 主色调
     */
    private String picColor;

}
