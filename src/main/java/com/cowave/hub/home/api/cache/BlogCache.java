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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cowave.zoo.http.client.response.Response;
import com.cowave.zoo.framework.access.Access;
import com.cowave.hub.home.api.entity.CategoryInfo;
import com.cowave.hub.home.api.entity.ChannelInfo;
import com.cowave.hub.home.api.entity.NavigationInfo;
import com.cowave.hub.home.api.entity.PostInfo;
import com.cowave.hub.home.api.mapper.CategoryMapper;
import com.cowave.hub.home.api.mapper.ChannelMapper;
import com.cowave.hub.home.api.mapper.NavigationMapper;
import com.cowave.hub.home.api.mapper.PostMapper;
import com.cowave.hub.home.configuration.BlogConfiguration;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

/**
 * @author shanhuiming
 */
@RequiredArgsConstructor
@Service
public class BlogCache {

    private final PostMapper postMapper;

    private final ChannelMapper channelMapper;

    private final CategoryMapper categoryMapper;

    private final NavigationMapper navigationMapper;

    private final BlogConfiguration blogConfiguration;

    /**
     * 导航栏
     */
    public List<NavigationInfo> getNavigations(){
        List<NavigationInfo> navigationList = navigationMapper.queryNavigations(null);
        // 过滤掉停用的(后台与前台用的同一个sql，所以不在sql里where，会使后台导航管理列表不显示)
        String sessionId = Access.getCookie("JSESSIONID");
        // 只是示意，这里随便写下
        if(StringUtils.isBlank(sessionId)){
            return navigationList.stream()
                    .filter(e -> Objects.equals(1, e.getStatus()))
                    .filter(e -> e.getId() < 12)
                    .toList();
        }else{
            return navigationList.stream()
                    .filter(e -> Objects.equals(1, e.getStatus()))
                    .toList();
        }
    }

    /**
     * 文章分类
     */
    @Cacheable(value = "categories", key = "'xx'", cacheManager = "ehcacheCacheManager")
    public List<CategoryInfo> getCategories() {
        return categoryMapper.selectList(new QueryWrapper<CategoryInfo>().lambda().orderByDesc(CategoryInfo::getWeight));
    }

    /**
     * 文章专栏
     */
    @Cacheable(value = "channels", key = "'xx'", cacheManager = "ehcacheCacheManager")
    public List<ChannelInfo> getChannels() {
        return channelMapper.selectList(new QueryWrapper<ChannelInfo>().lambda().orderByDesc(ChannelInfo::getWeight));
    }

    /**
     * 文章标签
     */
    @Cacheable(value = "blogs", key = "'tags'", cacheManager = "ehcacheCacheManager")
    public List<String> getTags() {
        return postMapper.queryTags();
    }

    /**
     * 轮播图
     */
    @Cacheable(value = "blogs", key = "'slideshow'", cacheManager = "ehcacheCacheManager")
    public Response.Page<PostInfo> getSlideshow(){
        PostInfo postInfo = new PostInfo();
        postInfo.setSlider(1);
        return getPostPage(postInfo);
    }

    /**
     * 最新文章
     */
    @Cacheable(value = "blogs", key = "'newPosts'", cacheManager = "ehcacheCacheManager")
    public List<PostInfo> getNewPosts() {
        return postMapper.selectList(new QueryWrapper<PostInfo>().lambda()
                .eq(PostInfo::getStatus, 1).orderByDesc(PostInfo::getCreateTime).last("limit 10"));
    }

    /**
     * 推荐文章
     */
    @Cacheable(value = "blogs", key = "'recommendPosts'", cacheManager = "ehcacheCacheManager")
    public List<PostInfo> getRecommendPosts() {
        return postMapper.selectList(new QueryWrapper<PostInfo>().lambda().eq(PostInfo::getStatus, 1)
                .eq(PostInfo::getFeatured, 1).orderByDesc(PostInfo::getCreateTime).last("limit 10"));
    }

    /**
     * 最热文章
     */
    @Cacheable(value = "blogs", key = "'hotPosts'", cacheManager = "ehcacheCacheManager")
    public List<PostInfo> getHotPosts() {
        return postMapper.selectList(new QueryWrapper<PostInfo>().lambda()
                .eq(PostInfo::getStatus, 1).orderByDesc(PostInfo::getViews).last("limit 10"));
    }

