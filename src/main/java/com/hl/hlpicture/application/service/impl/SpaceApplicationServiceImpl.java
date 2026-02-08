package com.hl.hlpicture.application.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hl.hlpicture.application.service.SpaceApplicationService;
import com.hl.hlpicture.application.service.SpaceUserApplicationService;
import com.hl.hlpicture.application.service.UserApplicationService;
import com.hl.hlpicture.domain.space.entity.Space;
import com.hl.hlpicture.domain.space.entity.SpaceUser;
import com.hl.hlpicture.domain.space.service.SpaceDomainService;
import com.hl.hlpicture.domain.space.valueobject.SpaceLevelEnum;
import com.hl.hlpicture.domain.space.valueobject.SpaceRoleEnum;
import com.hl.hlpicture.domain.space.valueobject.SpaceTypeEnum;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.infrastructure.exception.ErrorCode;
import com.hl.hlpicture.infrastructure.exception.ThrowUtils;
import com.hl.hlpicture.infrastructure.mapper.SpaceMapper;
import com.hl.hlpicture.interfaces.dto.space.SpaceAddRequest;
import com.hl.hlpicture.interfaces.dto.space.SpaceQueryRequest;
import com.hl.hlpicture.interfaces.vo.space.SpaceVO;
import com.hl.hlpicture.interfaces.vo.user.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author hegl
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2026-01-05 01:03:33
*/
@Service
public class SpaceApplicationServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceApplicationService {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

    @Resource
    private SpaceDomainService spaceDomainService;

//    @Resource 注释分表
//    @Lazy
//    private DynamicShardingManager dynamicShardingManager;


    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 实体类转VO
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        space.fillDefaultValue();
        // 填充空间大小
        this.fillSpaceBySpaceLevel(space);
        // 校验参数
        space.validSpace(true);
        // 设置创建用户
        space.setUserId(loginUser.getId());
        // 权限校验，非管理员只能创建普通空间
        ThrowUtils.throwIf(SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel()
                && !loginUser.isAdmin(), ErrorCode.NO_AUTH_ERROR,
                "只有管理员才能创建专业版及以上空间");;
        // 控制同一用户只能创建一个私有空间，以及一个团队空间
        String lock = String.valueOf(loginUser.getId()).intern();
        synchronized (lock) {
            Long executeId = transactionTemplate.execute(status -> {
                // 判断是否已有空间
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, loginUser.getId())
                        .eq(Space::getSpaceType, space.getSpaceType())
                        .exists();
                // 如果有空间，就不能创建
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每类空间只能创建一个");
                // 创建
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "空间创建失败");
                // 如果是创建团队空间，则自动添加当前用户为成员
                if (space.getSpaceType() == SpaceTypeEnum.TEAM.getValue()) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(loginUser.getId());
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    result = spaceUserApplicationService.save(spaceUser);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
                }
                // 创建分表 (仅旗舰版团队空间) 为了方便使用，注释掉
//                dynamicShardingManager.createSpacePictureTable(space);
                return space.getId();
            });

            return Optional.ofNullable(executeId).orElse(-1L);
        }
    }

    @Override
    public void deleteSpace(Long spaceId, User loginUser) {
        // 判断空间是否存在
        Space space = this.getById(spaceId);
        ThrowUtils.throwIf(ObjUtil.isNull(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 校验权限
        this.checkSpaceAuth(space, loginUser);
        // 删除空间
        boolean result = this.removeById(spaceId);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "空间删除失败");
    }

    @Override
    public SpaceVO getSpaceVO(Space space) {
        if (space == null) {
            return null;
        }
        SpaceVO spaceVO = SpaceVO.objToVO(space);
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            UserVO userVO = userApplicationService.getUserVOById(userId);
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
        Map<Long, UserVO> userIdUserListMap = userApplicationService.listByIds(userIdSet).stream().collect(Collectors.toMap(User::getId, userApplicationService::getUserVO));
        // 填充用户信息
        spaceVOList.forEach(spaceVO -> {
            spaceVO.setUserVO(userIdUserListMap.get(spaceVO.getUserId()));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getSpaceQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        return spaceDomainService.getSpaceQueryWrapper(spaceQueryRequest);
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
       spaceDomainService.fillSpaceBySpaceLevel(space);
    }

    @Override
    public void checkSpaceAuth(Space space, User loginUser) {
        spaceDomainService.checkSpaceAuth(space, loginUser);
    }
}




