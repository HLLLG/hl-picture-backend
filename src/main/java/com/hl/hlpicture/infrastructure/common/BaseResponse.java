package com.hl.hlpicture.infrastructure.common;

import com.hl.hlpicture.infrastructure.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 全局响应包装类
 * @param <T>
 */
@Data
public class BaseResponse<T> implements Serializable {
    private  int code;

    private  T data;

    private  String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this.code = code;
        this.data = data;
        this.message = "";
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
