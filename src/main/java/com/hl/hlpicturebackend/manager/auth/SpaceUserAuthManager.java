package com.hl.hlpicturebackend.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONUtil;
import com.hl.hlpicturebackend.manager.auth.model.SpaceUserAuthConfig;
import com.hl.hlpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.hl.hlpicturebackend.manager.auth.model.SpaceUserRole;
import com.hl.hlpicture.domain.space.entity.Space;
import com.hl.hlpicture.domain.space.entity.SpaceUser;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.domain.space.valueobject.SpaceRoleEnum;
import com.hl.hlpicture.domain.space.valueobject.SpaceTypeEnum;
import com.hl.hlpicture.application.service.SpaceUserApplicationService;
import com.hl.hlpicture.application.service.UserApplicationService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 空间成员权限管理器
 *
 */
@Component
public class SpaceUserAuthManager {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    /**
     * 根据角色获取权限列表
     *
     * @param spaceUserRole 角色键
     * @return 权限列表
     */
    public List<String> getPermissionsByRole(String spaceUserRole) {
        if (spaceUserRole == null) {
            return new ArrayList<>();
        }
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles()
                .stream()
                .filter(r -> r.getKey().equals(spaceUserRole))
                .findFirst()
                .orElse(null);
        if (role == null) {
            return new ArrayList<>();
        }
        return role.getPermissions();
    }

    /**
     * 获取权限列表
     *
     * @param space     空间实体
     * @param loginUser 登录用户实体
     * @return 权限列表
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if (space == null) {
            // 系统管理员
            if (loginUser.isAdmin()) {
                return ADMIN_PERMISSIONS;
            } else {
                return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
            }
        }
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }
        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间，只有空间创建者拥有全部权限
                if (space.getUserId().equals(loginUser.getId())) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                // 团队空间，查询用户的空间成员角色，返回对应权限
                SpaceUser spaceUser = spaceUserApplicationService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                }
                return getPermissionsByRole(spaceUser.getSpaceRole());
        }
        return new ArrayList<>();
    }

}
