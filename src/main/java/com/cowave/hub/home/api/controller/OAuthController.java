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

import com.cowave.hub.admin.client.AdminOAuthService;
import com.cowave.hub.admin.client.dto.OAuthAppCardDto;
import com.cowave.hub.admin.client.dto.UserProfileDto;
import com.cowave.zoo.framework.access.AccessProperties;
import com.cowave.zoo.framework.access.security.AccessUserDetails;
import com.cowave.zoo.http.client.constants.HttpCode;
import com.cowave.zoo.http.client.response.HttpResponse;
import com.cowave.zoo.http.client.response.Response;
import com.cowave.hub.home.api.cache.SessionCache;
import com.cowave.hub.home.api.entity.Session;
import com.cowave.hub.home.api.entity.command.OAuthCallbackReq;
import com.cowave.hub.home.api.entity.vo.IconVo;
import com.cowave.hub.home.api.entity.vo.OAuthCallbackVo;
import com.cowave.hub.home.configuration.BlogConfiguration;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * OAuth REST API
 * 处理前端 SPA 的 OAuth 回调、令牌刷新、应用列表
 *
 * @author shanhuiming
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/oauth")
public class OAuthController {

    private final SessionCache sessionCache;
    private final AdminOAuthService oAuthService;
    private final AccessProperties accessProperties;
    private final BlogConfiguration blogConfiguration;

    /**
     * OAuth 回调 —— 用授权码兑换令牌
     * POST /api/v1/oauth/callback/{provider}
     *
     * @param provider OAuth provider (cowave, github, qq, wechat...)
     * @param req      授权码 + PKCE code_verifier
     */
    @PostMapping("/callback/{provider}")
    public Response<OAuthCallbackVo> callback(@PathVariable String provider,
                                               @RequestBody OAuthCallbackReq req,
                                               HttpServletResponse response) {
        // 目前只有 cowave (hub-admin) 一个 provider，后续扩展
        if (!"cowave".equals(provider)) {
            return Response.error("不支持的授权提供商: " + provider);
        }

        // 向 hub-admin 兑换令牌（传入 code + code_verifier）
        AccessUserDetails userDetails = oAuthService.getAuthorizeToken(req.getCode(), req.getCodeVerifier());

        // 保存 session 到 Redis
        String sessionId = sessionCache.save(userDetails.getAccessToken(), userDetails.getRefreshToken());

        // refreshToken 写入 HttpOnly cookie（跨页面存活，防 XSS）
        setHttpOnlyCookie(response, "REFRESH_TOKEN", sessionId, 86400 * 7);

        // 获取头像
        String avatar = null;
        try {
            UserProfileDto profileDto = oAuthService.getUserProfile(userDetails.getAccessToken());
            if (StringUtils.isNotBlank(profileDto.getAvatar())) {
                avatar = profileDto.getAvatar();
            }
        } catch (Exception ignored) {
        }

        // accessToken 返回给前端（存在内存中，不落盘）
        OAuthCallbackVo vo = new OAuthCallbackVo();
        vo.setAccessToken(userDetails.getAccessToken());
        vo.setUsername(userDetails.getUsername());
        vo.setUserNick(userDetails.getUserNick());
        vo.setAvatar(avatar);

        return Response.success(vo);
    }

    /**
     * 刷新访问令牌
     * POST /api/v1/oauth/refresh
     *
     * 前端每个页面加载时调用，用 HttpOnly cookie 中的 refreshToken 换取新的 accessToken
     */
    @PostMapping("/refresh")
    public Response<OAuthCallbackVo> refresh(
            @CookieValue(value = "REFRESH_TOKEN", required = false) String sessionId,
            HttpServletResponse response) {
        if (StringUtils.isBlank(sessionId)) {
            return Response.error("未登录");
        }

        Session session = sessionCache.get(sessionId);
        if (session == null) {
            return Response.error("登录已过期");
        }

        // 刷新令牌
        AccessUserDetails userDetails;
        try {
            userDetails = oAuthService.refreshAuthorizeToken(session.getRefreshToken());
        } catch (Exception e) {
            sessionCache.remove(sessionId);
            return Response.error("令牌刷新失败");
        }

        // 更新 Redis 中的 session
        String newSessionId = sessionCache.save(userDetails.getAccessToken(), userDetails.getRefreshToken());
        sessionCache.remove(sessionId);

        // 更新 cookie
        setHttpOnlyCookie(response, "REFRESH_TOKEN", newSessionId, 86400 * 7);

        OAuthCallbackVo vo = new OAuthCallbackVo();
        vo.setAccessToken(userDetails.getAccessToken());
        vo.setUsername(userDetails.getUsername());
        vo.setUserNick(userDetails.getUserNick());

        return Response.success(vo);
    }

