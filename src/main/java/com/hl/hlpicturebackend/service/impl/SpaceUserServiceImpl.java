package com.hl.hlpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hl.hlpicturebackend.exception.ErrorCode;
import com.hl.hlpicturebackend.exception.ThrowUtils;
import com.hl.hlpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.hl.hlpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.hl.hlpicturebackend.model.entity.Space;
import com.hl.hlpicturebackend.model.entity.SpaceUser;
import com.hl.hlpicturebackend.model.entity.User;
import com.hl.hlpicturebackend.model.enums.SpaceRoleEnum;
import com.hl.hlpicturebackend.model.enums.SpaceTypeEnum;
import com.hl.hlpicturebackend.model.vo.SpaceUserVO;
import com.hl.hlpicturebackend.model.vo.SpaceVO;
import com.hl.hlpicturebackend.service.SpaceService;
import com.hl.hlpicturebackend.service.SpaceUserService;
import com.hl.hlpicturebackend.mapper.SpaceUserMapper;
import com.hl.hlpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author 21628
* @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
* @createDate 2026-01-29 17:09:02
*/
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService{

    @Resource
    @Lazy
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean isAdd) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR, "空间成员信息不能为空");
        // 创建时，空间ID和用户ID不能为空
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if (isAdd) {
            ThrowUtils.throwIf(ObjUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR, "空间ID和用户ID不能为空");
            // 校验空间是否存在
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            // 校验用户是否存在
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        // 校验角色是否合法
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        ThrowUtils.throwIf(spaceRole != null && spaceRoleEnum == null, ErrorCode.PARAMS_ERROR, "空间角色不合法");
    }

    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddRequest, spaceUser);
        // 校验参数
        this.validSpaceUser(spaceUser, true);
        // 插入数据
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "添加空间成员失败");
        return spaceUser.getId();
    }

    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser) {
        if (spaceUser == null) {
            return null;
        }
        SpaceUserVO spaceUserVO = new SpaceUserVO();
        BeanUtils.copyProperties(spaceUser, spaceUserVO);
        // 获取用户信息
        Long userId = spaceUserVO.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            spaceUserVO.setUser(userService.getUserVO(user));
        }
        // 获取空间信息
        Long spaceId = spaceUserVO.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            spaceUserVO.setSpace(spaceService.getSpaceVO(space));
        }
        return spaceUserVO;
    }

    @Override
    public List<SpaceUserVO> getSpaceVOList(List<SpaceUser> spaceList) {
        if (CollUtil.isEmpty(spaceList)) {
            return new ArrayList<>();
        }
        List<SpaceUserVO> spaceUserVOList = spaceList.stream()
                .map(SpaceUserVO::objToVo)
                .collect(Collectors.toList());
        // 获取用户id集合
        Set<Long> userIdSet = spaceUserVOList.stream()
                .map(SpaceUserVO::getUserId)
                .filter(ObjUtil::isNotEmpty)
                .collect(Collectors.toSet());
        // 获取空间id集合
        Set<Long> spaceIdSet = spaceUserVOList.stream()
                .map(SpaceUserVO::getSpaceId)
                .filter(ObjUtil::isNotEmpty)
                .collect(Collectors.toSet());
        // 批量获取用户信息
        Map<Long, List<User>> userIdMap = userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));
        // 批量获取空间信息
        Map<Long, List<Space>> spaceIdMap = spaceService.listByIds(spaceIdSet)
                .stream()
                .collect(Collectors.groupingBy(Space::getId));
        // 填充用户和空间信息
        spaceUserVOList.forEach(spaceUserVO -> {
            // 填充用户信息
            Long userId = spaceUserVO.getUserId();
            User user = null;
            if (userIdMap.containsKey(userId)) {
                user = userIdMap.get(userId).get(0);
            }
            spaceUserVO.setUser(userService.getUserVO(user));
            // 填充空间信息
            Long spaceId = spaceUserVO.getSpaceId();
            Space space = null;
            if (spaceIdMap.containsKey(spaceId)) {
                space = spaceIdMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpace(spaceService.getSpaceVO(space));
        });
        return spaceUserVOList;
    }

    @Override
    public QueryWrapper<SpaceUser> getSpaceUserQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象取值
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(StrUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
        return queryWrapper;
    }
}




