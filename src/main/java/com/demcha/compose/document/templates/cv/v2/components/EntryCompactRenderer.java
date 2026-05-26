package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.data.CvEntry;

import java.util.Locale;

/**
 * Compact entry renderer for editorial/card/rail presets where title,
 * subtitle, and date are packed tighter than the canonical
 * two-column {@link EntryRenderer}.
 */
public final class EntryCompactRenderer {
    private EntryCompactRenderer() {
    }

    public static void twoColumnTitleDateBody(SectionBuilder host,
                                              CvEntry entry,
                                              String rowName,
                                              DocumentTextStyle titleStyle,
                                              DocumentTextStyle dateStyle,
                                              DocumentTextStyle subtitleStyle,
                                              DocumentTextStyle bodyStyle,
                                              double rowSpacing,
                                              double titleWeight,
                                              double dateWeight,
                                              DocumentInsets subtitleMargin,
                                              DocumentInsets bodyMargin,
                                              double bodyLineSpacing,
                                              boolean uppercaseTitle) {
        host.addRow(rowName, row -> row
                .spacing(rowSpacing)
                .weights(titleWeight, dateWeight)
                .addSection("Title", titleColumn -> titleColumn
                        .padding(DocumentInsets.zero())
                        .addParagraph(paragraph -> paragraph
                                .text(formattedTitle(entry.title(),
                                        uppercaseTitle))
                                .textStyle(titleStyle)
                                .align(TextAlign.LEFT)
                                .margin(DocumentInsets.zero())))
                .addSection("Date", dateColumn -> dateColumn
                        .padding(DocumentInsets.zero())
                        .addParagraph(paragraph -> paragraph
                                .text(MarkdownInline.plainText(entry.date()))
                                .textStyle(dateStyle)
                                .align(TextAlign.RIGHT)
                                .margin(DocumentInsets.zero()))));

        if (!entry.subtitle().isBlank()) {
            host.addParagraph(paragraph -> paragraph
                    .text(MarkdownInline.plainText(entry.subtitle()))
                    .textStyle(subtitleStyle)
                    .align(TextAlign.LEFT)
                    .margin(subtitleMargin));
        }
        RichParagraphRenderer.render(host, entry.body(), bodyStyle,
                bodyLineSpacing, bodyMargin);
    }

    public static void slashMeta(SectionBuilder host,
                                 CvEntry entry,
                                 DocumentTextStyle titleStyle,
                                 DocumentTextStyle metaStyle,
                                 double lineSpacing,
                                 DocumentInsets margin) {
        host.addParagraph(paragraph -> paragraph
                .textStyle(titleStyle)
                .lineSpacing(lineSpacing)
                .align(TextAlign.LEFT)
                .margin(margin)
                .rich(rich -> {
                    rich.style(MarkdownInline.plainText(entry.title()),
                            titleStyle);
                    MarkdownInline.appendPlainIfPresent(rich, " / ",
                            entry.subtitle(), metaStyle);
                    MarkdownInline.appendPlainIfPresent(rich, " / ",
                            entry.date(), metaStyle);
                }));
    }

    public static void slashSubtitleDate(SectionBuilder host,
                                         CvEntry entry,
                                         DocumentTextStyle titleStyle,
                                         DocumentTextStyle subtitleStyle,
                                         DocumentTextStyle dateStyle,
                                         double lineSpacing,
                                         DocumentInsets margin) {
        host.addParagraph(paragraph -> paragraph
                .textStyle(titleStyle)
                .lineSpacing(lineSpacing)
                .align(TextAlign.LEFT)
                .margin(margin)
                .rich(rich -> {
                    rich.style(MarkdownInline.plainText(entry.title()),
                            titleStyle);
                    MarkdownInline.appendPlainIfPresent(rich, " / ",
                            entry.subtitle(), subtitleStyle);
                    MarkdownInline.appendPlainIfPresent(rich, " / ",
                            entry.date(), dateStyle);
                }));
    }

    public static void titleDateBody(SectionBuilder host,
                                     CvEntry entry,
                                     DocumentTextStyle titleStyle,
                                     DocumentTextStyle dateStyle,
                                     DocumentTextStyle subtitleStyle,
                                     DocumentTextStyle bodyStyle,
                                     String datePrefix,
                                     double headerLineSpacing,
                                     DocumentInsets headerMargin,
                                     DocumentInsets subtitleMargin,
                                     DocumentInsets bodyMargin,
                                     double bodyLineSpacing,
                                     boolean uppercaseTitle) {
        host.addParagraph(paragraph -> paragraph
                .textStyle(titleStyle)
                .lineSpacing(headerLineSpacing)
                .align(TextAlign.LEFT)
                .margin(headerMargin)
                .rich(rich -> {
                    rich.style(formattedTitle(entry.title(), uppercaseTitle),
                            titleStyle);
                    if (!entry.date().isBlank()) {
                        rich.style(datePrefix, titleStyle);
                        rich.style(MarkdownInline.plainText(entry.date()),
                                dateStyle);
                    }
                }));
        if (!entry.subtitle().isBlank()) {
            host.addParagraph(paragraph -> paragraph
                    .text(MarkdownInline.plainText(entry.subtitle()))
                    .textStyle(subtitleStyle)
                    .align(TextAlign.LEFT)
                    .margin(subtitleMargin));
        }
        RichParagraphRenderer.render(host, entry.body(), bodyStyle,
                bodyLineSpacing, bodyMargin);
    }

    public static void titleSubtitleDateBody(SectionBuilder host,
                                             CvEntry entry,
                                             DocumentTextStyle titleStyle,
                                             DocumentTextStyle subtitleStyle,
                                             DocumentTextStyle dateStyle,
                                             DocumentTextStyle bodyStyle,
                                             String subtitlePrefix,
                                             String datePrefix,
                                             double headerLineSpacing,
                                             DocumentInsets headerMargin,
                                             DocumentInsets bodyMargin,
                                             double bodyLineSpacing) {
        host.addParagraph(paragraph -> paragraph
                .textStyle(titleStyle)
                .lineSpacing(headerLineSpacing)
                .align(TextAlign.LEFT)
                .margin(headerMargin)
                .rich(rich -> {
                    rich.style(MarkdownInline.plainText(entry.title()),
                            titleStyle);
                    MarkdownInline.appendPlainIfPresent(rich, subtitlePrefix,
                            entry.subtitle(), subtitleStyle);
                    MarkdownInline.appendPlainIfPresent(rich, datePrefix,
                            entry.date(), dateStyle);
                }));
        RichParagraphRenderer.render(host, entry.body(), bodyStyle,
                bodyLineSpacing, bodyMargin);
    }

    private static String formattedTitle(String title, boolean uppercase) {
        String clean = MarkdownInline.plainText(title);
        return uppercase ? clean.toUpperCase(Locale.ROOT) : clean;
    }
}
