package com.hl.hlpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hl.hlpicturebackend.exception.ErrorCode;
import com.hl.hlpicturebackend.exception.ThrowUtils;
import com.hl.hlpicturebackend.mapper.SpaceMapper;
import com.hl.hlpicturebackend.model.dto.space.SpaceAddRequest;
import com.hl.hlpicturebackend.model.dto.space.SpaceQueryRequest;
import com.hl.hlpicturebackend.model.entity.Space;
import com.hl.hlpicturebackend.model.entity.User;
import com.hl.hlpicturebackend.model.enums.SpaceLevelEnum;
import com.hl.hlpicturebackend.model.vo.SpaceVO;
import com.hl.hlpicturebackend.model.vo.UserVO;
import com.hl.hlpicturebackend.service.SpaceService;
import com.hl.hlpicturebackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author hegl
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2026-01-05 01:03:33
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{

    @Resource
    private UserService userService;

    @Override
    public void validSpace(Space space, boolean isAdd) {
        ThrowUtils.throwIf(ObjUtil.isNull(space), ErrorCode.PARAMS_ERROR);
        // 从空间中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum levelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        if (isAdd) {
            // 添加时，空间名称不能为空
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR,
                    "空间名称不能为空");
            // 添加时，空间等级不能为空
            ThrowUtils.throwIf(spaceLevel == null, ErrorCode.PARAMS_ERROR, "空间等级不能为空");
        }
        // 修改时，空间名称不能为空且长度不能超过30个字符
        ThrowUtils.throwIf(StrUtil.isNotBlank(spaceName) && spaceName.length() > 30, ErrorCode.PARAMS_ERROR,
                "空间名称长度不能超过30个字符");
        // 修改时，空间等级必须是0-2之间的整数
        ThrowUtils.throwIf(spaceLevel != null && ObjUtil.isNull(levelEnum), ErrorCode.PARAMS_ERROR,
                "空间等级必须是0-2之间的整数");
    }

    @Override
    public Boolean addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 校验参数
        String spaceName = spaceAddRequest.getSpaceName();
        Integer spaceLevel = spaceAddRequest.getSpaceLevel();
        // 空间名称不能为空，且长度不能超过20个字符
        ThrowUtils.throwIf(StrUtil.isBlank(spaceName) || spaceName.length() > 20, ErrorCode.PARAMS_ERROR, "空间名称不能为空且长度不能超过20个字符");
        // 空间等级不能为空，且必须是0-2之间的整数
        SpaceLevelEnum levelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        ThrowUtils.throwIf(ObjUtil.isNull(levelEnum), ErrorCode.PARAMS_ERROR, "空间等级不能为空，且必须是0-2之间的整数");
        // 创建空间
        Space space = new Space();
        space.setSpaceName(spaceName);
        space.setSpaceLevel(spaceLevel);
        space.setMaxCount(levelEnum.getMaxCount());
        space.setMaxSize(levelEnum.getMaxSize());
        space.setUserId(loginUser.getId());
        boolean result = this.save(space);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "空间创建失败");
        return true;
    }

    @Override
    public SpaceVO getSpaceVO(Space space) {
        if (space == null) {
            return null;
        }
        SpaceVO spaceVO = SpaceVO.objToVO(space);
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUserVO(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage) {
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        List<Space> spaceList = spacePage.getRecords();
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVO).collect(Collectors.toList());
        // 获取用户信息
        Set<Long> userIdSet = spaceVOList.stream().map(SpaceVO::getUserId).collect(Collectors.toSet());
        Map<Long, UserVO> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.toMap(User::getId, userService::getUserVO));
        // 填充用户信息
        spaceVOList.forEach(spaceVO -> {
            spaceVO.setUserVO(userIdUserListMap.get(spaceVO.getUserId()));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getSpaceQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        Long id = spaceQueryRequest.getId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Long userId = spaceQueryRequest.getUserId();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.like(ObjUtil.isNotNull(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotNull(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        // 拼接排序条件
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), "ascend".equalsIgnoreCase(sortOrder), sortField);
        return queryWrapper;
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        if (spaceLevelEnum != null) {
            // 若管理员以指定空间最大值，则不覆盖
            Long maxCount = spaceLevelEnum.getMaxCount();
            Long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxCount(maxSize);
            }
            if (space.getMaxCount() == null) {
                space.setMaxSize(maxCount);
            }
        }
    }
}




