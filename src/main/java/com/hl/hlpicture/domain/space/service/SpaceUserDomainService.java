package com.hl.hlpicture.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hl.hlpicture.domain.space.entity.SpaceUser;
import com.hl.hlpicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;

/**
* @author 21628
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2026-01-29 17:09:02
*/
public interface SpaceUserDomainService extends IService<SpaceUser> {


    /**
     * 构造查询空间QueryWrapper
     *
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getSpaceUserQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

}
