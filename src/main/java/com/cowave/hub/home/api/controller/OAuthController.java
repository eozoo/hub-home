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

import com.cowave.hub.admin.client.AdminOAuthClient;
import com.cowave.hub.admin.client.dto.OAuthAppCardDto;
import com.cowave.hub.admin.client.dto.OAuthEntryDto;
import com.cowave.hub.admin.client.dto.MemberProfileDto;
import com.cowave.hub.admin.client.dto.UserProfileDto;
import com.cowave.hub.admin.client.request.OAuth2TokenRequest;
import com.cowave.zoo.framework.access.AccessProperties;
import com.cowave.zoo.framework.access.security.AccessUserDetails;
import com.cowave.zoo.http.client.response.Response;
import com.cowave.hub.home.api.cache.SessionCache;
import com.cowave.hub.home.api.entity.Session;
import com.cowave.hub.home.api.entity.command.OAuthCallbackReq;
import com.cowave.hub.home.api.entity.vo.OAuthCallbackVo;
import com.cowave.hub.home.configuration.BlogConfiguration;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Random;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * OAuth授权管理
 *
 * @author shanhuiming
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/oauth")
public class OAuthController {

    private final SessionCache sessionCache;

    private final AdminOAuthClient oauthClient;

    private final AccessProperties accessProperties;

    private final BlogConfiguration blogConfiguration;

    private final Random rand = new Random();

    @Value("${spring.access.oauth.memberTenantId:cowave}")
    private String memberTenantId;

    /**
     * 授权服务列表
     */
    @GetMapping("/list")
    public Response<List<OAuthEntryDto>> list() {
        return Response.success(oauthClient.getOauthList(memberTenantId));
    }

    /**
     * OAuth授权回调
     *
     * @param provider OAuth授权提供放
     * @param req      授权码 + code_verifier
     */
    @PostMapping("/callback/{provider}")
    public Response<OAuthCallbackVo> callback(
            @PathVariable String provider, @RequestBody OAuthCallbackReq req, HttpServletResponse response) {
        AccessUserDetails userDetails;
        String avatar;
        if ("cowave".equals(provider)) {
            OAuth2TokenRequest tokenRequest = new OAuth2TokenRequest();
            tokenRequest.setCode(req.getCode());
            tokenRequest.setClientId(accessProperties.oauthAppId());
            tokenRequest.setClientSecret(accessProperties.oauthAppSecret());
            tokenRequest.setRedirectUri(accessProperties.oauthAppRedirectUri());
            tokenRequest.setCodeVerifier(req.getCodeVerifier());
            // 系统用户（PKCE）
            userDetails = oauthClient.getAuthorizeToken(tokenRequest);
            UserProfileDto profileDto = oauthClient.getUserProfile(userDetails.getAccessToken());
            if (StringUtils.isNotBlank(profileDto.getAvatar())) {
                avatar = profileDto.getAvatar();
            } else {
                avatar = generateAvatar(profileDto.getUserName(), profileDto.getUserAccount());
            }
        } else if ("gitlab".equals(provider)) {
            // 会员 gitlab 登录（落 hub_member）
            userDetails = oauthClient.gitlabAuthorizeToken(memberTenantId, req.getCode());
            MemberProfileDto profileDto = oauthClient.getMemberProfile(userDetails.getAccessToken());
            avatar = profileDto.getMemberAvatar();
        } else {
            return Response.error("不支持的授权提供商: " + provider);
        }

        // 保存 session 到 Redis
        String sessionId = sessionCache.save(userDetails.getAccessToken(), userDetails.getRefreshToken());

        // refreshToken 写入 HttpOnly cookie（跨页面存活，防 XSS）
        setHttpOnlyCookie(response, "REFRESH_TOKEN", sessionId, 86400 * 7);

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
     * <p>
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
            userDetails = oauthClient.refreshAuthorizeToken(session.getRefreshToken());
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
     * 获取应用列表（匿名返回 public，登录返回完整可见应用）
     * GET /api/v1/oauth/apps
     */
    @GetMapping("/apps")
    public Response<List<OAuthAppCardDto>> apps(
            @CookieValue(value = "REFRESH_TOKEN", required = false) String sessionId) {
        // 登录态：取 session 里的 accessToken
        String accessToken = null;
        if (StringUtils.isNotBlank(sessionId)) {
            Session session = sessionCache.get(sessionId);
            if (session != null) {
                accessToken = session.getAccessToken();
            }
        }

        List<OAuthAppCardDto> appList;
        if (accessToken != null) {
            appList = oauthClient.getAuthorizedAppNav(accessToken);
        } else {
            appList = oauthClient.getAppNav(memberTenantId);
        }
        return Response.success(appList);
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

    private String generateAvatar(String name, String account) {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(account)) {
            return null;
        }
        String path = blogConfiguration.getProfile() + "/avatar/" + account + ".jpg";
        File avatarFile = new File(path);
        if (avatarFile.exists()) {
            return "/profile/avatar/" + account + ".jpg";
        }
        try {
            String text = name.substring(0, 1);
            BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color backgroundColor = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
            g2d.setColor(backgroundColor);
            g2d.fillRect(0, 0, 128, 128);
            double bgLuminance = 0.299 * backgroundColor.getRed() + 0.587 * backgroundColor.getGreen()
                    + 0.114 * backgroundColor.getBlue();
            g2d.setColor(bgLuminance < 128 ? Color.WHITE : Color.BLACK);
            g2d.setFont(new Font("宋体", Font.BOLD, 64));
            FontRenderContext context = g2d.getFontRenderContext();
            Rectangle2D bounds = g2d.getFont().getStringBounds(text, context);
            int x = (128 - (int) bounds.getWidth()) / 2;
            int y = (128 - (int) bounds.getHeight()) / 2 - (int) bounds.getY();
            g2d.drawString(text, x, y);
            File avatarDir = avatarFile.getParentFile();
            if (avatarDir.exists() || avatarDir.mkdirs()) {
                ImageIO.write(image, "jpg", avatarFile);
            }
            g2d.dispose();
        } catch (Exception e) {
            return null;
        }
        return "/profile/avatar/" + account + ".jpg";
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
