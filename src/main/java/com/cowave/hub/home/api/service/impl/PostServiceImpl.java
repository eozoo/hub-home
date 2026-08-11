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

import com.cowave.zoo.http.client.response.Response;
import com.cowave.zoo.framework.access.Access;
import com.cowave.hub.home.api.cache.BlogCache;
import com.cowave.hub.home.api.entity.PostInfo;
import com.cowave.hub.home.api.mapper.PostMapper;
import com.cowave.hub.home.api.service.PostService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * @author shanhuiming
 */
@RequiredArgsConstructor
@Service
public class PostServiceImpl implements PostService {

    private final BlogCache blogCache;

    private final PostMapper postMapper;

    @Override
    public Response.Page<PostInfo> list(PostInfo postInfo) {
        postInfo.setCreateUser(Access.getCookie("blog-userName"));
        return new Response.Page<>(postMapper.list(postInfo), postMapper.count(postInfo));
    }

    @CacheEvict(value = "blogs", allEntries = true, cacheManager = "ehcacheCacheManager")
    @Override
    public void add(PostInfo postInfo) {
        postInfo.setCreateTime(Access.accessTime());
        postInfo.setUpdateTime(Access.accessTime());
        postMapper.insertPost(postInfo);
        postMapper.insertPostContent(postInfo);
        String tags = postInfo.getTags();
        if(StringUtils.isNotBlank(tags)){
            List<String> tagList = Arrays.stream(tags.split(",")).map(String::trim).toList();
            postMapper.insertPostTags(postInfo.getId(), tagList);
        }
        // 删除Html缓存
        blogCache.removeHtmlOfPrefix("blog-");
    }

    @CacheEvict(value = "blogs", allEntries = true, cacheManager = "ehcacheCacheManager")
    @Override
    public void edit(PostInfo postInfo) {
        postInfo.setUpdateTime(Access.accessTime());
        postMapper.updatePost(postInfo);
        postMapper.updatePostContent(postInfo);
        postMapper.deletePostTags(postInfo.getId());
        String tags = postInfo.getTags();
        if(StringUtils.isNotBlank(tags)){
            List<String> tagList = Arrays.stream(tags.split(",")).map(String::trim).toList();
            postMapper.insertPostTags(postInfo.getId(), tagList);
        }
        // 删除Html缓存
        blogCache.removeHtml(postInfo.getId());
        blogCache.removeHtmlOfPrefix("blog-");
    }

    @CacheEvict(value = "blogs", allEntries = true, cacheManager = "ehcacheCacheManager")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long id) {
        postMapper.deletePost(id);
        postMapper.deletePostContent(id);
        postMapper.deletePostTags(id);
        // 删除Html缓存
        blogCache.removeHtml(id);
        blogCache.removeHtmlOfPrefix("blog-");
    }

    @CacheEvict(value = "blogs", allEntries = true, cacheManager = "ehcacheCacheManager")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void importMd(MultipartFile[] files) throws IOException {
        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            assert fileName != null;
            String mdName = fileName.substring(0, fileName.lastIndexOf("."));
            PostInfo postInfo = new PostInfo();
            postInfo.setTitle(mdName);
            postInfo.setFeatured(0);
            postInfo.setSlider(0);
            postInfo.setStatus(1);
            postInfo.setCreateUser(Access.getCookie("blog-userName"));
            postInfo.setCreateTime(Access.accessTime());
            postInfo.setUpdateTime(Access.accessTime());
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))){
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
                postInfo.setContent(builder.toString());
            }
            postMapper.insertPost(postInfo);
            postMapper.insertPostContent(postInfo);
        }
        // 删除Html缓存
        blogCache.removeHtmlOfPrefix("blog-");
    }

    @Override
    public void exportMd(Long postId, HttpServletResponse response) throws IOException {
        PostInfo postInfo = postMapper.queryPostById(postId);
        response.setContentType("text/plain; charset=utf-8");
        response.setHeader("Content-Type", "text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("markdown");
        try(PrintWriter writer = response.getWriter()){
            writer.write(postInfo.getContent());
        }
    }
}
