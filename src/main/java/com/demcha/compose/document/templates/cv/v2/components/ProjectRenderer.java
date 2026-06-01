package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.data.CvRow;

/**
 * Renders project rows that carry a title and optional technology
 * stack in the legacy "Project (Stack)" label shape.
 *
 * <p>Since v1.6.8 the title segment is routed through
 * {@link MarkdownInline#append(com.demcha.compose.document.dsl.RichText,
 * String, DocumentTextStyle)} rather than emitted as a flat styled
 * run, so {@code [name](url)} inside a {@link CvRow#label()} renders
 * as a clickable hyperlink. Labels without inline Markdown render
 * identically to before — the only visible change is that link
 * syntax now actually produces links.</p>
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
                    MarkdownInline.append(rich, label.title(), titleStyle);
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
                    // plainInline intentionally drops link syntax — it is
                    // for one-line listings where a clickable link would
                    // not survive the formatting context. Continue to use
                    // plainText so [name](url) appears as just "name".
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
                    MarkdownInline.append(rich, label.title(), titleStyle);
                    if (!label.stack().isBlank()) {
                        rich.style(" (" + label.stack() + ")", stackStyle);
                    }
                }));
        RichParagraphRenderer.render(host, row.body(), bodyStyle,
                bodyLineSpacing, bodyMargin);
    }
}
