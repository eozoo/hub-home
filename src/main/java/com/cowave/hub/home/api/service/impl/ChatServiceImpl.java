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
package com.cowave.hub.home.api.service.impl;

import com.cowave.zoo.http.client.asserts.Asserts;
import com.cowave.zoo.framework.access.Access;
import com.cowave.zoo.framework.helper.redis.RedisHelper;
import com.cowave.hub.home.api.entity.VerifyCode;
import com.cowave.hub.home.api.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author shanhuiming
 */
@RequiredArgsConstructor
@Service
public class ChatServiceImpl implements ChatService {

    private static final String CAPTCHA_KEY = "chat:";

    private final RedisHelper redisHelper;

    private final JavaMailSender mailSender;

    @Override
    public void verifyCode(String mailBox, String uuid) {
        String code = String.valueOf((int)((Math.random() * 9 + 1) * 100000));
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom("cowaveAdmin@163.com");
        mailMessage.setTo(mailBox);
        mailMessage.setSubject("聊天室验证码");
        mailMessage.setText("验证码:" + code + "，有效期为3分钟");
        mailSender.send(mailMessage);
        redisHelper.putExpire(CAPTCHA_KEY + uuid, code, 3, TimeUnit.MINUTES);
    }

    @Override
    public void loginChatRoom(VerifyCode verifyCode) {
        String code = redisHelper.getValue(CAPTCHA_KEY + verifyCode.getUuid());
        Asserts.notNull(code, "验证码已过期");
        Asserts.equals(code, verifyCode.getCode(), "验证码错误");
        Access.setCookie("chat_token", verifyCode.getNickName() + ":" + verifyCode.getUuid());
    }
}
