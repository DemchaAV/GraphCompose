package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.data.CvEntry;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

/**
 * Renders one {@link CvEntry} as the canonical four-zone timeline
 * row used by both Education and Professional Experience:
 *
 * <pre>
 *  &lt;Title bold, left&gt;                      &lt;Date, right&gt;
 *  &lt;Subtitle, italic, muted&gt;
 *  &lt;Body, full-width paragraph&gt;
 * </pre>
 *
 * <p>Blank fields collapse — a blank date drops the right column,
 * a blank subtitle drops the italic line, a blank body drops the
 * paragraph beneath. The same renderer fits both flavours because
 * the visual is identical; if a preset wants to style Education
 * differently it does so by passing a different theme, not by
 * forking this code.</p>
 */
public final class EntryRenderer {

    private EntryRenderer() {
    }

    public static void render(SectionBuilder section, CvEntry entry, CvTheme theme) {
        DocumentTextStyle titleStyle = theme.entryTitleStyle();
        DocumentTextStyle dateStyle = theme.entryDateStyle();
        DocumentTextStyle subtitleStyle = theme.entrySubtitleStyle();
        DocumentTextStyle bodyStyle = theme.bodyStyle();

        // -- title + date row -------------------------------------------
        section.addRow("CvV2EntryHeader", row -> row
                .spacing(theme.spacing().entryHeaderRowSpacing())
                .weights(theme.spacing().entryTitleWeight(),
                        theme.spacing().entryDateWeight())
                .addSection("Title", titleColumn -> titleColumn
                        .padding(DocumentInsets.zero())
                        .addParagraph(p -> p
                                .text(entry.title())
                                .textStyle(titleStyle)
                                .align(TextAlign.LEFT)
                                .margin(DocumentInsets.zero())))
                .addSection("Date", dateColumn -> dateColumn
                        .padding(DocumentInsets.zero())
                        .addParagraph(p -> p
                                .text(entry.date())
                                .textStyle(dateStyle)
                                .align(TextAlign.RIGHT)
                                .margin(DocumentInsets.zero()))));

        // -- italic subtitle --------------------------------------------
        if (!entry.subtitle().isBlank()) {
            section.addParagraph(p -> p
                    .textStyle(subtitleStyle)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.zero())
                    .rich(rich -> MarkdownInline.append(rich,
                            entry.subtitle(), subtitleStyle)));
        }

        // -- body paragraph ---------------------------------------------
        if (!entry.body().isBlank()) {
            section.addParagraph(p -> p
                    .textStyle(bodyStyle)
                    .lineSpacing(theme.typography().bodyLineSpacing())
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top((float) theme.spacing().paragraphMarginTop()))
                    .rich(rich -> MarkdownInline.append(rich,
                            entry.body(), bodyStyle)));
        }
    }
}
