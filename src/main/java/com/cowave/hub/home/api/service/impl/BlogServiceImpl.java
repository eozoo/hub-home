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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cowave.zoo.http.client.asserts.Asserts;
import com.cowave.zoo.http.client.response.Response;
import com.cowave.zoo.framework.access.Access;
import com.cowave.zoo.tools.DateUtils;
import com.cowave.hub.home.api.cache.BlogCache;
import com.cowave.hub.home.api.entity.AboutInfo;
import com.cowave.hub.home.api.entity.PostInfo;
import com.cowave.hub.home.api.mapper.CategoryMapper;
import com.cowave.hub.home.api.mapper.PostMapper;
import com.cowave.hub.home.api.service.BlogService;
import com.cowave.hub.home.api.service.NoteService;
import com.cowave.hub.home.configuration.BlogConfiguration;
import com.cowave.hub.home.configuration.ViewConfiguration;
import com.cowave.hub.home.utils.MarkdownUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * @author shanhuiming
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class BlogServiceImpl implements BlogService {
    private final SecureRandom random = new SecureRandom();
    private final BlogConfiguration blogConfiguration;
    private final TemplateEngine templateEngine;
    private final Executor applicationExecutor;
    private final PostMapper postMapper;
    private final CategoryMapper categoryMapper;
    private final BlogCache blogCache;
    private final NoteService noteService;

    @Override
    public String index(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) throws Exception {
        if (blogConfiguration.isPageStaticEnabled()) {
            String html = "blog-" + Access.pageIndex() + "-" + Access.pageSize() + ".html";
            File file = new File(blogConfiguration.getHtmlPath() + File.separator + html);
            if (file.exists()) {
                String htmlPath = blogConfiguration.getHtmlPath()
                        .substring(blogConfiguration.getHtmlPath().lastIndexOf("/") + 1);
                return "forward:" + ViewConfiguration.RESOURCE_PREFIX + "/" + htmlPath + "/" + html;
            }
            loadIndexData(modelMap);
            createHtml(request, response, true, modelMap, "blog/index", html);
            return "blog/index";
        }
        loadIndexData(modelMap);
        return "blog/index";
    }

    /**
     * 加载首页数据
     */
    private void loadIndexData(ModelMap modelMap) {
        // 首页加载轮播图
        if (1 == Access.pageIndex()) {
            loadSlideshow(modelMap);
        }
        // 文章列表
        CompletableFuture<Void> postFuture = loadPost(modelMap, new PostInfo());
        // 导航栏
        loadNavigation(modelMap);
        // 侧边栏
        CompletableFuture<Void> sideFuture = loadSidebar(modelMap);
        CompletableFuture.allOf(postFuture, sideFuture).join();
    }

    /**
     * 文章列表
     */
    private CompletableFuture<Void> loadPost(ModelMap modelMap, PostInfo postInfo) {
        return CompletableFuture.runAsync(
                () -> modelMap.put("postPage", blogCache.getPostPage(postInfo)), applicationExecutor)
                .exceptionally(e -> {log.error("", e); return null;});
    }

    /**
     * 标签文章列表
     */
    private CompletableFuture<Void> loadTagPost(ModelMap modelMap, String tag) {
        return CompletableFuture.runAsync(
                        () -> modelMap.put("postPage", blogCache.getTagPostPage(tag)), applicationExecutor)
                .exceptionally(e -> {log.error("", e); return null;});
    }

    /**
     * 导航栏
     */
    private void loadNavigation(ModelMap modelMap) {
        modelMap.put("navigations", blogCache.getNavigations());
    }

    /**
     * 侧边栏
     */
    private CompletableFuture<Void> loadSidebar(ModelMap modelMap){
        AboutInfo aboutInfo = new AboutInfo();
        aboutInfo.setAbAvatar("/img/profile.jpg");
        aboutInfo.setAbName("Cowave Blog");
        aboutInfo.setAbText("保持追求，积极开放；热爱学习，乐于分享！");
        modelMap.put("aboutInfo", aboutInfo);
        return CompletableFuture.runAsync(() -> {
                    modelMap.put("categories", blogCache.getCategories()); // 文章分类
                    modelMap.put("channels", blogCache.getChannels());     // 文章专栏
                    modelMap.put("tags", blogCache.getTags());             // 文章标签
                    modelMap.put("newPosts", blogCache.getNewPosts());             // 最新文章
                    modelMap.put("recommendPosts", blogCache.getRecommendPosts()); // 推荐文章
                    modelMap.put("hotPosts", blogCache.getHotPosts());             // 浏览排行
                }, applicationExecutor)
                .exceptionally(e -> {log.error("", e); return null;});
    }

    /**
     * 轮播图
     */
    private CompletableFuture<Void> loadSlideshow(ModelMap modelMap) {
        return CompletableFuture.runAsync(
                () -> modelMap.put("slideshow", blogCache.getSlideshow()), applicationExecutor)
                .exceptionally(e -> {log.error("", e); return null;});
    }

    /**
     * 创建静态页面
     */
    private void createHtml(HttpServletRequest request, HttpServletResponse response,
                            boolean force, Map<String, Object> paramMap, String templateUrl, String html) throws Exception {
        // thymeleaf上下文
        WebContext context = new WebContext(request, response, request.getServletContext());
        // 放入数据
        context.setVariables(paramMap);
        // 文件生成路径
        String fileUrl = StringUtils.appendIfMissing(blogConfiguration.getHtmlPath(), File.separator) + html;
        File directory = new File(StringUtils.substringBeforeLast(fileUrl, File.separator));
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File file = new File(fileUrl);
        if (!force && file.exists()) {
            return;
        }
        try(PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            templateEngine.process(templateUrl, context, printWriter);
        }
    }

    @Override
    public String article(HttpServletRequest request, HttpServletResponse response,
                          ModelMap modelMap, Long articleId, String articlePwd) throws Exception {
        PostInfo postInfo = getPostById(articleId);
        // 存在密码
        String blogPwd = postInfo.getPwd();
        if (StringUtils.isNotBlank(blogPwd) && !Objects.equals(blogPwd, articlePwd)) {
            postInfo.setContent(null);
            if (StringUtils.isNotBlank(articlePwd)) {
                postInfo.setTitle(postInfo.getTitle() + " (密码错误)");
            }
            return "blog/auth";
        }

        if (blogConfiguration.isPageStaticEnabled()) {
            File file = new File(blogConfiguration.getHtmlPath() + File.separator + articleId + ".html");
            if (file.exists()) {
                String htmlPath = blogConfiguration.getHtmlPath().substring(blogConfiguration.getHtmlPath().lastIndexOf("/") + 1);
                return "forward:" + ViewConfiguration.RESOURCE_PREFIX + "/" + htmlPath + "/" + articleId + ".html";
            }
        }
        // 导航栏
        loadNavigation(modelMap);
        // 专栏，侧边栏
        if (Objects.equals("2", postInfo.getChannelType())) {
            List<PostInfo> specialPostList = postMapper.queryChannelPosts(postInfo.getChannelId());
            postInfo.setSpecialPostList(specialPostList);
        }
        modelMap.put("post", postInfo);
        if (blogConfiguration.isPageStaticEnabled()) {
            createArticleHtml(request, response, modelMap, articleId);
        }
        return "blog/article";
    }

    /**
     * 博客查看
     */
    private PostInfo getPostById(Long id) {
        PostInfo postInfo = postMapper.queryPostById(id);
        Asserts.notNull(postInfo, "文章不存在");
        // markdown渲染
        String content = postInfo.getContent();
        if (StringUtils.isNotEmpty(content)) {
            content = content.replace("\\n", "\n").replace("\\r", "\r");
            content = MarkdownUtils.renderMarkdown(content);
            content = content.replaceAll("<img src=", "<img src=\"/blog/img/loading.jpg\" onerror=\"javascript:this.src='/blog/img/404_img.jpg'\"  data-echo=");
            postInfo.setContent(content);
        } else {
            postInfo.setContent("<h1><b>&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</b></h1><h1 style=\"text-align: left;\"><b>暂无文章内容</b><span style=\"font-weight: 700;\">&nbsp;&nbsp;</span><img src=\"/ajax/libs/summernote/tam-emoji/img/blank.gif\" class=\"img\" style=\"display:inline-block;width:25px;height:25px;background:url('/ajax/libs/summernote/tam-emoji/img/emoji_spritesheet_3.png') -75px -25px no-repeat;background-size:850px 75px;\" alt=\":rocket:\"></h1><h1><span style=\"font-weight: 700;\"> &nbsp; &nbsp; &nbsp; &nbsp;&nbsp;</span></h1><h1><b><br></b></h1><h1><b><br></b></h1><h1><b><br></b></h1>");
        }
        postMapper.viewIncrease(postInfo.getId());
        return postInfo;
    }

    /**
     * 文章静态页面
     */
    private void createArticleHtml(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap, Long articleId) throws Exception {
        Map<String, Object> paramMap = new LinkedHashMap<>();
        paramMap.put("navigations", modelMap.get("navigations"));
        paramMap.put("post", modelMap.get("post"));
        createHtml(request, response, true, paramMap, "blog/article", articleId + ".html");
    }

    @Override
    public String dynamics(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) throws Exception {
        if (blogConfiguration.isPageStaticEnabled()) {
            String html = "dynamic-" + Access.pageIndex() + "-" + Access.pageSize() + ".html";
            File file = new File(blogConfiguration.getHtmlPath() + File.separator + html);
            if (file.exists()) {
                String htmlPath = blogConfiguration.getHtmlPath().substring(blogConfiguration.getHtmlPath().lastIndexOf("/") + 1);
                return "forward:" + ViewConfiguration.RESOURCE_PREFIX + "/" + htmlPath + "/" + html;
            }
            loadDynamicData(modelMap);
            createHtml(request, response, true, modelMap, "blog/dynamic", html);
            return "blog/dynamic";
        }
        loadDynamicData(modelMap);
        return "blog/dynamic";
    }

    private void loadDynamicData(ModelMap modelMap) {
        // 获取导航
        loadNavigation(modelMap);
        // 获取侧边栏
        CompletableFuture<Void> sideFuture = loadSidebar(modelMap);
        // 获取动态列表
        CompletableFuture.allOf(sideFuture).join();
        modelMap.put("notePage", noteService.notePage(modelMap));
    }

    @Override
    public String news(ModelMap modelMap) {
        // 获取导航
        loadNavigation(modelMap);
        // 获取侧边栏
        CompletableFuture<Void> sideFuture = loadSidebar(modelMap);
        CompletableFuture.allOf(sideFuture).join();
        return "blog/news";
    }

    @Override
    public String comments(ModelMap modelMap) {
        // 获取导航
        loadNavigation(modelMap);
        // 获取侧边栏
        CompletableFuture<Void> sideFuture = loadSidebar(modelMap);
        CompletableFuture.allOf(sideFuture).join();
        return "blog/comment";
    }

    @Override
    public String chatRoom(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {
        String chatToken = Access.getCookie("chat_token");
        if (StringUtils.isNotBlank(chatToken)) {
            return "blog/chatRoom";
        }
//        if (ShiroUtils.isLogin()) {
//            String chatToke = ShiroUtils.getSysUser().getUserName() + "," + SnowFlake.getNextId();
//            CookieUtils.setCookie(response, webChatTokenName, Base64.encodeBase64String(chatToke.getBytes()));
//            return prefix + "/chatRoom";
//        }
        modelMap.put("uuid", random.nextInt());
        return "blog/chatRoomLogin";
    }

    @Override
    public String timeArchives(ModelMap modelMap) {
        // 获取导航
        loadNavigation(modelMap);
        // 获取侧边栏
        CompletableFuture<Void> sideFuture = loadSidebar(modelMap);
        CompletableFuture.allOf(sideFuture).join();
        // 获取文章
        Page<PostInfo> blogPostPage = postMapper.timeArchives(Access.page(15));
        Map<String, Object> postPage = new HashMap<>();
        Map<String, List<PostInfo>> collect = blogPostPage.getRecords().stream().collect(
                        Collectors.groupingBy(post -> DateUtils.format(post.getCreateTime(),"yyyy-MM")));
        postPage.put("list", collect);
        postPage.put("total", blogPostPage.getTotal());
        postPage.put("totalPage", blogPostPage.getPages());
        postPage.put("page", Access.pageIndex());
        postPage.put("pageSize", Access.pageSize(15));
        modelMap.put("postPage", postPage);
        return "blog/timeArchives";
    }

    @Override
    public String tags(ModelMap modelMap) {
        // 获取导航
        loadNavigation(modelMap);
        // 获取侧边栏
        CompletableFuture<Void> sideFuture = loadSidebar(modelMap);
        CompletableFuture.allOf(sideFuture).join();
        return "blog/tags";
    }

    @Override
    public String focus(ModelMap modelMap) {
        // 获取导航
        loadNavigation(modelMap);
        // 获取侧边栏
        CompletableFuture<Void> sideFuture = loadSidebar(modelMap);
        // 文章列表
        CompletableFuture<Void> postFuture = CompletableFuture.runAsync(() -> {
            Page<PostInfo> blogPostPage = postMapper.timeArchiving(Access.page(15));
            Response.Page<PostInfo> postPage = new Response.Page<>();
            postPage.setList(blogPostPage.getRecords());
            postPage.setTotal(blogPostPage.getTotal());
            postPage.setTotalPage(blogPostPage.getPages());
            postPage.setPage(Access.pageIndex());
            postPage.setPageSize(Access.pageSize(15));
            modelMap.put("postPage", postPage);
        }, applicationExecutor).exceptionally(e -> {log.error("", e); return null;});
        CompletableFuture.allOf(sideFuture, postFuture).join();
        return "blog/focus";
    }

    @Override
    public String categoryPage(ModelMap modelMap, Long categoryId) {
        PostInfo postInfo = new PostInfo();
        postInfo.setCategoryId(categoryId);
        // 文章列表
        CompletableFuture<Void> postFuture = loadPost(modelMap, postInfo);
        // 导航栏
        loadNavigation(modelMap);
        // 侧边栏
        CompletableFuture<Void> sideFuture = loadSidebar(modelMap);
        CompletableFuture.allOf(postFuture, sideFuture).join();
        return "blog/index";
    }

    @Override
    public String tagPage(ModelMap modelMap, String tag) {
        // 文章列表
        CompletableFuture<Void> postFuture = loadTagPost(modelMap, tag);
        // 导航栏
        loadNavigation(modelMap);
        // 侧边栏
        CompletableFuture<Void> sideFuture = loadSidebar(modelMap);
        CompletableFuture.allOf(postFuture, sideFuture).join();
        return "blog/index";
    }

    @Override
    public String ownCategory(ModelMap modelMap) {
        modelMap.put("navigations", blogCache.getNavigations());
        return "blog/own/category";
    }

    @Override
    public String ownCategoryEdit(Long id, ModelMap modelMap) {
        modelMap.put("category", categoryMapper.selectById(id));
        return "blog/own/category/edit";
    }

    @Override
    public String ownPost(ModelMap modelMap) {
        modelMap.put("navigations", blogCache.getNavigations());
        modelMap.put("categories", blogCache.getCategories());
        return "blog/own/post";
    }

    @Override
    public String ownPostAdd(ModelMap modelMap) {
        modelMap.put("navigations", blogCache.getNavigations());
        modelMap.put("categories", blogCache.getCategories());
        modelMap.put("channels", blogCache.getChannels());
        return "blog/own/post/add";
    }

    @Override
    public String ownPostDetail(Long id, ModelMap modelMap) {
        PostInfo postInfo = postMapper.queryPostById(id);
        String content = postInfo.getContent();
        if (StringUtils.isNotEmpty(content)) {
            content = content.replace("\\n", "\n").replace("\\r", "\r");
            content = content.replaceAll("<img src=", "<img src=\"/blog/img/loading.jpg\" onerror=\"javascript:this.src='/blog/img/404_img.jpg'\"  data-echo=");
            postInfo.setContent(content);
        } else {
            postInfo.setContent("<h1><b>&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</b></h1><h1 style=\"text-align: left;\"><b>暂无文章内容</b><span style=\"font-weight: 700;\">&nbsp;&nbsp;</span><img src=\"/ajax/libs/summernote/tam-emoji/img/blank.gif\" class=\"img\" style=\"display:inline-block;width:25px;height:25px;background:url('/ajax/libs/summernote/tam-emoji/img/emoji_spritesheet_3.png') -75px -25px no-repeat;background-size:850px 75px;\" alt=\":rocket:\"></h1><h1><span style=\"font-weight: 700;\"> &nbsp; &nbsp; &nbsp; &nbsp;&nbsp;</span></h1><h1><b><br></b></h1><h1><b><br></b></h1><h1><b><br></b></h1>");
        }
        modelMap.put("post", postInfo);
        return "blog/own/post/detail";
    }

    @Override
    public String ownPostEdit(Long id, ModelMap modelMap) {
        PostInfo postInfo = postMapper.queryPostById(id);
        String content = postInfo.getContent();
        if (StringUtils.isNotEmpty(content)) {
            content = content.replace("\\n", "\n").replace("\\r", "\r");
            content = content.replaceAll("<img src=", "<img src=\"/blog/img/loading.jpg\" onerror=\"javascript:this.src='/blog/img/404_img.jpg'\"  data-echo=");
            postInfo.setContent(content);
        } else {
            postInfo.setContent("<h1><b>&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</b></h1><h1 style=\"text-align: left;\"><b>暂无文章内容</b><span style=\"font-weight: 700;\">&nbsp;&nbsp;</span><img src=\"/ajax/libs/summernote/tam-emoji/img/blank.gif\" class=\"img\" style=\"display:inline-block;width:25px;height:25px;background:url('/ajax/libs/summernote/tam-emoji/img/emoji_spritesheet_3.png') -75px -25px no-repeat;background-size:850px 75px;\" alt=\":rocket:\"></h1><h1><span style=\"font-weight: 700;\"> &nbsp; &nbsp; &nbsp; &nbsp;&nbsp;</span></h1><h1><b><br></b></h1><h1><b><br></b></h1><h1><b><br></b></h1>");
        }
        modelMap.put("post", postInfo);
        modelMap.put("navigations", blogCache.getNavigations());
        modelMap.put("categories", blogCache.getCategories());
        modelMap.put("channels", blogCache.getChannels());
        return "blog/own/post/edit";
    }
}
