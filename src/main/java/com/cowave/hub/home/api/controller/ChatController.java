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
package com.cowave.hub.home.api.controller;

import com.cowave.zoo.http.client.response.Response;
import com.cowave.hub.home.api.entity.VerifyCode;
import com.cowave.hub.home.api.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 聊天室
 *
 * @author shanhuiming
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    /**
     * 聊天室验证码
     */
    @PostMapping("/verifyCode")
    public Response<Void> verifyCode(@RequestParam(value = "mailBox") String mailBox, @RequestParam(value = "uuid") String uuid) {
        chatService.verifyCode(mailBox, uuid);
        return Response.success();
    }

    /**
     * 登录聊天界面
     */
    @PostMapping("/loginChatRoom")
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> loginChatRoom(VerifyCode verifyCode) {
        chatService.loginChatRoom(verifyCode);
        return Response.success();
    }
}
