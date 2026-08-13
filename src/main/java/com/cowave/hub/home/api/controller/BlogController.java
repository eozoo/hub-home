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

import com.cowave.zoo.framework.access.AccessProperties;
import com.cowave.hub.home.api.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 首页
 *
 * @author shanhuiming
 */
@RequiredArgsConstructor
@Controller
@RequestMapping("/blog")
public class BlogController {
    private final BlogService blogService;
    private final AccessProperties accessProperties;

    /**
     * 博客首页
     */
    @GetMapping({"", "/", "index", "/list"})
    public String index(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) throws Exception {
        return blogService.index(request, response, modelMap);
    }

    /**
     * 查看全文
     */
    @RequestMapping(value = "/article/{articleId}")
    public String article(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,
                          @PathVariable("articleId") Long articleId,
                          @RequestParam(value = "articlePwd", required = false) String articlePwd) throws Exception {
        return blogService.article(request, response, modelMap, articleId, articlePwd);
    }

    /**
     * 动态
     */
    @GetMapping(value = {"/dynamics"})
    public String dynamics(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) throws Exception {
        return blogService.dynamics(request, response, modelMap);
    }

    /**
     * 热搜
     */
    @GetMapping("/news")
    public String news(ModelMap modelMap) {
        return blogService.news(modelMap);
    }

    /**
     * 留言板
     */
    @GetMapping("/comments")
    public String comments(ModelMap modelMap) {
        return blogService.comments(modelMap);
    }

    /**
     * 聊天室
     */
    @GetMapping("/chatRoom")
    public String chatRoom(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {
        return blogService.chatRoom(request, response, modelMap);
    }

    /**
     * 归档
     */
    @GetMapping("timeArchives")
    public String timeArchives(ModelMap modelMap) {
        return blogService.timeArchives(modelMap);
    }

    /**
     * 标签
     */
    @GetMapping({"/tags"})
    public String tags(ModelMap modelMap) {
        return blogService.tags(modelMap);
    }

    /**
     * 时间轴
     */
    @GetMapping("/focus")
    public String focus(ModelMap modelMap) {
        return blogService.focus(modelMap);
    }

    /**
     * 分类文章
     */
    @GetMapping("category/{categoryId}")
    public String categoryPage(ModelMap modelMap, @PathVariable("categoryId") Long categoryId) {
        return blogService.categoryPage(modelMap, categoryId);
    }

    /**
     * 标签文章
     */
    @GetMapping("tag/{tag}")
    public String tagPage(ModelMap modelMap, @PathVariable("tag") String tag) {
        return blogService.tagPage(modelMap, tag);
    }

    /**
     * 我的分类
     */
    @GetMapping("/own/category")
    public String ownCategory(ModelMap modelMap) {
        return blogService.ownCategory(modelMap);
    }

    /**
     * 我的分类｜新增
     */
    @GetMapping("/own/category/add")
    public String ownCategoryAdd() {
        return "blog/own/category/add";
    }

    /**
     * 我的分类｜修改
     */
    @GetMapping("/own/category/edit/{id}")
    public String ownCategoryEdit(@PathVariable("id") Long id, ModelMap modelMap) {
        return blogService.ownCategoryEdit(id, modelMap);
    }

    /**
     * 我的博客
     */
    @GetMapping("/own/post")
    public String ownPost(ModelMap modelMap) {
        return blogService.ownPost(modelMap);
    }

    /**
     * 我的博客｜新增
     */
    @GetMapping("/own/post/add")
    public String ownPostAdd(ModelMap modelMap) {
        return blogService.ownPostAdd(modelMap);
    }

    /**
     * 我的博客｜详情
     */
    @GetMapping("/own/post/detail/{id}")
    public String ownPostDetail(@PathVariable("id") Long id, ModelMap modelMap) {
        return blogService.ownPostDetail(id, modelMap);
    }

    /**
     * 我的博客｜修改
     */
    @GetMapping("/own/post/edit/{id}")
    public String ownPostEdit(@PathVariable("id") Long id, ModelMap modelMap) {
        return blogService.ownPostEdit(id, modelMap);
    }
}
