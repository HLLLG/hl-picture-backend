package com.hl.hlpicture.domain.picture.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hl.hlpicture.domain.picture.entity.Picture;
import com.hl.hlpicture.domain.picture.repository.PictureRepository;
import com.hl.hlpicture.domain.picture.service.PictureDomainService;
import com.hl.hlpicture.domain.picture.valueobject.ImgSizeEnum;
import com.hl.hlpicture.domain.picture.valueobject.PictureReviewStatusEnum;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.infrastructure.api.aliyunai.AliYunAiApi;
import com.hl.hlpicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.hl.hlpicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.hl.hlpicture.infrastructure.exception.BusinessException;
import com.hl.hlpicture.infrastructure.exception.ErrorCode;
import com.hl.hlpicture.infrastructure.exception.ThrowUtils;
import com.hl.hlpicture.infrastructure.utils.ColorSimilarUtils;
import com.hl.hlpicture.interfaces.dto.picture.*;
import com.hl.hlpicture.interfaces.vo.picture.PictureVO;
import com.hl.hlpicturebackend.manager.CosManager;
import com.hl.hlpicturebackend.manager.upload.FilePictureUpload;
import com.hl.hlpicturebackend.manager.upload.PictureUploadTemplate;
import com.hl.hlpicturebackend.manager.upload.UrlPictureUpload;
import com.hl.hlpicturebackend.model.dto.file.UploadPictureResult;
import com.hl.hlpicture.domain.space.entity.Space;
import com.hl.hlpicture.application.service.SpaceApplicationService;
import com.qcloud.cos.model.DeleteObjectsRequest;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 21628
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-12-21 16:25:52
 */
@Service
@Slf4j
public class PictureDomainServiceImpl implements PictureDomainService {

