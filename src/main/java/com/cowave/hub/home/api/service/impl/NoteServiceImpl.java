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
import com.cowave.hub.home.api.entity.NoteInfo;
import com.cowave.hub.home.api.mapper.NoteMapper;
import com.cowave.hub.home.api.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;

/**
 * @author shanhuiming
 */
@RequiredArgsConstructor
@Service
public class NoteServiceImpl implements NoteService {

    private final NoteMapper noteMapper;

    @Override
    public Response.Page<NoteInfo> notePage(ModelMap modelMap) {
        QueryWrapper<NoteInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(NoteInfo::getNoteType, "-1")
                .and(e -> e.eq(NoteInfo::getIsPublic, "0")).orderByDesc(NoteInfo::getCreateTime);
        Page<NoteInfo> blogNotePage = noteMapper.selectPage(Access.page(), queryWrapper);
        Response.Page<NoteInfo> notePage = new Response.Page<>();
        notePage.setList(blogNotePage.getRecords());
        notePage.setTotal(blogNotePage.getTotal());
        notePage.setTotalPage(blogNotePage.getPages());
        notePage.setPage(Access.pageIndex());
        notePage.setPageSize(Access.pageSize());
        return notePage;
    }

}