    /**
     * 获取应用列表（含 OAuth 应用 + 静态应用）
     * GET /api/v1/oauth/apps
     *
     * 始终返回应用列表，无需认证：
     * - 默认 OAuth 入口（Hub Admin，使用 hub-home 自身 client_id，始终可见）
     * - 已授权的动态 OAuth 应用（需认证后可见）
     * - 静态链接应用（始终可见）
     */
    @GetMapping("/apps")
    public Response<java.util.List<IconVo>> apps(
            @CookieValue(value = "REFRESH_TOKEN", required = false) String sessionId) {
        java.util.List<IconVo> list = new java.util.ArrayList<>();

        // 1. 默认 OAuth 入口（始终可见，点击触发 OAuth 登录流程）
        //    后续可扩展为从配置读取多个默认 OAuth 应用
        IconVo defaultEntry = new IconVo();
        defaultEntry.setClientId(accessProperties.oauthAppId());
        defaultEntry.setName("系统管理");
        defaultEntry.setIcon("Cloud");
        list.add(defaultEntry);

        // 2. 已授权的动态 OAuth 应用（需要登录）
        if (StringUtils.isNotBlank(sessionId)) {
            Session session = sessionCache.get(sessionId);
            if (session != null) {
                try {
                    HttpResponse<Response<java.util.List<OAuthAppCardDto>>> httpResponse =
                            oAuthService.getAuthorizedApps(session.getAccessToken());

                    if (HttpCode.INVALID_TOKEN.getStatus() == httpResponse.getStatus()) {
                        AccessUserDetails userDetails =
                                oAuthService.refreshAuthorizeToken(session.getRefreshToken());
                        String newSessionId = sessionCache.save(
                                userDetails.getAccessToken(), userDetails.getRefreshToken());
                        sessionCache.remove(sessionId);
                        httpResponse = oAuthService.getAuthorizedApps(userDetails.getAccessToken());
                    }

                    Response<java.util.List<OAuthAppCardDto>> response = httpResponse.getBody();
                    for (OAuthAppCardDto dto : response.getData()) {
                        if (dto.getClientId().equals(accessProperties.oauthAppId())) {
                            continue;
                        }
                        IconVo iconVo = new IconVo();
                        iconVo.setClientId(dto.getClientId());
                        iconVo.setIcon(dto.getCardIcon());
                        iconVo.setName(dto.getCardName());
                        iconVo.setLink(dto.getRedirectUrl());
                        list.add(iconVo);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // 3. 静态链接应用（始终可见）
        if (blogConfiguration.getApplications() != null) {
            list.addAll(blogConfiguration.getApplications());
        }

        return Response.success(list);
    }

    /**
     * 退出登录
     * POST /api/v1/oauth/logout
     */
    @PostMapping("/logout")
    public Response<Void> logout(
            @CookieValue(value = "REFRESH_TOKEN", required = false) String sessionId,
            HttpServletResponse response) {
        if (StringUtils.isNotBlank(sessionId)) {
            sessionCache.remove(sessionId);
        }
        // 清除 cookie
        setHttpOnlyCookie(response, "REFRESH_TOKEN", "", 0);
        return Response.success();
    }

    /**
     * 设置 HttpOnly Cookie
     * 使用 response.setHeader 实现 SameSite 属性（兼容老版本 Servlet API）
     */
    private void setHttpOnlyCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // 本地开发 HTTP，生产通过 nginx 做 HTTPS 时改为 true
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
        // 通过 setHeader 手动追加 SameSite 属性（老版本 Servlet API 不支持 Cookie.setAttribute）
        // SameSite=Lax: 允许从其他站点通过 GET 导航到本站时发送 cookie（OAuth 回调场景需要）
        String header = response.getHeader("Set-Cookie");
        if (header != null && !header.contains("SameSite")) {
            response.setHeader("Set-Cookie", header + "; SameSite=Lax");
        }
    }
}
