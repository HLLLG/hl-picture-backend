package com.hl.hlpicture.domain.space.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hl.hlpicture.domain.space.entity.Space;
import com.hl.hlpicture.domain.space.repository.SpaceRepository;
import com.hl.hlpicture.domain.space.service.SpaceDomainService;
import com.hl.hlpicture.domain.space.valueobject.SpaceLevelEnum;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.infrastructure.exception.ErrorCode;
import com.hl.hlpicture.infrastructure.exception.ThrowUtils;
import com.hl.hlpicture.interfaces.dto.space.SpaceQueryRequest;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author hegl
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2026-01-05 01:03:33
 */
@Service
public class SpaceDomainServiceImpl implements SpaceDomainService {

    @Resource
    private SpaceRepository spaceRepository;

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
        Integer spaceType = spaceQueryRequest.getSpaceType();
        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.like(ObjUtil.isNotNull(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotNull(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotNull(spaceType), "spaceType", spaceType);
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
                space.setMaxSize(maxSize);
            }
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    @Override
    public void checkSpaceAuth(Space space, User loginUser) {
        // 仅管理员和空间创建者可访问
        ThrowUtils.throwIf(!loginUser.isAdmin() && !space.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR);
    }
}




