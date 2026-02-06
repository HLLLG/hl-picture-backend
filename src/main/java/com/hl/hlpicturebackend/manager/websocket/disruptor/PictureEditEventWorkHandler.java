package com.hl.hlpicturebackend.manager.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.hl.hlpicturebackend.manager.websocket.PictureEditHandler;
import com.hl.hlpicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.hl.hlpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.hl.hlpicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.hl.hlpicturebackend.model.entity.User;
import com.hl.hlpicturebackend.service.UserService;
import com.lmax.disruptor.WorkHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * disruptor 消费者
 */
@Slf4j
@Component
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    private PictureEditHandler pictureEditHandler;

    @Resource
    private UserService userService;

    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        // 解析消息
        PictureEditRequestMessage pictureEditRequestMessage = pictureEditEvent.getPictureEditRequestMessage();
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);

        // 从pictureEditEvent中获取公共参数
        User user = pictureEditEvent.getUser();
        Long pictureId = pictureEditEvent.getPictureId();
        WebSocketSession session = pictureEditEvent.getSession();

        // 调用对应的消息处理方法
        switch (pictureEditMessageTypeEnum) {
            case ENTER_EDIT:
                // 处理进入编辑状态的逻辑
                pictureEditHandler.handleEnterEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EDIT_ACTION:
                // 处理编辑动作的逻辑
                pictureEditHandler.handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EXIT_EDIT:
                // 处理退出编辑状态的逻辑
                pictureEditHandler.handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            default:
                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                pictureEditResponseMessage.setMessage("消息类型错误");
                pictureEditResponseMessage.setUser(userService.getUserVO(user));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));
        }
    }
}
