package com.hl.hlpicturebackend.api.aliyunai;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.hl.hlpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.hl.hlpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.hl.hlpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.hl.hlpicturebackend.exception.BusinessException;
import com.hl.hlpicturebackend.exception.ErrorCode;
import com.hl.hlpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunAiApi {

    // 读取配置文件
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    // 创建任务的地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务的地址
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建扩图任务
     * @param createOutPaintingTaskRequest
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest){
        // 校验参数
        ThrowUtils.throwIf(ObjUtil.isNull(createOutPaintingTaskRequest), ErrorCode.PARAMS_ERROR, "创建扩图任务请求不能为空");
        // 使用hutool工具类发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                // 必须开启异步模式
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.toString())
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));
        try (HttpResponse httpResponse = httpRequest.execute()) {
            // 检查响应状态码
            if (!httpResponse.isOk()) {
                log.error("创建扩图任务请求失败，状态码：{}，响应体：{}", httpResponse.getStatus(), httpResponse.body());
                ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "创建扩图任务请求失败");
            }
            // 解析响应
            String responseBody = httpResponse.body();
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(responseBody, CreateOutPaintingTaskResponse.class);
            String errorCode = response.getCode();
            if (StrUtil.isNotEmpty(errorCode)) {
                String message = response.getMessage();
                log.error("AI扩图失败，错误码：{}，错误信息：{}", errorCode, message);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图接口响应异常");
            }
            return response;
        } catch (Exception e) {
            log.error("创建扩图任务请求异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建扩图任务请求异常");
        }
    }

    /**
     * 获取扩图任务结果
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId){
        // 校验参数
        ThrowUtils.throwIf(ObjUtil.isNull(taskId), ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        String url = String.format(GET_OUT_PAINTING_TASK_URL, taskId);
        // 使用hutool工具类发送请求
        HttpRequest httpRequest = HttpRequest.get(url)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey);
        try (HttpResponse httpResponse = httpRequest.execute()) {
            // 检查响应状态码
            if (!httpResponse.isOk()) {
                log.error("获取扩图任务结果请求失败，状态码：{}，响应体：{}", httpResponse.getStatus(), httpResponse.body());
                ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "获取扩图任务结果请求失败");
            }
            // 解析响应
            String responseBody = httpResponse.body();
            GetOutPaintingTaskResponse response = JSONUtil.toBean(responseBody, GetOutPaintingTaskResponse.class);
            return response;
        } catch (Exception e) {
            log.error("获取扩图任务结果请求异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取扩图任务结果请求异常");
        }
    }
}
