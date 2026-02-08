package com.hl.hlpicturebackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.hl.hlpicturebackend.manager.websocket.disruptor.PictureEditEventProducer;
import com.hl.hlpicturebackend.manager.websocket.model.PictureEditActionEnum;
import com.hl.hlpicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.hl.hlpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.hl.hlpicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.application.service.UserApplicationService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * websocket 处理器
 */
@Component
public class PictureEditHandler extends TextWebSocketHandler {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    @Lazy
    private PictureEditEventProducer pictureEditEventProducer;

    // 每张图片的编辑状态：key：pictureID，value：当前正在编辑的用户ID
    private final Map<Long, Long> pictureEditUsers = new ConcurrentHashMap<>();

    // 保存所有连接的会话， key:pictureId, value: WebSocketSession
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    /**
     * 建立连接后，将 session 添加到对应图片的会话集合中，并广播用户加入编辑的消息
     *
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        // 获取参数
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        // 将 session 添加到对应图片的会话集合中
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);

        // 构造响应
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        pictureEditResponseMessage.setMessage(String.format("%s加入编辑", user.getUserName()));
        pictureEditResponseMessage.setUser(userApplicationService.getUserVO(user));

        // 广播给用一张图片的用户
        this.broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 收到前端发送的消息，根据消息类别处理消息
     *
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 将消息解析为 PictureEditMessage
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(),
                PictureEditRequestMessage.class);
        String type = pictureEditRequestMessage.getType();

        // 从 Session 属性中获取公共参数
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");

        // 生产消息
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);
    }


    /**
     * 处理退出编辑状态的逻辑
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     * @throws IOException
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session,
                                      User user, Long pictureId) throws IOException {
        Long editUserId = pictureEditUsers.get(pictureId);
        if (editUserId != null && editUserId.equals(user.getId())) {
            // 将当前用户从正在编辑该图片的用户中移除
            pictureEditUsers.remove(pictureId);
            // 构造响应消息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            pictureEditResponseMessage.setMessage(String.format("%s退出编辑图片", user.getUserName()));
            pictureEditResponseMessage.setUser(userApplicationService.getUserVO(user));
            this.broadcastToPicture(pictureId, pictureEditResponseMessage);
        }

    }

    /**
     * 处理编辑动作的逻辑
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     * @throws IOException
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage,
                                        WebSocketSession session, User user, Long pictureId) throws IOException {
        // 获取参数
        Long editUserId = pictureEditUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum editActionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (editActionEnum == null) {
            return;
        }
        // 只有当前用户正在编辑该图片，才能执行编辑动作
        if (editUserId != null && editUserId.equals(user.getId())) {
            // 构造响应消息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            pictureEditResponseMessage.setEditAction(editAction);
            pictureEditResponseMessage.setMessage(String.format("%s执行%s", user.getUserName(), editActionEnum.getText()));
            pictureEditResponseMessage.setUser(userApplicationService.getUserVO(user));
            // 广播给除了当前用户的其他用户，否则会重复编辑
            this.broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }

    }

    /**
     * 处理进入编辑状态的逻辑
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     * @throws IOException
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session
            , User user, Long pictureId) throws IOException {

        // 没有用户正在编辑该图片，才能进入编辑
        if (!pictureEditUsers.containsKey(pictureId)) {
            // 将当前用户设置为正在编辑该图片
            pictureEditUsers.put(pictureId, user.getId());
            // 构造响应消息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            pictureEditResponseMessage.setMessage(String.format("%s开始编辑图片", user.getUserName()));
            pictureEditResponseMessage.setUser(userApplicationService.getUserVO(user));
            // 广播给用一张图片的用户
            this.broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }


    /**
     * 处理保存编辑的逻辑
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEditSaveMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session,
                                      User user, Long pictureId) throws IOException {
        // 只有当前用户正在编辑该图片，才能保存编辑
        Long editUserId = pictureEditUsers.get(pictureId);
        if (editUserId != null && editUserId.equals(user.getId())) {
            // 构造响应消息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_SAVE.getValue());
            pictureEditResponseMessage.setMessage(String.format("%s保存编辑", user.getUserName()));
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            pictureEditResponseMessage.setPicture(pictureService.getPictureVO(pictureService.getById(pictureId)));
            // 广播给用一张图片的用户
            this.broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 获取参数
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        // 移除当前用户的编辑状态
        this.handleExitEditMessage(null, session, user, pictureId);
        // 从对应图片的会话集合中移除 session
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        sessionSet.remove(session);
        if (sessionSet.isEmpty()) {
            pictureSessions.remove(pictureId);
        }
        // 构造响应消息
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        pictureEditResponseMessage.setMessage(String.format("%s离开编辑", user.getUserName()));
        pictureEditResponseMessage.setUser(userApplicationService.getUserVO(user));
        // 广播给用一张图片的用户
        this.broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 广播消息到指定图片的所有会话
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     * @param excludeSession
     * @throws IOException
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage,
                                    WebSocketSession excludeSession) throws IOException {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (ObjUtil.isNotEmpty(sessionSet)) {
            for (WebSocketSession session : sessionSet) {
                // 创建 ObjectMapper 实例
                ObjectMapper objectMapper = new ObjectMapper();
                SimpleModule module = new SimpleModule();
                module.addSerializer(Long.class, ToStringSerializer.instance);
                module.addSerializer(Long.TYPE, ToStringSerializer.instance);
                objectMapper.registerModule(module);
                String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
                TextMessage textMessage = new TextMessage(message);
                // 排除掉的 session 不发送
                if (session.isOpen() && !session.equals(excludeSession)) {
                    // 发送消息
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    /**
     * 广播消息到所有会话
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     * @throws IOException
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws IOException {
        this.broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }
}
