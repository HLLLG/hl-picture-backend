package com.hl.hlpicture.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hl.hlpicture.interfaces.dto.space.SpaceAddRequest;
import com.hl.hlpicture.interfaces.dto.space.SpaceQueryRequest;
import com.hl.hlpicture.domain.space.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.interfaces.vo.space.SpaceVO;

/**
* @author hegl
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2026-01-05 01:03:33
*/
public interface SpaceApplicationService extends IService<Space> {


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

    /**
     * 校验空间权限
     *
     * @param space
     * @param loginUser
     */
    void checkSpaceAuth(Space space, User loginUser);
}
