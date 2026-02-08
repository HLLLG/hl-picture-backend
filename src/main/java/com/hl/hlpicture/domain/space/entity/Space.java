package com.hl.hlpicture.domain.space.entity;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

import com.hl.hlpicture.domain.space.valueobject.SpaceLevelEnum;
import com.hl.hlpicture.domain.space.valueobject.SpaceTypeEnum;
import com.hl.hlpicture.infrastructure.exception.ErrorCode;
import com.hl.hlpicture.infrastructure.exception.ThrowUtils;
import lombok.Data;

/**
 * 空间
 * @TableName space
 */
@TableName(value ="space")
@Data
public class Space {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
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
     * 空间类型：0-私有 1-团队
     */
    private Integer spaceType;


    /**
     * 空间图片的最大总大小
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    private Long maxCount;

    /**
     * 当前空间下图片的总大小
     */
    private Long totalSize;

    /**
     * 当前空间下的图片数量
     */
    private Long totalCount;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 验证空间参数的合法性，并设置默认值
     */
    public void fillDefaultValue() {
        // 默认值
        if (StrUtil.isBlank(this.getSpaceName())) {
            this.setSpaceName("默认空间");
        }
        if (this.getSpaceLevel() == null) {
            this.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (this.getSpaceType() == null) {
            this.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
    }

    public void validSpace(boolean isAdd) {
        // 从空间中取值
        String spaceName = this.getSpaceName();
        Integer spaceLevel = this.getSpaceLevel();
        Integer spaceType = this.getSpaceType();
        SpaceLevelEnum levelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        if (isAdd) {
            // 添加时，空间名称不能为空
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR,
                    "空间名称不能为空");
            // 添加时，空间等级不能为空
            ThrowUtils.throwIf(spaceLevel == null, ErrorCode.PARAMS_ERROR, "空间等级不能为空");
            // 添加时，空间类别不能为空
            ThrowUtils.throwIf(spaceType == null, ErrorCode.PARAMS_ERROR, "空间类别不能为空");
        }
        // 修改时，空间名称不能为空且长度不能超过30个字符
        ThrowUtils.throwIf(StrUtil.isNotBlank(spaceName) && spaceName.length() > 30, ErrorCode.PARAMS_ERROR,
                "空间名称长度不能超过30个字符");
        // 修改时，空间等级必须是0-2之间的整数
        ThrowUtils.throwIf(spaceLevel != null && ObjUtil.isNull(levelEnum), ErrorCode.PARAMS_ERROR,
                "空间等级必须是0-2之间的整数");
        // 修改时，空间类别必须是0-1之间的整数
        ThrowUtils.throwIf(spaceType != null && ObjUtil.isNull(SpaceTypeEnum.getEnumByValue(spaceType)),
                ErrorCode.PARAMS_ERROR,
                "空间类别必须是0-1之间的整数");
    }
}