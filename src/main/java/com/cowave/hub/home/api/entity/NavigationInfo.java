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
package com.cowave.hub.home.api.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 导航栏
 *
 * @author shanhuiming
 */
@Data
@TableName("info_navigation")
public class NavigationInfo {

    /**
     * id
     */
    private Long id;

    /**
     * 父级id
     */
    private Long pid;

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 图标
     */
    private Integer type;

    /**
     * 页面地址
     */
    private String pageUrl;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 图标
     */
    private String icon;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 父级栏目
     */
    @TableField(exist = false)
    private NavigationInfo parent;

    /**
     * 子栏目
     */
    @TableField(exist = false)
    private List<NavigationInfo> children;
}
