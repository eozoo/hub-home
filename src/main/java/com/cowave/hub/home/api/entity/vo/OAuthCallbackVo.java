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
package com.cowave.hub.home.api.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OAuth 回调响应
 *
 * @author shanhuiming
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OAuthCallbackVo {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String userNick;

    /**
     * 头像地址
     */
    private String avatar;
}
