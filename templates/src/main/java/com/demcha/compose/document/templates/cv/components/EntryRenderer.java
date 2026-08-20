package com.demcha.compose.document.templates.cv.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.core.text.MarkdownInline;
import com.demcha.compose.document.templates.core.theme.BrandTheme;

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
 * paragraph beneath.</p>
 *
 * <p>The subtitle and body paragraphs go through
 * {@link ParagraphPrimitive} so they share alignment, margin, and
 * markdown handling with every other renderer in this package.</p>
 */
public final class EntryRenderer {

    private EntryRenderer() {
    }

    /**
     * Renders one entry as the four-zone timeline row described above.
     *
     * @param section the section builder being populated
     * @param entry   the entry supplying title, date, subtitle, and body
     * @param theme   the active theme supplying palette, typography, and spacing
     */
    public static void render(SectionBuilder section, CvEntry entry, BrandTheme theme) {
        DocumentTextStyle titleStyle = theme.entryTitleStyle();
        DocumentTextStyle dateStyle = theme.entryDateStyle();
        DocumentTextStyle subtitleStyle = theme.entrySubtitleStyle();
        DocumentTextStyle bodyStyle = theme.bodyStyle();

        // -- title (+ date) header --------------------------------------
        // With a date this is a two-column row, not a paragraph, so it does
        // not go through ParagraphPrimitive — its DSL shape is genuinely
        // different. Without one the row is dropped entirely rather than
        // reserving an empty column: an undated entry — a certification, a
        // project, anything a runtime module renders without dates — would
        // otherwise have its title wrapped early to leave room for nothing.
        if (entry.date().isBlank()) {
            section.addParagraph(p -> p
                    .textStyle(titleStyle)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.zero())
                    .rich(rich -> MarkdownInline.append(rich, entry.title(), titleStyle)));
        } else {
            section.addRow("CvV2EntryHeader", row -> row
                    .spacing(theme.spacing().entryHeaderRowSpacing())
                    .weights(theme.spacing().entryTitleWeight(),
                            theme.spacing().entryDateWeight())
                    .addSection("Title", titleColumn -> titleColumn
                            .padding(DocumentInsets.zero())
                            .addParagraph(p -> p
                                    .textStyle(titleStyle)
                                    .align(TextAlign.LEFT)
                                    .margin(DocumentInsets.zero())
                                    .rich(rich -> MarkdownInline.append(rich, entry.title(), titleStyle))))
                    .addSection("Date", dateColumn -> dateColumn
                            .padding(DocumentInsets.zero())
                            .addParagraph(p -> p
                                    .text(entry.date())
                                    .textStyle(dateStyle)
                                    .align(TextAlign.RIGHT)
                                    .margin(DocumentInsets.zero()))));
        }

        // -- italic subtitle --------------------------------------------
        if (!entry.subtitle().isBlank()) {
            ParagraphPrimitive.writeSubtitle(section, entry.subtitle(), subtitleStyle);
        }

        // -- body paragraph ---------------------------------------------
        if (!entry.body().isBlank()) {
            ParagraphPrimitive.writeBody(section, entry.body(), bodyStyle, theme);
        }
    }
}
