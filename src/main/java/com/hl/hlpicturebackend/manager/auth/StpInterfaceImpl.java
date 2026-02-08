package com.hl.hlpicturebackend.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.hl.hlpicture.application.service.PictureApplicationService;
import com.hl.hlpicture.domain.picture.entity.Picture;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.infrastructure.exception.ErrorCode;
import com.hl.hlpicture.infrastructure.exception.ThrowUtils;
import com.hl.hlpicturebackend.constant.UserConstant;
import com.hl.hlpicture.domain.space.entity.Space;
import com.hl.hlpicture.domain.space.entity.SpaceUser;
import com.hl.hlpicture.domain.space.valueobject.SpaceRoleEnum;
import com.hl.hlpicture.domain.space.valueobject.SpaceTypeEnum;
import com.hl.hlpicture.application.service.SpaceApplicationService;
import com.hl.hlpicture.application.service.SpaceUserApplicationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

    @Resource
    private PictureApplicationService pictureApplicationService;


    @Resource
    private SpaceApplicationService spaceApplicationService;

    /**
     * 返回一个账号所拥有的权限码集合 
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 判断 loginType, 仅对类型为"space” 进行权限校验
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return new ArrayList<>();
        }
        // 管理员权限，表示权限校验通过
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 获取上下文对象
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        // 如果所有字段为空，表示查询公共图库，可以通过
        if (isAllFieldsNull(authContext)) {
            return ADMIN_PERMISSIONS;
        }
        // 判断用户是否登录
        User loginUer = (User) StpKit.SPACE.getSession().get(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(loginUer == null, ErrorCode.NO_AUTH_ERROR, "用户未登录");
        Long userId = loginUer.getId();
        // 优先从上下文中获取spaceUser对象
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
        // 如果有spaceUserId, 必然是团队空间， 通过数据库查询 SpaceUser 对象
        Long spaceUserId = authContext.getSpaceUserId();
        if (ObjUtil.isNotEmpty(spaceUserId)) {
            spaceUser = spaceUserApplicationService.getById(spaceUserId);
            ThrowUtils.throwIf(spaceUser == null, ErrorCode.NO_AUTH_ERROR, "未找到空间用户信息");
            // 取出当前用户对应的spaceUser对象
            SpaceUser loginSpaceUser = spaceUserApplicationService.lambdaQuery()
                    .eq(SpaceUser::getUserId, userId)
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .one();
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }
            // 返回权限列表
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }
        // 若如果有spaceUserId不存在，尝试根据 spaceId 或 pictureId 获取 Space 对象
        Long spaceId = authContext.getSpaceId();
        // 公共图库
        if (ObjUtil.isEmpty(spaceId)) {
            // 如果没有 spaceId，则尝试通过 pictureId 获取 space
            Long pictureId = authContext.getPictureId();
            // 如果 pictureId 为空，默认通过权限校验
            if (ObjUtil.isEmpty(pictureId)) {
                return ADMIN_PERMISSIONS;
            }
            Picture picture = pictureApplicationService.getById(pictureId);
            ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
            spaceId = picture.getSpaceId();
            // 公共图库，上传者或管理员返回管理员权限
            if (spaceId == null) {
                if (picture.getUserId().equals(userId) || loginUer.isAdmin()) {
                    return ADMIN_PERMISSIONS;
                } else {
                    // 不是自己的图片，且非管理员，无权限
                    return new ArrayList<>();
                }
            }
        }
        // 获取 Space 对象
        Space space = spaceApplicationService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");
        // 私有空间，仅空间创建者或管理员有管理员权限
        if (SpaceTypeEnum.PRIVATE.getValue() == space.getSpaceType()) {
            if (space.getUserId().equals(userId) || loginUer.isAdmin()) {
                return ADMIN_PERMISSIONS;
            } else {
                return new ArrayList<>();
            }
        } else {
            // 团队空间，查询当前用户的 SpaceUser 对象
            spaceUser = spaceUserApplicationService.lambdaQuery()
                    .eq(SpaceUser::getUserId, userId)
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .one();
            // 未加入空间，无权限
            if (spaceUser == null) {
                return new ArrayList<>();
            }
            // 返回权限列表
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return new ArrayList<String>();
    }

    /**
     * 获取请求中的空间用户认证上下文
     * @return
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authContext;
        // 根据Content-Type解析请求参数
        if (ContentType.JSON.toString().equals(contentType)) {
            // post请求
            String body = ServletUtil.getBody(request);
            authContext = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            // get请求
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authContext = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        // 根据请求路劲区分 id 字段的含义
        Long id = authContext.getId();
        if (ObjUtil.isNotEmpty(id)) {
            String requestPath = request.getRequestURI();
            // 去除context-path
            String partUri = StrUtil.removePrefix(requestPath, contextPath + "/");
            String moduleName = StrUtil.subBefore(partUri, "/", false);
            switch (moduleName) {
                case "space":
                    authContext.setSpaceId(id);
                    break;
                case "spaceUser":
                    authContext.setSpaceUserId(id);
                    break;
                case "picture":
                    authContext.setPictureId(id);
                    break;
                default:
            }
        }
        return authContext;
    }

    /**
     * 判断认证上下文对象的所有字段是否均为 null
     * @param object
     * @return
     */
    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true;
        }
        // 获取对象的所有字段值，判断是否全部为 null 或 空
        return Arrays.stream(ReflectUtil.getFields(SpaceUserAuthContext.class))
                // 获取字段值
                .map(field -> ReflectUtil.getFieldValue(object, field))
                // 判断字段值是否为空
                .allMatch(ObjUtil::isEmpty);
    }

}
