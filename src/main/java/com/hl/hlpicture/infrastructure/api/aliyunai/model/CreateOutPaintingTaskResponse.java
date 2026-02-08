package com.hl.hlpicture.infrastructure.api.aliyunai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 创建扩图任务响应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOutPaintingTaskResponse implements Serializable {

    /**
     * 任务输出信息
     */
    private Output output;

    /**
     * 请求唯一标识，可用于请求明细溯源和问题排查
     */
    private String requestId;

    /**
     * 请求失败的错误码
     */
    private String code;

    /**
     * 请求失败的详细信息
     */
    private String message;

    @Data
    public static class Output implements Serializable {
        /**
         * 任务ID，查询有效期24小时
         */
        private String taskId;

        /**
         * 任务状态
         * PENDING：任务排队中
         * RUNNING：任务处理中
         * SUCCEEDED：任务执行成功
         * FAILED：任务执行失败
         * CANCELED：任务已取消
         * UNKNOWN：任务不存在或状态未知
         */
        private String taskStatus;
    }
}