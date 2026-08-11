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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cowave.zoo.http.client.response.Response;
import com.cowave.zoo.framework.access.Access;
import com.cowave.hub.home.api.entity.CategoryInfo;
import com.cowave.hub.home.api.mapper.CategoryMapper;
import com.cowave.hub.home.api.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author shanhuiming
 */
@RequiredArgsConstructor
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public Response.Page<CategoryInfo> list(CategoryInfo categoryInfo) {
        QueryWrapper<CategoryInfo> queryWrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(categoryInfo.getName())){
            queryWrapper.lambda().like(CategoryInfo::getName, categoryInfo.getName());
        }
        Page<CategoryInfo> page = categoryMapper.selectPage(Access.page(), queryWrapper);
        Response.Page<CategoryInfo> categoryPage = new Response.Page<>();
        categoryPage.setList(page.getRecords());
        categoryPage.setTotal(page.getTotal());
        categoryPage.setPage(Access.pageIndex());
        categoryPage.setPageSize(Access.pageSize());
        categoryPage.setTotalPage(page.getPages());
        return categoryPage;
    }

    @Override
    public void add(CategoryInfo categoryInfo) {
        categoryMapper.insert(categoryInfo);
    }

    @Override
    public void edit(CategoryInfo categoryInfo) {
        categoryMapper.updateById(categoryInfo);
    }

    @Override
    public void delete(Long id) {
        categoryMapper.deleteById(id);
    }
}
