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
package com.cowave.hub.home.api.cache;

import com.cowave.zoo.framework.configuration.ApplicationProperties;
import com.cowave.zoo.framework.helper.redis.RedisHelper;
import com.cowave.hub.home.api.entity.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author shanhuiming
 */
@RequiredArgsConstructor
@Service
public class SessionCache {
    private final SecureRandom random = new SecureRandom();
    private final ApplicationProperties applicationProperties;
    private final RedisHelper redisHelper;

    public String generateSessionId() {
        return new BigInteger(130, random).toString(32).toUpperCase();
    }

    public String save(String accessToken, String refreshToken) {
        String sessionId = generateSessionId();
        Session session = new Session(accessToken, refreshToken);
        redisHelper.putExpire(applicationProperties.getName()
                + ":session:" + sessionId, session, 86400 * 7, TimeUnit.SECONDS);
        return sessionId;
    }

    public Session get(String sessionId) {
        return redisHelper.getValue(applicationProperties.getName() + ":session:" + sessionId);
    }

    public void remove(String sessionId) {
        redisHelper.delete(applicationProperties.getName() + ":session:" + sessionId);
    }
}
