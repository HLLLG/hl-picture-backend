package com.hl.hlpicture.interfaces.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hl.hlpicture.application.service.PictureApplicationService;
import com.hl.hlpicture.application.service.UserApplicationService;
import com.hl.hlpicture.domain.picture.entity.Picture;
import com.hl.hlpicture.domain.picture.valueobject.PictureReviewStatusEnum;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.infrastructure.annotation.AuthCheck;
import com.hl.hlpicture.infrastructure.api.aliyunai.AliYunAiApi;
import com.hl.hlpicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.hl.hlpicture.infrastructure.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.hl.hlpicture.infrastructure.api.imagesearch.ImageSearchFacade;
import com.hl.hlpicture.infrastructure.api.imagesearch.model.ImageSearchResult;
import com.hl.hlpicture.infrastructure.common.BaseResponse;
import com.hl.hlpicture.infrastructure.common.DeleteRequest;
import com.hl.hlpicture.infrastructure.common.ResultUtils;
import com.hl.hlpicture.infrastructure.exception.ErrorCode;
import com.hl.hlpicture.infrastructure.exception.ThrowUtils;
import com.hl.hlpicture.interfaces.assembler.PictureAssembler;
import com.hl.hlpicture.interfaces.dto.picture.*;
import com.hl.hlpicture.interfaces.vo.picture.PictureTagCategory;
import com.hl.hlpicture.interfaces.vo.picture.PictureVO;
import com.hl.hlpicturebackend.constant.UserConstant;
import com.hl.hlpicturebackend.manager.auth.SpaceUserAuthManager;
import com.hl.hlpicturebackend.manager.auth.StpKit;
import com.hl.hlpicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.hl.hlpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.hl.hlpicture.domain.space.entity.Space;
import com.hl.hlpicture.application.service.SpaceApplicationService;
import lombok.extern.slf4j.Slf4j;
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
    private PictureApplicationService pictureApplicationService;
    @Resource
    private UserApplicationService userApplicationService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SpaceApplicationService spaceApplicationService;
    @Resource
    private AliYunAiApi aliYunAiApi;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 本地缓存
     */
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder().initialCapacity(1024) // 初始缓存数量
            .maximumSize(10_000L) // 最大缓存数量
            .expireAfterWrite(Duration.ofMinutes(5)).build();

    /**
     * 上传图片（可重新上传）
     *
     * @param multipartFile
     * @return
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(@RequestParam("file") MultipartFile multipartFile,
                                                 PictureUploadRequest uploadPictureRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(multipartFile == null || uploadPictureRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        PictureVO pictureVO = pictureApplicationService.uploadPicture(multipartFile, uploadPictureRequest, loginUser);
        return ResultUtils.success(pictureVO);

    }

    /**
     * 通过 url 上传图片（可重新上传）
     *
     * @param uploadPictureRequest
     * @return
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest uploadPictureRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(uploadPictureRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        String fileUrl = uploadPictureRequest.getFileUrl();
        PictureVO pictureVO = pictureApplicationService.uploadPicture(fileUrl, uploadPictureRequest, loginUser);
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
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest uploadByBatchRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(uploadByBatchRequest), ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        int uploadCount = pictureApplicationService.uploadPictureByBatch(uploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);

    }


    /**
     * 根据id删除图片
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        Long pictureId = deleteRequest.getId();
        ThrowUtils.throwIf(deleteRequest == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.deletePicture(pictureId, loginUser);
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
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditRequest == null || pictureEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 判断用户是否登录
        User loginUser = userApplicationService.getLoginUser(request);
        Picture picture = PictureAssembler.toPictureEntity(pictureEditRequest);
        pictureApplicationService.editPicture(picture, loginUser);
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
        User loginUser = userApplicationService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() <= 0, ErrorCode.NOT_LOGIN_ERROR);
        // 判断图片是否存在
        Picture oldPicture = pictureApplicationService.getById(pictureUpdateRequest.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 将实体类和dto 进行转换
        Picture picture = PictureAssembler.toPictureEntity(pictureUpdateRequest);
        // 数据校验
        pictureApplicationService.validPicture(picture);
        // 补充审核参数
        pictureApplicationService.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = pictureApplicationService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据id获取图片（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureApplicationService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅空间创建者可查看私有空间图片
        Long spaceId = picture.getSpaceId();
        Space space = null;
        if (spaceId != null) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR, "无权限访问该空间图片");
            space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 已经改为使用注解鉴权
            //pictureService.validPictureAuth(picture, loginUser);
        }
        // 获取当前用户
        User loginUser = userApplicationService.getLoginUser(request);
        // 获取权限列表
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        // 脱敏
        PictureVO pictureVO = pictureApplicationService.getPictureVO(picture);
        pictureVO.setPermissionList(permissionList);
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
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        Page<Picture> picturePage = pictureApplicationService.page(new Page<>(current, pageSize),
                pictureApplicationService.getPictureQueryWrapper(pictureQueryRequest));
        List<Picture> pictureList = picturePage.getRecords();
        // 判断是否按颜色排序
        pictureApplicationService.validPicturePageSortByColor(picturePage, pictureQueryRequest.getPicColor());
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     *
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
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
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR, "无权限访问该空间图片");
            // 私有空间
            // User loginUser = userService.getLoginUser(request);
            // 校验空间权限
            // Space space = spaceService.getById(spaceId);
            // ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // ThrowUtils.throwIf(!space.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限访问该空间图片");
            pictureQueryRequest.setNullSpaceId(false);
        }
        // 确保分页查询也能正确路由到所有相关分片
        QueryWrapper<Picture> wrapper = pictureApplicationService.getPictureQueryWrapper(pictureQueryRequest);

        // 打印实际执行的 SQL 进行调试
        log.info("Query wrapper: {}", wrapper.getTargetSql());

        Page<Picture> picturePage = pictureApplicationService.page(new Page<>(current, pageSize), wrapper);

        // 判断是否按颜色排序
        pictureApplicationService.validPicturePageSortByColor(picturePage, pictureQueryRequest.getPicColor());
        // 获取封装类
        return ResultUtils.success(pictureApplicationService.getPictureVOPage(picturePage));
    }

    /**
     * 分页获取图片列表（封装类）缓存
     *
     * @param pictureQueryRequest
     * @return
     */
    @Deprecated
    @PostMapping("/list/page/vo/batch")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithBatch(@RequestBody PictureQueryRequest pictureQueryRequest) {
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
        Page<Picture> picturePage = pictureApplicationService.page(new Page<>(current, pageSize),
                pictureApplicationService.getPictureQueryWrapper(pictureQueryRequest));
        // 获取封装类
        Page<PictureVO> pictureVOPage = pictureApplicationService.getPictureVOPage(picturePage);
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
     *
     * @param pictureReviewRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/review")
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null || pictureReviewRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 以图搜图
     *
     * @param searchPictureByPictureRequest
     * @return
     */
    @PostMapping("/search/picture")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureApplicationService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        return ResultUtils.success(ImageSearchFacade.searchImages(picture.getUrl()));
    }

    /**
     * 按照颜色搜图
     *
     * @param searchPictureByColorRequest
     * @return
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        String picColor = searchPictureByColorRequest.getPicColor();
        return ResultUtils.success(pictureApplicationService.searchPictureByColor(spaceId, picColor, loginUser));
    }

    /**
     * 图片批量编辑
     *
     * @param pictureEditByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest,
                                                    HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        // 获取空间id
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        if (spaceId == null) {
            // 若空间id为空，表示编辑公共空间图片，只有管理员可以操作
            ThrowUtils.throwIf(!loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE), ErrorCode.NO_AUTH_ERROR,
                    "无权限操作公共空间图片");
            pictureEditByBatchRequest.setNullSpaceId(true);
        } else {
            // 校验空间权限
            Space space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            // 仅空间创建者能操作
            ThrowUtils.throwIf(!loginUser.getId().equals(space.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权限操作该空间");
            pictureEditByBatchRequest.setNullSpaceId(false);
        }
        pictureApplicationService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 创建 AI 扩图任务
     *
     * @param createPictureOutPaintingTaskRequest
     * @param request
     * @return
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(@RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null, ErrorCode.PARAMS_ERROR);
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        CreateOutPaintingTaskResponse response =
                pictureApplicationService.createOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 获取 AI 扩图任务结果
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(@RequestParam String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse response = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(response);
    }
}