    /**
     * 文章列表
     */
    public Response.Page<PostInfo> getPostPage(PostInfo postInfo) {
        Page<PostInfo> postPage = postMapper.queryPostPage(Access.page(), postInfo);
        Response.Page<PostInfo> pageInfo = new Response.Page<>();
        if (ObjectUtils.isNotEmpty(postPage)) {
            // 搜索关键字
            String keyword = postInfo.getTitle();
            if (StringUtils.isNotEmpty(keyword)) {
                postPage.getRecords().forEach(e -> e.setTitle(getHitCode(e.getTitle(), keyword)));
            }
            pageInfo.setList(postPage.getRecords());
            pageInfo.setTotal(postPage.getTotal());
            pageInfo.setPage(Access.pageIndex());
            pageInfo.setPageSize(Access.pageSize());
            pageInfo.setTotalPage(postPage.getPages());
        }
        return pageInfo;
    }

    /**
     * 标签文章列表
     */
    public Response.Page<PostInfo> getTagPostPage(String tag) {
        Page<PostInfo> postPage = postMapper.queryTagPostPage(Access.page(), tag);
        Response.Page<PostInfo> pageInfo = new Response.Page<>();
        if (ObjectUtils.isNotEmpty(postPage)) {
            pageInfo.setList(postPage.getRecords());
            pageInfo.setTotal(postPage.getTotal());
            pageInfo.setPage(Access.pageIndex());
            pageInfo.setPageSize(Access.pageSize());
            pageInfo.setTotalPage(postPage.getPages());
        }
        return pageInfo;
    }

    /**
     * 关键词高亮
     */
    private String getHitCode(String title, String keyword) {
        if (StringUtils.isEmpty(keyword) || StringUtils.isEmpty(title)) {
            return title;
        }
        String startStr = "<span style = 'color:red'>";
        String endStr = "</span>";
        // 判断关键字是否直接是搜索的内容，否者直接返回
        if (title.equals(keyword)) {
            return startStr + title + endStr;
        }
        String lowerCaseStr = title.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        String[] lowerCaseArray = lowerCaseStr.split(lowerKeyword);
        boolean isEndWith = lowerCaseStr.endsWith(lowerKeyword);

        // 计算分割后的字符串位置
        int count = 0;
        List<Map<String, Integer>> list = new ArrayList<>();
        List<Map<String, Integer>> keyList = new ArrayList<>();
        for (int a = 0; a < lowerCaseArray.length; a++) {
            // 将切割出来的存储map
            Map<String, Integer> map = new HashMap<>();
            Map<String, Integer> keyMap = new HashMap<>();
            map.put("startIndex", count);
            int len = lowerCaseArray[a].length();
            count += len;
            map.put("endIndex", count);
            list.add(map);
            if (a < lowerCaseArray.length - 1 || isEndWith) {
                keyMap.put("startIndex", count);
                count += keyword.length();
                keyMap.put("endIndex", count);
                keyList.add(keyMap);
            }
        }
        // 截取切割对象
        List<String> arrayList = new ArrayList<>();
        for (Map<String, Integer> item : list) {
            Integer start = item.get("startIndex");
            Integer end = item.get("endIndex");
            String itemStr = title.substring(start, end);
            arrayList.add(itemStr);
        }
        // 截取关键字
        List<String> keyArrayList = new ArrayList<>();
        for (Map<String, Integer> item : keyList) {
            Integer start = item.get("startIndex");
            Integer end = item.get("endIndex");
            String itemStr = title.substring(start, end);
            keyArrayList.add(itemStr);
        }
        StringBuilder builder = new StringBuilder();
        for (int a = 0; a < arrayList.size(); a++) {
            builder.append(arrayList.get(a));
            if (a < arrayList.size() - 1 || isEndWith) {
                builder.append(startStr);
                builder.append(keyArrayList.get(a));
                builder.append(endStr);
            }
        }
        return builder.toString();
    }

    public void removeHtmlOfPrefix(String prefix) {
        if (blogConfiguration.isPageStaticEnabled()) {
            File htmlDir = new File(blogConfiguration.getHtmlPath());
            String[] files = htmlDir.list((dir, name) -> name.startsWith(prefix));
            if(files != null){
                for(String file : files){
                    new File(htmlDir + File.separator + file).delete();
                }
            }
        }
    }

    public void removeHtml(Long id) {
        if (blogConfiguration.isPageStaticEnabled()) {
            new File(blogConfiguration.getHtmlPath() + File.separator + id + ".html").delete();
        }
    }
}
