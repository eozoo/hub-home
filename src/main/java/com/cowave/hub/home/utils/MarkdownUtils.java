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
package com.cowave.hub.home.utils;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.ext.ins.InsExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 *
 * 参考 <a href="https://blog.csdn.net/qq_41609208/article/details/106824386"/>
 *
 */
public class MarkdownUtils {

    private static final List<Extension> EXTENSIONS = Arrays.asList(
            TablesExtension.create(),
            ImageAttributesExtension.create(),
            AutolinkExtension.create(),
            StrikethroughExtension.create(),
            HeadingAnchorExtension.create(),
            InsExtension.create(),
            YamlFrontMatterExtension.create(),
            TaskListItemsExtension.create()
    );

    private static HashSet enabledBlockTypes = new HashSet(Arrays.asList(
            Document.class,Image.class,Link.class,Paragraph.class,Text.class
    ));

    /**
     * 解析Markdown文档
     */
    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();

    /**
     * 渲染HTML文档
     */
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().extensions(EXTENSIONS).attributeProviderFactory(context -> new BlogAttributeProvider()).build();

    /**
     * 渲染Markdown
     *
     * @param content content
     * @return String
     */
    public static String renderMarkdown(String content) {
        final Node document = PARSER.parse(content);
        return RENDERER.render(document);
    }

    static class BlogAttributeProvider implements AttributeProvider {
        @Override
        public void setAttributes(Node node, String s, Map<String, String> map) {
            if (node instanceof TableBlock) {
                map.put("class", "table table-bordered");
            }
            if (node instanceof Link) {
                map.put("target", "_blank");
            }
        }
    }
}
