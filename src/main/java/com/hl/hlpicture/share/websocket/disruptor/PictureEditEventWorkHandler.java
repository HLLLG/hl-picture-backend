package com.hl.hlpicture.share.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.hl.hlpicture.share.websocket.PictureEditHandler;
import com.hl.hlpicture.share.websocket.model.PictureEditMessageTypeEnum;
import com.hl.hlpicture.share.websocket.model.PictureEditRequestMessage;
import com.hl.hlpicture.share.websocket.model.PictureEditResponseMessage;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.application.service.UserApplicationService;
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
    private UserApplicationService userApplicationService;

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
            case EDIT_SAVE:
                // 处理保存编辑的逻辑
                pictureEditHandler.handleEditSaveMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            default:
                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                pictureEditResponseMessage.setMessage("消息类型错误");
                pictureEditResponseMessage.setUser(userApplicationService.getUserVO(user));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));
        }
    }
}
