package com.hl.hlpicture.domain.space.valueobject;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 空间等级枚举
 */
@Getter
public enum SpaceLevelEnum {

    COMMON("普通版", 0, 100, 100L * 1024 * 1024), // 100MB
    PROFESSIONAL("专业版", 1, 1000, 1L * 1024 * 1024 * 1024), // 1GB
    FLAGSHIP("旗舰版", 2, 10000, 10L * 1024 * 1024 * 1024); // 10GB

    private final String text;

    private final int value;

    private final long maxCount;

    private final long maxSize;

    /**
     * @param text     空间描述
     * @param value    空间等级值
     * @param maxCount 最大图片数量
     * @param maxSize  最大存储空间大小
     */
    SpaceLevelEnum(String text, int value, long maxCount, long maxSize) {
        this.text = text;
        this.value = value;
        this.maxCount = maxCount;
        this.maxSize = maxSize;
    }

    /**
     * 根据value获取空间等级枚举
     *
     * @param value
     * @return
     */
    public static SpaceLevelEnum getEnumByValue(Integer value) {
        if (ObjUtil.isNull(value)) {
            return null;
        }
        for (SpaceLevelEnum spaceLevelEnum : SpaceLevelEnum.values()) {
            if (value == spaceLevelEnum.value) {
                return spaceLevelEnum;
            }
        }
        return null;
    }
}
