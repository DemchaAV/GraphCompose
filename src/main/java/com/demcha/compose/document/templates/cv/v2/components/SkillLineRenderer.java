package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.data.SkillGroup;

import java.util.List;

/**
 * Compact single-line renderer for one skill category.
 */
public final class SkillLineRenderer {

    private SkillLineRenderer() {
    }

    public static void limitedInline(SectionBuilder host,
                                     SkillGroup group,
                                     int limit,
                                     DocumentTextStyle labelStyle,
                                     DocumentTextStyle valueStyle,
                                     double lineSpacing,
                                     DocumentInsets margin,
                                     String labelSuffix) {
        List<String> skills = group.skills().stream().limit(limit).toList();
        if (skills.isEmpty()) {
            return;
        }
        host.addParagraph(paragraph -> paragraph
                .textStyle(valueStyle)
                .lineSpacing(lineSpacing)
                .align(TextAlign.LEFT)
                .margin(margin)
                .rich(rich -> {
                    rich.style(MarkdownInline.plainText(group.category())
                            + labelSuffix, labelStyle);
                    rich.style(String.join(", ", skills), valueStyle);
                }));
    }
}
