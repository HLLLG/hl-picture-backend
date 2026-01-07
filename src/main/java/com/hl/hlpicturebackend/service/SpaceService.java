package com.hl.hlpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hl.hlpicturebackend.model.dto.space.SpaceAddRequest;
import com.hl.hlpicturebackend.model.dto.space.SpaceQueryRequest;
import com.hl.hlpicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hl.hlpicturebackend.model.entity.User;
import com.hl.hlpicturebackend.model.vo.SpaceVO;

/**
* @author hegl
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2026-01-05 01:03:33
*/
public interface SpaceService extends IService<Space> {

    /**
     * 校验空间
     *
     * @param space
     */
    void validSpace(Space space, boolean isAdd);

    /**
     * 添加空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 删除空间
     * @param spaceId
     * @param loginUser
     */
    void deleteSpace(Long spaceId, User loginUser);

    /**
     * 获取空间包装类
     *
     * @param space
     * @return
     */
    SpaceVO getSpaceVO(Space space);

    /**
     * 获取空间包装类列表
     *
     * @param page
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> page);


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
}
