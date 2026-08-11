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
package com.cowave.hub.home.configuration;

import com.cowave.hub.home.api.entity.vo.IconVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.util.List;

/**
 * @author shanhuiming
 */
@ConfigurationProperties(prefix = "blog")
@Configuration
@Data
public class BlogConfiguration {

    /** 静态资源路径 */
    private String profile;

    /** IP地址获取 */
    private boolean addressEnabled;

    /** 开启页面静态化 */
    private boolean pageStaticEnabled;

    /** 默认应用列表 */
    private List<IconVo> applications;

    /**
     * 静态页面路径
     */
    public String getHtmlPath() {
        return profile + "/html";
    }

    /**
     * 文章图片路径
     */
    public String getPostImagePath() {
        return profile + "/images/post";
    }

    /**
     * Md图片路径
     */
    public String getMdImagePath() {
        return profile + "/images/markdown";
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    @Bean
    public EhCacheCacheManager ehcacheCacheManager() {
        return new EhCacheCacheManager(EhCacheManagerUtils.buildCacheManager("classpath:ehcache.xml"));
    }
}
