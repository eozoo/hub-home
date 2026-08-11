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
package com.cowave.hub.home.api.service;

import org.springframework.ui.ModelMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author shanhuiming
 */
public interface BlogService {

    /**
     * 博客首页
     */
    String index(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) throws Exception;

    /**
     * 查看全文
     */
    String article(HttpServletRequest request, HttpServletResponse response,
                   ModelMap modelMap, Long articleId, String articlePwd) throws Exception;

    /**
     * 动态
     */
    String dynamics(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) throws Exception;

    /**
     * 新闻
     */
    String news(ModelMap modelMap);

    /**
     * 留言板
     */
    String comments(ModelMap modelMap);

    /**
     * 聊天室
     */
    String chatRoom(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap);

    /**
     * 归档
     */
    String timeArchives(ModelMap modelMap);

    /**
     * 标签
     */
    String tags(ModelMap modelMap);

    /**
     * 时间轴
     */
    String focus(ModelMap modelMap);

    /**
     * 分类文章
     */
    String categoryPage(ModelMap modelMap, Long categoryId);

    /**
     * 标签文章
     */
    String tagPage(ModelMap modelMap, String tag);

    /**
     * 我的分类
     */
    String ownCategory(ModelMap modelMap);

    /**
     * 我的分类｜修改
     */
    String ownCategoryEdit(Long id, ModelMap modelMap);

    /**
     * 我的博客
     */
    String ownPost(ModelMap modelMap);

    /**
     * 我的博客｜新增
     */
    String ownPostAdd(ModelMap modelMap);

    /**
     * 我的博客｜详情
     */
    String ownPostDetail(Long id, ModelMap modelMap);

    /**
     * 我的博客｜修改
     */
    String ownPostEdit(Long id, ModelMap modelMap);
}
