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

    /**
     * Renders one skill category as a single line, listing up to
     * {@code limit} skills after the bolded category label.
     *
     * @param host        host section receiving the line
     * @param group       the skill group supplying the category and skills
     * @param limit       maximum number of skills to list
     * @param labelStyle  text style for the category label
     * @param valueStyle  text style for the joined skills
     * @param lineSpacing extra space between wrapped lines
     * @param margin      outer margin of the paragraph
     * @param labelSuffix text appended after the category label
     */
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