    @Resource
    private PictureRepository pictureRepository;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private SpaceApplicationService spaceApplicationService;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private CosManager cosManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Override
    public void deletePicture(Long pictureId, User loginUser) {
        // 判断图片是否存在
        Picture oldPicture = pictureRepository.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限，已经改为使用注解鉴权
//        this.validPictureAuth(oldPicture, loginUser);
        // 开启事务
        Long finalSpaceId = oldPicture.getSpaceId();
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = pictureRepository.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片删除失败");
            // 如果空间id存在，更新空间使用额度
            if (finalSpaceId != null) {
                boolean update = spaceApplicationService.lambdaUpdate().eq(Space::getId, finalSpaceId).setSql("totalSize = " +
                        "totalSize" + " + " + oldPicture.getPicSize()).setSql("totalCount = totalCount - 1").update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "空间额度更新失败");
            }
            return true;
        });
        // 清理图片文件
        this.clearPictureFile(oldPicture);
    }

    @Override
    public void editPicture(Picture picture, User loginUser) {
        // 判断图片是否存在
        Picture oldPicture = pictureRepository.getById(picture.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限，已经改为使用注解鉴权
//        this.validPictureAuth(oldPicture, loginUser);
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        picture.validPicture();
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = pictureRepository.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 判断图片是否新增
        Long pictureId = null;
        Long spaceId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
            spaceId = pictureUploadRequest.getSpaceId();
        }
        // 若空间id不为空，校验空间是否存在
        if (spaceId != null) {
            // 校验空间是否存在
            Space space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            // 改为使用统一权限校验
            // 仅空间创建者能上传到该空间
//            ThrowUtils.throwIf(!loginUser.getId().equals(space.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权限上传到该空间");
            // 校验额度
            ThrowUtils.throwIf(space.getTotalSize() >= space.getMaxSize(), ErrorCode.OPERATION_ERROR, "空间存储已满，无法上传图片");
            ThrowUtils.throwIf(space.getTotalCount() >= space.getMaxCount(), ErrorCode.OPERATION_ERROR,
                    "空间图片数量已达上限，无法上传图片");
        }
        // 如果是更新，需要校验图片是否存在
        Picture oldPicture = null;
        if (pictureId != null) {
            oldPicture = pictureRepository.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.PARAMS_ERROR, "图片不存在");
            // 改为使用统一权限校验
            // 仅本人和管理员能更新
//            ThrowUtils.throwIf(!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser),
//                    ErrorCode.NO_AUTH_ERROR);
            // 校验空间是否一致
            // 如果没穿空间id，则沿用旧图片的空间id
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 如果传了空间id，则空间id必须和旧图片一致
                ThrowUtils.throwIf(!spaceId.equals(oldPicture.getSpaceId()), ErrorCode.PARAMS_ERROR, "空间id不一致，无法修改");
            }

        }
        // 按照空间划分目录
        String uploadPathPrefix = "";
        if (spaceId == null) {
            // 公共图库
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            // 空间
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        // 根据inputSource的类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        // 上传图片
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造入库的信息
        Picture picture = new Picture();
        picture.setSpaceId(spaceId);
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getName();
        if (pictureUploadRequest != null) {
            if (StrUtil.isNotBlank(pictureUploadRequest.getName())) {
                picName = pictureUploadRequest.getName();
            }
            if (StrUtil.isNotBlank(pictureUploadRequest.getCategory())) {
                picture.setCategory(pictureUploadRequest.getCategory());
            }
            if (CollUtil.isNotEmpty(pictureUploadRequest.getTags())) {
                // 将标签列表转换为 JSON 字符串存储
                picture.setTags(JSONUtil.toJsonStr(pictureUploadRequest.getTags()));
            }

        }
        picture.setName(picName);
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 如果图片已存在
        if (pictureId != null) {
            // 需要补充id和更新编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
            // 清理旧图片文件
            this.clearPictureFile(oldPicture);
        }
        // 开启事务
        Long finalSpaceId = spaceId;
        Picture finalOldPicture = oldPicture;
        transactionTemplate.execute(status -> {
            // 保存或更新图片信息到数据库
            boolean result = pictureRepository.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片保存到数据库失败");
            // 仅当空间id不为空时，才更新空间使用额度
            if (finalSpaceId != null) {
                boolean update = spaceApplicationService.lambdaUpdate().eq(Space::getId, finalSpaceId).setSql("totalSize = " +
                        "totalSize + " + picture.getPicSize() + (finalOldPicture == null ? "" :
                        " - " + finalOldPicture.getPicSize())).setSql("totalCount = totalCount + 1" + (finalOldPicture == null ? "" : " - 1")).update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "空间额度更新失败");
            }
            return picture;
        });
        return PictureVO.objToVO(picture);
    }

    @Override
    public int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 获取参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        // 格式化数量
        Integer count = pictureUploadByBatchRequest.getCount();
        // 偏移量
        Integer offset = pictureUploadByBatchRequest.getOffset();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "单次批量上传数量不能超过30张");
        // 拼接图片抓取地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&first=%s&mmasync=1", searchText, offset);
        Document document = null;
        try {
            // 使用jsoup抓取图片地址html列表
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("图片抓取失败", e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片抓取失败");
        }
        // 解析类名 dgControl
        Element div = document.getElementsByClass("dgControl").first();
        ThrowUtils.throwIf(ObjectUtil.isEmpty(div), ErrorCode.PARAMS_ERROR, "获取元素失败");
        // 在 div 元素内部查找所有 CSS class 为 ming 的 <img> 标签
        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;
        // 循环上传图片地址列表
        for (Element imgElement : imgElementList) {
            // 获取src属性，得到图片地址列表
            String imgUrl = imgElement.attr("src");
            if (StrUtil.isBlank(imgUrl)) {
                log.info("图片地址为空，跳过:{}", imgUrl);
            }
            // 处理图片上传地址，防止出现转义问题
            int questionMarkIndex = imgUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                imgUrl = imgUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            // 设置分类和标签
            if (StrUtil.isNotBlank(pictureUploadByBatchRequest.getCategory())) {
                pictureUploadRequest.setCategory(pictureUploadByBatchRequest.getCategory());
            }
            if (CollUtil.isNotEmpty(pictureUploadByBatchRequest.getTags())) {
                pictureUploadRequest.setTags(pictureUploadByBatchRequest.getTags());
            }
            // 名称前缀等于搜索关键字
            String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
            if (StrUtil.isBlank(namePrefix)) {
                namePrefix = searchText;
            }
            try {
                if (StrUtil.isNotBlank(namePrefix)) {
                    pictureUploadRequest.setName(namePrefix + (uploadCount + 1));
                }
                // 上传
                PictureVO pictureVO = this.uploadPicture(imgUrl, pictureUploadRequest, loginUser);
                log.info("第{}张图片上传成功，id:{}", uploadCount + 1, pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("上传图片失败", e);
            }
            // 达到上传数量，跳出循环
            if (uploadCount >= count) {
                break;
            }
        }
        // 返回上传成功数量
        return uploadCount;
    }

    @Override
    public QueryWrapper<Picture> getPictureQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        Boolean nullSpaceId = pictureQueryRequest.getNullSpaceId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        Integer imgSize = pictureQueryRequest.getImgSize();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText).or().like("introduction", searchText));
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        // >= startEditTime
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        // < endEditTime
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        // 图片大小枚举
        if (ObjUtil.isNotEmpty(imgSize)) {
            // 获取图片大小枚举
            ImgSizeEnum imgSizeEnum = ImgSizeEnum.getEnumByValue(imgSize);
            if (imgSizeEnum != null) {
                queryWrapper.ge("picWidth * picHeight", imgSizeEnum.getMinPixels());
                queryWrapper.lt("picWidth * picHeight", imgSizeEnum.getMaxPixels());
            }
        }
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 1 校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        // 图片id不能为空 且 审核枚举值存在 且 不允许将已通过和拒绝的状态改为待审核
        ThrowUtils.throwIf(id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum), ErrorCode.PARAMS_ERROR);
        // 2 判断图片是否存在
        Picture oldPicture = pictureRepository.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 3 判断审核状态是否重复
        Integer oldReviewStatus = oldPicture.getReviewStatus();
        ThrowUtils.throwIf(oldReviewStatus.equals(reviewStatus), ErrorCode.PARAMS_ERROR, "请勿重复审核");
        // 4 操作数据库
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = pictureRepository.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "数据库操作失败");


    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (loginUser.isAdmin()) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，无论是编辑或创建都是默认待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        String url = oldPicture.getUrl();
        Long count = pictureRepository.lambdaQuery().eq(Picture::getUrl, url).count();
        // 仅当没有其他图片使用该url时，才删除文件
        if (count > 1) {
            return;
        }
        // 删除图片文件
        cosManager.deletePictureObject(url);
        // 删除缩略图文件
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deletePictureObject(thumbnailUrl);
        }
    }

    @Async
    @Override
    public void clearPictureFileBatch(List<Picture> pictureList) {
        // 删除图片文件
        List<DeleteObjectsRequest.KeyVersion> urlList =
                pictureList.stream().map(picture -> new DeleteObjectsRequest.KeyVersion(picture.getUrl())).filter(pictureUrl -> {
            Long count = pictureRepository.lambdaQuery().eq(Picture::getUrl, pictureUrl.getKey()).count();
            // 仅当没有其他图片使用该url时，才删除文件
            return count <= 1;
        }).collect(Collectors.toList());
        cosManager.deletePictureObjectBatch(urlList);
        // 删除缩略图文件
        List<DeleteObjectsRequest.KeyVersion> thumbnailUrlList =
                pictureList.stream().map(picture -> new DeleteObjectsRequest.KeyVersion(picture.getThumbnailUrl())).filter(ObjUtil::isNotEmpty).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(thumbnailUrlList)) {
            cosManager.deletePictureObjectBatch(thumbnailUrlList);
        }
    }

    @Override
    public void removePictureBySpaceId(Long spaceId) {
        // 查询spaceId对应的图片
        List<Picture> pictureList = pictureRepository.lambdaQuery().eq(Picture::getSpaceId, spaceId).list();
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = pictureRepository.removeBatchByIds(pictureList);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "空间图片删除失败");
            // 清理空间图片文件
            this.clearPictureFileBatch(pictureList);
            return true;
        });
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
        // 若查询的是个人空间，校验空间是否存在
        Space space = null;
        if (spaceId != null) {
            space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            // 校验权限，只有空间创建者能进行颜色搜索
            ThrowUtils.throwIf(!space.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限进行颜色搜索");
        }
        // 获取空间的图片列表，必须要有主色调
        List<Picture> pictureList = new ArrayList<>();
        if (space != null) {
            // 私有空间
            pictureList = pictureRepository.lambdaQuery().eq(Picture::getSpaceId, spaceId).isNotNull(Picture::getPicColor).list();
        } else {
            // 公共空间
            pictureList = pictureRepository.lambdaQuery().isNull(Picture::getSpaceId).isNotNull(Picture::getPicColor).list();
        }
        // 如果图片列表为空，直接返回
        if (CollUtil.isEmpty(pictureList)) {
            return new ArrayList<>();
        }
        List<Picture> sortedPictureList = sortPictureByColor(picColor, pictureList);
        return sortedPictureList.stream().map(PictureVO::objToVO).collect(Collectors.toList());
    }


    @Override
    public void validPicturePageSortByColor(Page<Picture> picturePage, String picColor) {
        List<Picture> pictureList = picturePage.getRecords();
        // 若颜色存在，按颜色排序
        if (StrUtil.isNotBlank(picColor)) {
            pictureList = this.sortPictureByColor(picColor, pictureList);
        }
        picturePage.setRecords(pictureList);
    }

    @Override
    public List<Picture> sortPictureByColor(String picColor, List<Picture> pictureList) {
        // 将颜色字符串转为主色调
        Color targetColor = Color.decode(picColor);
        // 根据相似度排序
        double minSimilarity = 0.80; // 相似度阈值：>= 0.80 才保留

        return pictureList.stream().map(picture -> {
                    String hexColor = picture.getPicColor();
                    try {
                        Color pictureColor = Color.decode(hexColor);
                        double similarity = ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                        return new AbstractMap.SimpleEntry<>(picture, similarity);
                    } catch (Exception e) {
                        return null; // 非法颜色值（如格式不对）直接过滤
                    }
                }).filter(Objects::nonNull).filter(entry -> entry.getValue() >= minSimilarity) // 阈值过滤
                .sorted(Map.Entry.<Picture, Double>comparingByValue().reversed()) // 相似度高的在前
                .map(AbstractMap.SimpleEntry::getKey).collect(Collectors.toList());
    }

    @Override
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        // 获取参数
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        List<String> tagList = pictureEditByBatchRequest.getTags();
        Boolean nullSpaceId = pictureEditByBatchRequest.getNullSpaceId();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        String nameRule = pictureEditByBatchRequest.getNameRule();
        // 参数校验
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR, "图片ID列表不能为空");
        // 获取图片列表（只取需要的参数）
        List<Picture> pictureList = pictureRepository.lambdaQuery()
                .select(Picture::getSpaceId, Picture::getId)
                .in(Picture::getId, pictureIdList)
                .eq(ObjUtil.isNotEmpty(spaceId), Picture::getSpaceId, spaceId)
                .isNull(nullSpaceId, Picture::getSpaceId)
                .list();
        // 设置分类和标签
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tagList)) {
                picture.setTags(JSONUtil.toJsonStr(tagList));
            }

        });
        // 批量重命名
        fillPictureWithNameRule(pictureList, nameRule);
        // 操作数据库批量更新图片信息
        boolean result = pictureRepository.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片批量更新失败");
    }

    private static void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String newName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(newName);
            }
        } catch (Exception e) {
            log.error("命名解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "命名解析错误");
        }
    }


    @Override
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        // 获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(pictureRepository.getById(pictureId)).orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "图片不存在"));
        // 校验权限，已经改为使用注解鉴权
//        this.validPictureAuth(picture, loginUser);
        // 构建请求参数
        CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
        BeanUtils.copyProperties(createPictureOutPaintingTaskRequest, taskRequest);
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        taskRequest.setInput(input);
        // 调用接口创建扩展任务
        return aliYunAiApi.createOutPaintingTask(taskRequest);
    }

}




