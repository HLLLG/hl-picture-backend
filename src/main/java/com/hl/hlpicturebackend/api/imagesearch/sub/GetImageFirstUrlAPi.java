package com.hl.hlpicturebackend.api.imagesearch.sub;

import com.hl.hlpicturebackend.exception.BusinessException;
import com.hl.hlpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 获取图片列表接口的api（Step 2）
 */
@Slf4j
public class GetImageFirstUrlAPi {

    public static String getImageFirstUrl(String url) {
        try {
            // 使用JSOUP解析网页内容
            Document document = Jsoup.connect(url).timeout(5000).get();

            // 获取所有<script>标签内容
            Elements scriptDocuments = document.getElementsByTag("script");

            // 遍历<script>标签，查找包含firstUrl的脚本
            for (Element script : scriptDocuments) {
                String scriptContent = script.html();
                if (scriptContent.contains("\"firstUrl\"")) {
                    // 用正则表达式提取firstUrl的值
                    String regex = "\"firstUrl\"\\s*:\\s*\"(.*?)\"";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(scriptContent);
                    if (matcher.find()) {
                        String firstUrl = matcher.group(1);
                        // 解码URL中的转义字符
                        firstUrl = firstUrl.replace("\\/", "/");
                        return firstUrl;
                    }
                }
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到url");
        } catch (Exception e) {
            log.error("获取图片列表地址失败，错误信息：{}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

    public static void main(String[] args) {
        String searchResultUrl = "https://graph.baidu.com/s?card_key=&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&f=all&isLogoShow=1&session_id=17141169479695560997&sign=126001b137bea1130e56601768390218&tpl_from=pc";
        String firstUrl = getImageFirstUrl(searchResultUrl);
        System.out.println("First URL: " + firstUrl);
    }
}
