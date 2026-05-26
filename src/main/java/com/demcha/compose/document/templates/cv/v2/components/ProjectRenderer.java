package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.data.CvRow;

/**
 * Renders project rows that carry a title and optional technology
 * stack in the legacy "Project (Stack)" label shape.
 */
public final class ProjectRenderer {
    private ProjectRenderer() {
    }

    public static void inline(SectionBuilder host,
                              CvRow row,
                              DocumentTextStyle titleStyle,
                              DocumentTextStyle stackStyle,
                              DocumentTextStyle bodyStyle,
                              double lineSpacing,
                              DocumentInsets margin) {
        ProjectLabel label = ProjectLabel.parse(row.label());
        host.addParagraph(paragraph -> paragraph
                .textStyle(bodyStyle)
                .lineSpacing(lineSpacing)
                .align(TextAlign.LEFT)
                .margin(margin)
                .rich(rich -> {
                    rich.style(label.title(), titleStyle);
                    if (!label.stack().isBlank()) {
                        rich.style(" (" + label.stack() + ")", stackStyle);
                    }
                    if (!row.body().isBlank()) {
                        rich.style(" - ", bodyStyle);
                        MarkdownInline.appendTrimmed(rich, row.body(), bodyStyle);
                    }
                }));
    }

    public static void plainInline(SectionBuilder host,
                                   CvRow row,
                                   DocumentTextStyle labelStyle,
                                   DocumentTextStyle bodyStyle,
                                   double lineSpacing,
                                   DocumentInsets margin,
                                   String delimiter) {
        host.addParagraph(paragraph -> paragraph
                .textStyle(bodyStyle)
                .lineSpacing(lineSpacing)
                .align(TextAlign.LEFT)
                .margin(margin)
                .rich(rich -> {
                    rich.style(MarkdownInline.plainText(row.label()),
                            labelStyle);
                    if (!row.body().isBlank()) {
                        rich.style(delimiter, bodyStyle);
                        MarkdownInline.appendTrimmed(rich, row.body(), bodyStyle);
                    }
                }));
    }

    public static void titleThenBody(SectionBuilder host,
                                     CvRow row,
                                     DocumentTextStyle titleStyle,
                                     DocumentTextStyle stackStyle,
                                     DocumentTextStyle bodyStyle,
                                     double bodyLineSpacing,
                                     DocumentInsets titleMargin,
                                     DocumentInsets bodyMargin) {
        ProjectLabel label = ProjectLabel.parse(row.label());
        host.addParagraph(paragraph -> paragraph
                .textStyle(titleStyle)
                .align(TextAlign.LEFT)
                .margin(titleMargin)
                .rich(rich -> {
                    rich.style(label.title(), titleStyle);
                    if (!label.stack().isBlank()) {
                        rich.style(" (" + label.stack() + ")", stackStyle);
                    }
                }));
        RichParagraphRenderer.render(host, row.body(), bodyStyle,
                bodyLineSpacing, bodyMargin);
    }
}
