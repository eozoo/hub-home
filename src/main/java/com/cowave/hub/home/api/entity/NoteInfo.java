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

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 动态
 *
 * @author shanhuiming
 */
@Data
@TableName("info_note")
public class NoteInfo {

    /**
     * 唯一id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标题
     */
    private String noteTitle;

    /**
     * 作者id
     */
    private Long authorId;

    /**
     * 图片（预留字段，暂时不考虑）
     */
    private String noteImage;

    /**
     * 内容
     */
    private String noteContent;

    /**
     * 摘要
     */
    private String noteSummary;

    /**
     * 类型（数据字典，不写死）
     */
    private String noteType;

    /**
     * 状态
     */
    private String noteStatus;

    /**
     * 是否公共，0公共，  1 私有
     */
    private String isPublic;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 备注
     */
    private String remark;

    @TableField(exist = false)
    private String praiseNum;
}
