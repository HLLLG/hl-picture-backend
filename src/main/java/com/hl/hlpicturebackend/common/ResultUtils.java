package com.hl.hlpicturebackend.common;

import com.hl.hlpicturebackend.exception.ErrorCode;

/**
 * 响应工具类
 * @param <T>
 */
public class ResultUtils<T> {

    /**
     * 成功
     * @param data 数据
     * @return
     * @param <T> 数据类型
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data ,"ok");
    }

    /**
     * 失败
     * @param errorCode 错误码
     * @return
     */
    public static BaseResponse<?> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败
     * @param code 状态码
     * @param message 错误消息
     * @return
     */
    public static BaseResponse<?> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    /**
     * 失败
     * @param errorCode 错误码
     * @param message 错误消息
     * @return
     */
    public static BaseResponse<?> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }


}
