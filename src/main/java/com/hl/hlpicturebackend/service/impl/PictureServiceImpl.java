package com.hl.hlpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hl.hlpicturebackend.exception.BusinessException;
import com.hl.hlpicturebackend.exception.ErrorCode;
import com.hl.hlpicturebackend.exception.ThrowUtils;
import com.hl.hlpicturebackend.manager.CosManager;
import com.hl.hlpicturebackend.manager.upload.FilePictureUpload;
import com.hl.hlpicturebackend.manager.upload.PictureUploadTemplate;
import com.hl.hlpicturebackend.manager.upload.UrlPictureUpload;
import com.hl.hlpicturebackend.mapper.PictureMapper;
import com.hl.hlpicturebackend.model.dto.file.UploadPictureResult;
import com.hl.hlpicturebackend.model.dto.picture.*;
import com.hl.hlpicturebackend.model.entity.Picture;
import com.hl.hlpicturebackend.model.entity.Space;
import com.hl.hlpicturebackend.model.entity.User;
import com.hl.hlpicturebackend.model.enums.ImgSizeEnum;
import com.hl.hlpicturebackend.model.enums.PictureReviewStatusEnum;
import com.hl.hlpicturebackend.model.vo.PictureVO;
import com.hl.hlpicturebackend.model.vo.UserVO;
import com.hl.hlpicturebackend.service.PictureService;
import com.hl.hlpicturebackend.service.SpaceService;
import com.hl.hlpicturebackend.service.UserService;
import com.hl.hlpicturebackend.utils.ColorSimilarUtils;
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
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private SpaceService spaceService;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private CosManager cosManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象取值
        Long pictureId = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(pictureId == null, ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public void deletePicture(Long pictureId, User loginUser) {
        // 判断图片是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        this.validPictureAuth(oldPicture, loginUser);
        // 开启事务
        Long finalSpaceId = oldPicture.getSpaceId();
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片删除失败");
            // 更新空间使用额度
            boolean update = spaceService.lambdaUpdate().eq(Space::getId, finalSpaceId).setSql("totalSize = " +
                    "totalSize" + " + " + oldPicture.getPicSize()).setSql("totalCount = totalCount - 1").update();
            ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "空间额度更新失败");
            return true;
        });
        // 清理图片文件
        this.clearPictureFile(oldPicture);
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 判断图片是否存在
        Picture oldPicture = this.getById(pictureEditRequest.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 验证用户是否有权限操作
        this.validPictureAuth(oldPicture, loginUser);
        // 将实体类和dto 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 将list 数组转化为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
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
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            // 仅空间创建者能上传到该空间
            ThrowUtils.throwIf(!loginUser.getId().equals(space.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权限上传到该空间");
            // 校验额度
            ThrowUtils.throwIf(space.getTotalSize() >= space.getMaxSize(), ErrorCode.OPERATION_ERROR, "空间存储已满，无法上传图片");
            ThrowUtils.throwIf(space.getTotalCount() >= space.getMaxCount(), ErrorCode.OPERATION_ERROR,
                    "空间图片数量已达上限，无法上传图片");
        }
        // 如果是更新，需要校验图片是否存在
        Picture oldPicture = null;
        if (pictureId != null) {
            oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.PARAMS_ERROR, "图片不存在");
            // 仅本人和管理员能更新
            ThrowUtils.throwIf(!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser),
                    ErrorCode.NO_AUTH_ERROR);
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
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片保存到数据库失败");
            // 仅当空间id不为空时，才更新空间使用额度
            if (finalSpaceId != null) {
                boolean update = spaceService.lambdaUpdate().eq(Space::getId, finalSpaceId).setSql("totalSize = " +
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
    public PictureVO getPictureVO(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVO pictureVO = PictureVO.objToVO(picture);
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUserVO(userVO);
        }
        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage) {
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getPages(),
                picturePage.getTotal());
        List<Picture> pictureList = picturePage.getRecords();
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 过滤未审核的图片
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVO).collect(Collectors.toList());
        // 1 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap =
                userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        // 2 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUserVO(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
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
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 3 判断审核状态是否重复
        Integer oldReviewStatus = oldPicture.getReviewStatus();
        ThrowUtils.throwIf(oldReviewStatus.equals(reviewStatus), ErrorCode.PARAMS_ERROR, "请勿重复审核");
        // 4 操作数据库
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "数据库操作失败");


    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
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
        Long count = this.lambdaQuery().eq(Picture::getUrl, url).count();
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
            Long count = this.lambdaQuery().eq(Picture::getUrl, pictureUrl.getKey()).count();
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
    public void validPictureAuth(Picture picture, User loginUser) {
        Long spaceId = picture.getSpaceId();
        Long loginUserId = loginUser.getId();
        if (spaceId == null) {
            // 公共空间，本人和管理员都能操作
            ThrowUtils.throwIf(!picture.getUserId().equals(loginUserId) && !userService.isAdmin(loginUser),
                    ErrorCode.NO_AUTH_ERROR);
        } else {
            // 私有空间，仅空间创建者可操作
            ThrowUtils.throwIf(!picture.getUserId().equals(loginUserId), ErrorCode.NO_AUTH_ERROR);
        }

    }

    @Override
    public void removePictureBySpaceId(Long spaceId) {
        // 查询spaceId对应的图片
        List<Picture> pictureList = this.lambdaQuery().eq(Picture::getSpaceId, spaceId).list();
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeBatchByIds(pictureList);
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
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            // 校验权限，只有空间创建者能进行颜色搜索
            ThrowUtils.throwIf(!space.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限进行颜色搜索");
        }
        // 获取空间的图片列表，必须要有主色调
        List<Picture> pictureList = new ArrayList<>();
        if (space != null) {
            // 私有空间
            pictureList = this.lambdaQuery().eq(Picture::getSpaceId, spaceId).isNotNull(Picture::getPicColor).list();
        } else {
            // 公共空间
            pictureList = this.lambdaQuery().isNull(Picture::getSpaceId).isNotNull(Picture::getPicColor).list();
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


}




