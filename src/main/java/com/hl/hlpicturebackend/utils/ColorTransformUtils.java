package com.hl.hlpicturebackend.utils;

import lombok.NoArgsConstructor;

import java.awt.*;

/**
 * 颜色转换工具类
 */
@NoArgsConstructor
public class ColorTransformUtils {

    /**
     * 获取标准颜色（将数据万象的5位颜色码转换为标准的6位颜色码）
     */
    public static String getStandardColor(String color) {
        // 每一种 rgb 色值都有可能只有一个0，要转换为00
        // 如果是六位，不用转换，如果是五位，要给第三位后面加0
        // 示例：
        // 0x080e0 => 0x0800e0
        if (color.length() == 7) {
            StringBuilder stringBuilder = new StringBuilder(color);
            stringBuilder.insert(6, "0");
            color = stringBuilder.toString();
        }

        return color;
    }
}

