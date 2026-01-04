package com.hl.hlpicturebackend.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hl.hlpicturebackend.annotation.AuthCheck;
import com.hl.hlpicturebackend.common.BaseResponse;
import com.hl.hlpicturebackend.common.DeleteRequest;
import com.hl.hlpicturebackend.common.ResultUtils;
import com.hl.hlpicturebackend.constant.UserConstant;
import com.hl.hlpicturebackend.exception.ErrorCode;
import com.hl.hlpicturebackend.exception.ThrowUtils;
import com.hl.hlpicturebackend.model.dto.picture.*;
import com.hl.hlpicturebackend.model.entity.Picture;
import com.hl.hlpicturebackend.model.entity.Space;
import com.hl.hlpicturebackend.model.entity.User;
import com.hl.hlpicturebackend.model.enums.PictureReviewStatusEnum;
import com.hl.hlpicturebackend.model.vo.PictureTagCategory;
import com.hl.hlpicturebackend.model.vo.PictureVO;
import com.hl.hlpicturebackend.service.PictureService;
import com.hl.hlpicturebackend.service.SpaceService;
import com.hl.hlpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SpaceService spaceService;

    /**
     * 本地缓存
     */
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024) // 初始缓存数量
            .maximumSize(10_000L) // 最大缓存数量
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    /**
     * 上传图片（可重新上传）
     *
     * @param multipartFile
     * @return
     */
    @PostMapping("/upload")
    public BaseResponse<PictureVO> UploadPicture(@RequestParam("file") MultipartFile multipartFile,
                                                 PictureUploadRequest uploadPictureRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(multipartFile == null || uploadPictureRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, uploadPictureRequest, loginUser);
        return ResultUtils.success(pictureVO);

    }

    /**
     * 通过 url 上传图片（可重新上传）
     *
     * @param uploadPictureRequest
     * @return
     */
    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> UploadPictureByUrl(@RequestBody PictureUploadRequest uploadPictureRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(uploadPictureRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        String fileUrl = uploadPictureRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, uploadPictureRequest, loginUser);
        return ResultUtils.success(pictureVO);

    }

    /**
     * 批量上传上传图片（仅管理员）
     *
     * @param uploadByBatchRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/upload/batch")
    public BaseResponse<Integer> UploadPictureByBatch(@RequestBody PictureUploadByBatchRequest uploadByBatchRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(uploadByBatchRequest), ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(uploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);

    }


    /**
     * 根据id删除图片
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        Long pictureId = deleteRequest.getId();
        ThrowUtils.throwIf(deleteRequest == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.deletePicture(pictureId, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 编辑图片（给用户使用）
     *
     * @param pictureEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditRequest == null || pictureEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 判断用户是否登录
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(true);
    }


    /**
     * 更新图片（仅管理员使用）
     *
     * @param pictureUpdateRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/update")
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,
                                               HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 判断用户是否登录
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() <= 0, ErrorCode.NOT_LOGIN_ERROR);
        // 判断图片是否存在
        Picture oldPicture = pictureService.getById(pictureUpdateRequest.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 将实体类和dto 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 将list 数组转化为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.validPicture(picture);
        // 补充审核参数
        pictureService.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据id获取图片（封装类）
     *
     * @param id
     * @return
     */
    @PostMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅空间创建者可查看私有空间图片
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            // 获取当前用户
            User loginUser = userService.getLoginUser(request);
            pictureService.validPictureAuth(picture, loginUser);
        }
        // 脱敏
        PictureVO pictureVO = pictureService.getPictureVO(picture);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 分页获取图片信息（仅管理员可用）
     *
     * @param pictureQueryRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/list/page")
    public BaseResponse<Page<Picture>> ListPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getPictureQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     *
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> ListPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) {
            // 公共空间
            // 普通用户默认只能看到审核用过的类型
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            // 私有空间
            User loginUser = userService.getLoginUser(request);
            // 校验空间权限
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            ThrowUtils.throwIf(!space.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限访问该空间图片");
        }
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getPictureQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage));
    }

    /**
     * 分页获取图片列表（封装类）缓存
     *
     * @param pictureQueryRequest
     * @return
     */
    @Deprecated
    @PostMapping("/list/page/vo/batch")
    public BaseResponse<Page<PictureVO>> ListPictureVOByPageWithBatch(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        // 普通用户默认只能看到审核用过的类型
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        // 查询缓存，缓存中没有再查询数据库
        // 构建缓存的key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = String.format("hlpicture:ListPictureVOByPage:%s", hashKey);
        // 1、先查本地缓存
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            // 命中缓存，直接返回
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        // 2、如果本地缓存未命中，再查 Redis 缓存
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        cachedValue = opsForValue.get(cacheKey);
        if (cachedValue != null) {
            // 写入本地缓存
            LOCAL_CACHE.put(cacheKey, cachedValue);
            // 命中缓存，直接返回
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getPictureQueryWrapper(pictureQueryRequest));
        // 获取封装类
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage);
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        // 写入本地缓存
        LOCAL_CACHE.put(cacheKey, cacheValue);
        // 写入redis缓存，设置过期时间为 5-10 分钟，防止缓存雪崩
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
        opsForValue.set(cacheKey, cacheValue, cacheExpireTime);
        return ResultUtils.success(pictureVOPage);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 审核图片
     * @param pictureReviewRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null || pictureReviewRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

}
