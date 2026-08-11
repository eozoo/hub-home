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

import com.cowave.zoo.http.client.response.Response;
import com.cowave.hub.home.api.entity.CategoryInfo;
import com.cowave.hub.home.api.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文章
 *
 * @author shanhuiming
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/category")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Response<Response.Page<CategoryInfo>> list(CategoryInfo categoryInfo) throws Exception {
        return Response.success(categoryService.list(categoryInfo));
    }

    /**
     * 新增
     */
    @RequestMapping("/add")
    public Response<Void> add(CategoryInfo categoryInfo) throws Exception {
        categoryService.add(categoryInfo);
        return Response.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/edit")
    public Response<Void> edit(CategoryInfo categoryInfo) throws Exception {
        categoryService.edit(categoryInfo);
        return Response.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete/{id}")
    public Response<Void> delete(@PathVariable("id") Long id) {
        categoryService.delete(id);
        return Response.success();
    }
}
