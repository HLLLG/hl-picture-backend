package com.hl.hlpicturebackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用删除请求类
 */
@Data
public class DeleteRequest implements Serializable {

    private Long id;

    public static final long serialVersionUID = 1L;
}
