package com.hl.hlpicture.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hl.hlpicture.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.hl.hlpicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.hl.hlpicture.domain.space.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hl.hlpicture.interfaces.vo.space.SpaceUserVO;

import java.util.List;

/**
* @author 21628
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2026-01-29 17:09:02
*/
public interface SpaceUserApplicationService extends IService<SpaceUser> {

    /**
     * 校验空间成员对象
     *
     * @param spaceUser
     */
    void validSpaceUser(SpaceUser spaceUser, boolean isAdd);

    /**
     * 添加空间成员
     *
     * @param spaceUserAddRequest
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);


    /**
     * 获取空间成员关联包装类
     *
     * @param spaceUser
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser);

    /**
     * 获取空间成员包装类列表
     *
     * @param spaceList
     * @return
     */
    List<SpaceUserVO> getSpaceVOList(List<SpaceUser> spaceList);


    /**
     * 构造查询空间QueryWrapper
     *
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getSpaceUserQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

}
