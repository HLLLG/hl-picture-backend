package com.hl.hlpicture.domain.picture.valueobject;

import lombok.Getter;

@Getter
public enum ImgSizeEnum {

    // 以“像素面积”(width * height) 分档，区间为 [minPixels, maxPixels)
    EXTRA_SMALL("特小", 0, 0L, 640L * 480L),                 // < 307,200
    SMALL("小", 1, 640L * 480L, 1280L * 720L),               // < 921,600
    MEDIUM("中", 2, 1280L * 720L, 1920L * 1080L),            // < 2,073,600
    LARGE("大", 3, 1920L * 1080L, 3840L * 2160L),            // < 8,294,400
    EXTRA_LARGE("特大", 4, 3840L * 2160L, Long.MAX_VALUE);   // >= 8,294,400

    private final String text;
    private final int value;

    private final long minPixels; // inclusive
    private final long maxPixels; // exclusive

    ImgSizeEnum(String text, int value, long minPixels, long maxPixels) {
        this.text = text;
        this.value = value;
        this.minPixels = minPixels;
        this.maxPixels = maxPixels;
    }

    /**
     * 更具value获取图片大小枚举
     * @return
     */
    public static ImgSizeEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (ImgSizeEnum imgSizeEnum : ImgSizeEnum.values()) {
            if (value == imgSizeEnum.value) {
                return imgSizeEnum;
            }
        }
        return null;
    }

}
