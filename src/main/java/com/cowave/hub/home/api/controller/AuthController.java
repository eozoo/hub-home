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

import com.cowave.hub.admin.client.dto.OAuthAppCardDto;
import com.cowave.hub.admin.client.dto.UserProfileDto;
import com.cowave.zoo.http.client.constants.HttpCode;
import com.cowave.zoo.http.client.response.HttpResponse;
import com.cowave.zoo.http.client.response.Response;
import com.cowave.zoo.framework.access.Access;
import com.cowave.zoo.framework.access.AccessProperties;
import com.cowave.zoo.framework.access.security.AccessUserDetails;
import com.cowave.hub.admin.client.AdminOAuthService;
import com.cowave.hub.home.api.cache.SessionCache;
import com.cowave.hub.home.api.entity.Session;
import com.cowave.hub.home.api.entity.vo.IconVo;
import com.cowave.hub.home.configuration.BlogConfiguration;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;

/**
 * 认证
 *
 * @author shanhuiming
 */
@RequiredArgsConstructor
@Controller
@RequestMapping("/auth")
public class AuthController {
    private final Random rand = new Random();
    private final SessionCache sessionCache;
    private final AdminOAuthService oAuthService;
    private final BlogConfiguration blogConfiguration;
    private final AccessProperties accessProperties;

    @RequestMapping("/callback")
    public String authCallback(String code) throws Exception {
        AccessUserDetails userDetails = oAuthService.getAuthorizeToken(code);
        String sessionId = sessionCache.save(userDetails.getAccessToken(), userDetails.getRefreshToken());
        // cookie设置
        Access.setCookie("JSESSIONID", sessionId);
        Access.setCookie("blog-userName", userDetails.getUsername());
        Access.setCookie("blog-userNick",
                Base64.getUrlEncoder().encodeToString(userDetails.getUserNick().getBytes(StandardCharsets.UTF_8)));
        // 头像
        UserProfileDto profileDto = oAuthService.getUserProfile(userDetails.getAccessToken());
        if(StringUtils.isBlank(profileDto.getAvatar())){
            avatarGenerate(profileDto.getUserName(), profileDto.getUserAccount());
        }else{
            URL url = new URL(profileDto.getAvatar());
            IOUtils.copy(url, new File("public/avatar/" + profileDto.getUserAccount() + ".jpg"));
        }
        return "redirect:/blog/home";
    }

    private void avatarGenerate(String userName, String userAccount) throws IOException {
        if(StringUtils.isBlank(userName) || StringUtils.isBlank(userAccount)){
            return;
        }
        File avatar = new File("public/avatar/" + userAccount + ".jpg");
        if(avatar.exists()){
            return;
        }
        String text = userName.substring(0, 1);
        int width = 128;
        int height = 128;
        // Create a buffered image with a specific size and type
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        // Enable antialiasing for text
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Draw a white background
        Color backgroundColor = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
        g2d.setColor(backgroundColor);
        g2d.fillRect(0, 0, width, height);
        // Set the font to use for drawing the string
        double bgLuminance = 0.299 * backgroundColor.getRed() + 0.587 * backgroundColor.getGreen() + 0.114 * backgroundColor.getBlue();
        Color textColor;
        if (bgLuminance < 128) {
            textColor = Color.WHITE;
        } else {
            textColor = Color.BLACK;
        }
        Font font = new Font("宋体", Font.BOLD, 64);
        g2d.setFont(font);
        g2d.setColor(textColor);

        // Get font metrics for the string to be drawn
        FontRenderContext context = g2d.getFontRenderContext();
        Rectangle2D bounds = font.getStringBounds(text, context);
        // Calculate the coordinates to center the text
        int x = (width - (int) bounds.getWidth()) / 2;
        int y = (height - (int) bounds.getHeight()) / 2 - (int) bounds.getY();
        // Draw the string
        g2d.drawString(text, x, y);
        // Save the image as JPG
        File avatarDir = new File("public/avatar");
        if (avatarDir.exists() || avatarDir.mkdirs()) {
            ImageIO.write(image, "jpg", avatar);
        }
        // Dispose the graphics object
        g2d.dispose();
    }

    @ResponseBody
    @GetMapping("/icons")
    public java.util.List<IconVo> icons() {
        List<IconVo> list = new ArrayList<>(blogConfiguration.getApplications());
        // 从hub获取授权的应用
        String sessionId = Access.getCookie("JSESSIONID");
        if(StringUtils.isNotBlank(sessionId)){
            Session session = sessionCache.get(sessionId);
            HttpResponse<Response<List<OAuthAppCardDto>>> httpResponse = oAuthService.getAuthorizedApps(session.getAccessToken());
            if(HttpCode.INVALID_TOKEN.getStatus() == httpResponse.getStatus()){
                // 刷新令牌
                AccessUserDetails userDetails = oAuthService.refreshAuthorizeToken(session.getRefreshToken());
                String newSessionId = sessionCache.save(userDetails.getAccessToken(), userDetails.getRefreshToken());
                Access.setCookie("JSESSIONID", newSessionId);
                sessionCache.remove(sessionId);
                // 重新获取
                httpResponse = oAuthService.getAuthorizedApps(userDetails.getAccessToken());
            }

            Response<List<OAuthAppCardDto>> response = httpResponse.getBody();
            for(OAuthAppCardDto dto : response.getData()){
                if(dto.getClientId().equals(accessProperties.oauthAppId())){
                    continue;
                }
                String link = accessProperties.oauthCodeUri()
                        + "?client_id=" + dto.getClientId()
                        + "&response_type=code&redirect_uri=" + dto.getRedirectUrl();
                IconVo iconVo = new IconVo();
                iconVo.setLink(link);
                iconVo.setIcon(dto.getCardIcon());
                iconVo.setName(dto.getCardName());
                list.add(iconVo);
            }
        }
        return list;
    }

    @ResponseBody
    @GetMapping("/authUrl")
    public String authUrl() {
        return accessProperties.oauthCodeUri()
                + "?client_id=" + accessProperties.oauthAppId()
                + "&response_type=code&redirect_uri=" + accessProperties.oauthAppRedirectUri();
    }

    @GetMapping("/quit")
    public String quit(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) throws Exception {
        Access.removeCookie("JSESSIONID", "/");
        Access.removeCookie("blog-userName", "/");
        Access.removeCookie("blog-userNick", "/");
        return "blog/home";
    }
}
