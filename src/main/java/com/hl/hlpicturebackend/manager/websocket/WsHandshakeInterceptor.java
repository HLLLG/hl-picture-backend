package com.hl.hlpicturebackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import com.hl.hlpicture.application.service.PictureApplicationService;
import com.hl.hlpicturebackend.manager.auth.SpaceUserAuthManager;
import com.hl.hlpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.hl.hlpicture.domain.picture.entity.Picture;
import com.hl.hlpicture.domain.space.entity.Space;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.domain.space.valueobject.SpaceTypeEnum;
import com.hl.hlpicture.application.service.SpaceApplicationService;
import com.hl.hlpicture.application.service.UserApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private PictureApplicationService pictureApplicationService;

    @Resource
    private SpaceApplicationService spaceApplicationService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 握手前拦截
     * @param request
     * @param response
     * @param wsHandler
     * @param attributes 给 Session 会话设置属性
     * @return
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler
            , Map<String, Object> attributes)  {
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

            // 获取请求参数
            Long pictureId = Long.valueOf(servletRequest.getParameter("pictureId"));
            if (ObjUtil.isEmpty(pictureId) || pictureId <= 0) {
                log.error("WebSocket 握手失败，缺少 pictureId 参数");
                return false;
            }
            // 根据 pictureId 获取图片信息
            Picture picture = pictureApplicationService.getById(pictureId);
            if (picture == null) {
                log.error("WebSocket 握手失败，图片不存在，pictureId={}", pictureId);
                return false;
            }
            // 获取当前用户
            User loginUser = userApplicationService.getLoginUser(servletRequest);
            if (loginUser == null) {
                log.error("WebSocket 握手失败，用户未登录");
                return false;
            }
            Long spaceId = picture.getSpaceId();
            Space space = null;
            if (spaceId != null) {
                // 获取空间
                space = spaceApplicationService.getById(spaceId);
                if (space == null) {
                    log.error("WebSocket 握手失败，空间不存在，spaceId={}", spaceId);
                    return false;
                }
                if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
                    log.error("不是团队空间，拒绝握手");
                }
            }
            // 校验用户是否有编辑权限
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("WebSocket 握手失败，用户没有图片编辑权限，userId={}, pictureId={}", loginUser.getId(), pictureId);
                return false;
            }
            // 设置用户信息等属性到 Websocket中
            attributes.put("userId", loginUser.getId());
            attributes.put("pictureId", pictureId);
            attributes.put("user", loginUser);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                               Exception exception) {

    }
}
