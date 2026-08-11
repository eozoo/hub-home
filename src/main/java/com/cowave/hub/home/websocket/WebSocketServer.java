/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.hub.home.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author shanhuiming
 */
@Slf4j
@Component
@ServerEndpoint(value = "/blog/websocket/message/{userId}")
public class WebSocketServer {

    private static final Map<String, WebSocketServer> USER_HOLDER = new ConcurrentHashMap<>();

    private String userId;

    private Session session;

    @OnOpen
    public void onOpen(Session session, EndpointConfig endpointConfig, @PathParam("userId") String userId) throws Exception {
        this.session = session;
        this.userId = userId;
        USER_HOLDER.put(userId, this);
        // 设置消息大小最大值10M
        session.setMaxBinaryMessageBufferSize(20 * 1024 * 1024);
        session.setMaxTextMessageBufferSize(20 * 1024 * 1024);
        session.setMaxIdleTimeout(60 * 1000 * 10);
        // 推送在线人数
        sendMsgAll("webOnlineUserNum:" + USER_HOLDER.size(), session, userId, true);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        String fromUserId = userId;
        try {
            sendMsg(message, session, fromUserId);
        } catch (Exception e) {
            USER_HOLDER.get(fromUserId).session.getAsyncRemote().sendText("系统消息：消息格式错误，拒绝发送 =》" + message);
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session) {
        USER_HOLDER.remove(userId);
        sendMsgAll("webOnlineUserNum:" + USER_HOLDER.size(), session, userId, true);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("chat error", throwable);
    }

    private void sendMsg(String message, Session session, String fromUserId) throws Exception {
        if (Objects.equals("ping", message)) { // 心跳
            USER_HOLDER.get(fromUserId).session.getAsyncRemote().sendText("pong");
            return;
        }

        if (USER_HOLDER.size() == 1 && USER_HOLDER.containsKey(userId)) {
            USER_HOLDER.get(userId).session.getBasicRemote().sendText("{\"fromUserID\":\"\",\"toUserId\":\"\",\"msg\":\"" + "当前会话仅您一人" + "\",\"msgType\":\"3\",\"my\":\"false\",\"prompt\":true}");
        }

        JSONObject msgJson = JSON.parseObject(message);
        String toUserId = msgJson.getString("toUserId");
        String msgType = msgJson.getString("msgType");
        String msg = msgJson.getString("msg");
        // 从cookie中获取 nickName
        String token = new String(Base64.decodeBase64(userId));
        String[] tokenArr = token.split(":");
        msgJson.put("nickName", tokenArr[0]);

        if (Objects.equals("3", msgType)) { // 发送给所有在线的人
            sendMsgAll(msgJson.toJSONString(), session, fromUserId, false);
            return;
        }
        if (StringUtils.isNotEmpty(toUserId) && USER_HOLDER.containsKey(toUserId)) {
            USER_HOLDER.get(toUserId).session.getAsyncRemote().sendText(msg);
        }
    }

    private static void sendMsgAll(String message, Session session, String fromUserId, boolean oneself) {
        USER_HOLDER.entrySet().stream().forEach((entry) -> {
            if (oneself) {
                USER_HOLDER.get(entry.getKey()).session.getAsyncRemote().sendText(message);
            } else {
                if (!Objects.equals(fromUserId, entry.getKey())){
                    USER_HOLDER.get(entry.getKey()).session.getAsyncRemote().sendText(message);
                }
            }
        });
    }

    public static void sendToUserList(List<String> persons, String message) {
        persons.forEach(userId -> {
            if (StringUtils.isNotBlank(userId) && USER_HOLDER.containsKey(userId)) {
                USER_HOLDER.get(userId).session.getAsyncRemote().sendText(message);
            }
        });
    }
}
