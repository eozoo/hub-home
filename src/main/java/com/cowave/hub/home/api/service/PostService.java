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

import com.cowave.zoo.http.client.response.Response;
import com.cowave.hub.home.api.entity.PostInfo;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author shanhuiming
 */
public interface PostService {

    /**
     * 列表
     */
    Response.Page<PostInfo> list(PostInfo postInfo);

    /**
     * 新增
     */
    void add(PostInfo postInfo);

    /**
     * 修改
     */
    void edit(PostInfo postInfo);

    /**
     * 删除
     */
    void delete(Long id);

    /**
     * 导入markdown
     */
    void importMd(MultipartFile[] files) throws IOException;

    /**
     * 导出markdown
     */
    void exportMd(Long postId, HttpServletResponse response) throws IOException;
}
