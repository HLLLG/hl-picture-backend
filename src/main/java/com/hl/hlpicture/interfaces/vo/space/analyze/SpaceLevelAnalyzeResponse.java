package com.hl.hlpicture.interfaces.vo.space.analyze;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间等级分析响应体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceLevelAnalyzeResponse implements Serializable {

    /**
     * 空间等级
     */
    private String spaceLevel;

    /**
     * 空间数量
     */
    private Long count;

    private static final long serialVersionUID = 1L;
}
