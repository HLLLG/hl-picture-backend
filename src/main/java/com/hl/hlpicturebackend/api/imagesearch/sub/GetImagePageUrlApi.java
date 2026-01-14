package com.hl.hlpicturebackend.api.imagesearch.sub;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.hl.hlpicturebackend.exception.BusinessException;
import com.hl.hlpicturebackend.exception.ErrorCode;
import com.hl.hlpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 获取图片页面地址接口的api（Step 1）
 */
@Slf4j
public class GetImagePageUrlApi {

    public static String getImagePageUrl(String imageUrl) {
        // 构造请求表单数据
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");
        // 获取当前时间戳
        long updateTime = System.currentTimeMillis();
        // 请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + updateTime;

        try {
            // 发送请求
            HttpResponse httpResponse =
                    HttpRequest.post(url)
                            // 这里需要指定acs-token 不然会响应系统异常
                            .header("acs-token", RandomUtil.randomString(1))
                            .form(formData).timeout(5000).execute();
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                log.error("获取图片页面地址失败，http状态码：{}", httpResponse.getStatus());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片页面地址失败");
            }
            // 解析响应结果
            String responseBody = httpResponse.body();
            Map result = JSONUtil.toBean(responseBody, Map.class);
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                log.error("解析图片页面地址失败，响应内容：{}", responseBody);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "解析图片页面地址失败");
            }
            // 从响应中提取图片页面URL
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            String rawUrl = (String) data.get("url");
            // 对rawUrl进行解码
            String searchResultUrl = URLUtil.decode(rawUrl);
            // 判断Url是否为空
            ThrowUtils.throwIf(StrUtil.isBlank(searchResultUrl), ErrorCode.OPERATION_ERROR, "图片页面地址为空");
            return searchResultUrl;
        } catch (Exception e) {
            log.error("调用百度以图搜图接口失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "调用百度以图搜图接口失败");
        }
    }

    public static void main(String[] args) {
        String imageUrl =
                "https://ts1.tc.mm.bing.net/th/id/OIP-C" + ".gohj_saHp_TpKQnubpOedgHaLH?w=151&h=211&c=8&rs" + "=1&qlt" +
                        "=90&o=6&dpr=1.3&pid=3.1&rm=2";
        String searchResultUrl = getImagePageUrl(imageUrl);
        System.out.println("图片页面URL: " + searchResultUrl);
    }
}
