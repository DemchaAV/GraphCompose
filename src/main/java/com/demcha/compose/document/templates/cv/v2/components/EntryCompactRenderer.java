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

    /**
     * Renders title and date as a two-column row, then an optional
     * subtitle line and the body paragraph beneath.
     *
     * @param host           host section receiving the entry
     * @param entry          the entry supplying title, date, subtitle, and body
     * @param rowName        node name for the title/date row
     * @param titleStyle     text style for the title column
     * @param dateStyle      text style for the date column
     * @param subtitleStyle  text style for the subtitle line
     * @param bodyStyle      text style for the body paragraph
     * @param rowSpacing     horizontal spacing between the title and date columns
     * @param titleWeight    relative weight of the title column
     * @param dateWeight     relative weight of the date column
     * @param subtitleMargin outer margin of the subtitle line
     * @param bodyMargin     outer margin of the body paragraph
     * @param bodyLineSpacing extra space between wrapped body lines
     * @param uppercaseTitle whether the title is upper-cased
     */
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

    /**
     * Renders the title followed by subtitle and date appended as
     * {@code " / "}-separated meta on a single line.
     *
     * @param host        host section receiving the entry
     * @param entry       the entry supplying title, subtitle, and date
     * @param titleStyle  text style for the title
     * @param metaStyle   text style for the appended subtitle and date
     * @param lineSpacing extra space between wrapped lines
     * @param margin      outer margin of the paragraph
     */
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

    /**
     * Renders the title followed by subtitle and date appended as
     * {@code " / "}-separated meta, each in its own style.
     *
     * @param host          host section receiving the entry
     * @param entry         the entry supplying title, subtitle, and date
     * @param titleStyle    text style for the title
     * @param subtitleStyle text style for the appended subtitle
     * @param dateStyle     text style for the appended date
     * @param lineSpacing   extra space between wrapped lines
     * @param margin        outer margin of the paragraph
     */
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

    /**
     * Renders the title with the date appended inline after
     * {@code datePrefix}, then an optional subtitle line and the body
     * paragraph beneath.
     *
     * @param host             host section receiving the entry
     * @param entry            the entry supplying title, date, subtitle, and body
     * @param titleStyle       text style for the title
     * @param dateStyle        text style for the appended date
     * @param subtitleStyle    text style for the subtitle line
     * @param bodyStyle        text style for the body paragraph
     * @param datePrefix       separator inserted between title and date
     * @param headerLineSpacing extra space between wrapped header lines
     * @param headerMargin     outer margin of the title/date header
     * @param subtitleMargin   outer margin of the subtitle line
     * @param bodyMargin       outer margin of the body paragraph
     * @param bodyLineSpacing  extra space between wrapped body lines
     * @param uppercaseTitle   whether the title is upper-cased
     */
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

    /**
     * Renders the title with subtitle and date appended inline after
     * {@code subtitlePrefix} and {@code datePrefix}, then the body
     * paragraph beneath.
     *
     * @param host             host section receiving the entry
     * @param entry            the entry supplying title, subtitle, date, and body
     * @param titleStyle       text style for the title
     * @param subtitleStyle    text style for the appended subtitle
     * @param dateStyle        text style for the appended date
     * @param bodyStyle        text style for the body paragraph
     * @param subtitlePrefix   separator inserted before the subtitle
     * @param datePrefix       separator inserted before the date
     * @param headerLineSpacing extra space between wrapped header lines
     * @param headerMargin     outer margin of the header paragraph
     * @param bodyMargin       outer margin of the body paragraph
     * @param bodyLineSpacing  extra space between wrapped body lines
     */
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
