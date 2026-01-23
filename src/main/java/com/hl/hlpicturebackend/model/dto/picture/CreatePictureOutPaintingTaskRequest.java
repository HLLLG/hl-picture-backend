package com.hl.hlpicturebackend.model.dto.picture;

import com.hl.hlpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI扩图请求类
 * @author 21628
 */
@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {
    private static final long serialVersionUID = 2732314194728777392L;

    /**
     * 图片ID
      */
    private Long pictureId;

    /**
     * 扩图参数
     */
    private CreateOutPaintingTaskRequest.Parameters parameters;

}
