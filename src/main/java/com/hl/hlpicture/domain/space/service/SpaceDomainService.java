package com.hl.hlpicture.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hl.hlpicture.domain.space.entity.Space;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.interfaces.dto.space.SpaceQueryRequest;

/**
* @author hegl
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2026-01-05 01:03:33
*/
public interface SpaceDomainService {

    /**
     * 构造查询空间QueryWrapper
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getSpaceQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据空间等级填充空间信息
     *
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 校验空间权限
     *
     * @param space
     * @param loginUser
     */
    void checkSpaceAuth(Space space, User loginUser);
}
