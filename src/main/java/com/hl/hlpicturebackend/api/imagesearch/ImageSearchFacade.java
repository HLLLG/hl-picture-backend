package com.hl.hlpicturebackend.api.imagesearch;

import com.hl.hlpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.hl.hlpicturebackend.api.imagesearch.sub.GetImageFirstUrlAPi;
import com.hl.hlpicturebackend.api.imagesearch.sub.GetImageListApi;
import com.hl.hlpicturebackend.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 搜索图片门面
 */
@Slf4j
public class ImageSearchFacade {

    public static List<ImageSearchResult> searchImages(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlAPi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

    public static void main(String[] args) {
        List<ImageSearchResult> imageList = searchImages("https://ts1.tc.mm.bing.net/th/id/OIP-C" +
                ".gohj_saHp_TpKQnubpOedgHaLH?w=151&h=211&c=8&rs=1&qlt=90&o=6&dpr=1.3&pid=3.1&rm=2");
        System.out.println(imageList);
    }

}
