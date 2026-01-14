package com.hl.hlpicturebackend.api.imagesearch.sub;

import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hl.hlpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.hl.hlpicturebackend.exception.BusinessException;
import com.hl.hlpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 获取图片列表的api（Step 3）
 */
@Slf4j
public class GetImageListApi {

    public static List<ImageSearchResult> getImageList(String url) {
        try {
            // 发起Get请求
            HttpResponse response = HttpRequest.get(url).timeout(5000).execute();
            // 获取响应内容
            String body = response.body();
            int statusCode = response.getStatus();

            // 处理响应
            if (statusCode == 200) {
                // 解析JSON数据并处理
                return processResponse(body);
            } else {
                log.error("获取图片列表失败，HTTP状态码：{}，响应内容：{}", statusCode, body);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片列表失败");
        }
    }

    private static List<ImageSearchResult> processResponse(String body) {
        // 解析响应对象
        JSONObject jsonObject = new JSONObject(body);
        if (jsonObject == null || !jsonObject.containsKey("data")) {
            log.error("解析图片列表失败，响应内容：{}", body);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "解析图片列表失败");
        }
        // 提取图片数据
        JSONObject data = jsonObject.getJSONObject("data");
        if (data == null || !data.containsKey("list")) {
            log.error("图片数据缺失，响应内容：{}", body);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片数据缺失");
        }
        JSONArray list = data.getJSONArray("list");
        return JSONUtil.toList(list, ImageSearchResult.class);
    }

    public static void main(String[] args) {
        String url = "https://graph.baidu.com/ajax/pcsimi?carousel=503&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&inspire=general_pc&limit=30&next=2&render_type=card&session_id=17141169479695560997&sign=126001b137bea1130e56601768390218&tk=5ceec&tpl_from=pc";
        List<ImageSearchResult> results = getImageList(url);
        System.out.println(results);
    }

}
