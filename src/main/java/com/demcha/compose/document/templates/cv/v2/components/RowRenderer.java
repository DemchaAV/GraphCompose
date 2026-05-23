package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.data.CvRow;
import com.demcha.compose.document.templates.cv.v2.data.RowStyle;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

/**
 * Unified renderer for a {@link CvRow} under any {@link RowStyle}.
 *
 * <p>One entry point — {@link #render(SectionBuilder, CvRow, RowStyle, CvTheme)} —
 * branches by style into three private helpers that share their
 * paragraph configuration. There is no copy-paste of {@code
 * .textStyle(...).lineSpacing(...).align(...)} across four near-identical
 * methods like the first v2 iteration had — the only thing that varies
 * per style is the bullet glyph and whether the row stacks onto a
 * second line.</p>
 */
public final class RowRenderer {

    private static final String BULLET_GLYPH = "• ";
    /** Two-space prefix so wrapped stacked-body text sits under the name. */
    private static final String STACK_INDENT = "  ";

    private RowRenderer() {
    }

    /**
     * Draws one row with the given decoration.
     *
     * @param section host
     * @param row     content (label + body)
     * @param style   decoration toggle
     * @param theme   active theme
     */
    public static void render(SectionBuilder section, CvRow row,
                              RowStyle style, CvTheme theme) {
        switch (style) {
            case PLAIN -> inline(section, row, null, theme);
            case BULLETED -> inline(section, row, BULLET_GLYPH, theme);
            case BULLETED_STACKED -> stacked(section, row, theme);
        }
    }

    /**
     * Renders the row as a single paragraph
     * {@code [bullet?] <b>label:</b> body}.
     *
     * @param bulletGlyph bullet glyph to attach, or null for plain
     */
    private static void inline(SectionBuilder section, CvRow row,
                               String bulletGlyph, CvTheme theme) {
        DocumentTextStyle base = theme.bodyStyle();
        String source = labelColonValue(row);
        section.addParagraph(p -> {
            p.textStyle(base)
                    .lineSpacing(theme.typography().bodyLineSpacing())
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top((float) theme.spacing().paragraphMarginTop()))
                    .rich(rich -> MarkdownInline.append(rich, source, base));
            if (bulletGlyph != null) {
                p.bulletOffset(bulletGlyph)
                        .indentStrategy(DocumentTextIndent.ALL_LINES);
            }
        });
    }

    /**
     * Renders the row as two stacked paragraphs:
     * {@code • <b>label</b>} on line 1, body indented under the label
     * on line 2.
     */
    private static void stacked(SectionBuilder section, CvRow row, CvTheme theme) {
        DocumentTextStyle base = theme.bodyStyle();
        DocumentTextStyle nameStyle = theme.bodyBoldStyle();

        // Line 1 — bullet + bold name (markdown still honoured).
        section.addParagraph(p -> p
                .textStyle(base)
                .lineSpacing(theme.typography().bodyLineSpacing())
                .align(TextAlign.LEFT)
                .margin(DocumentInsets.top((float) theme.spacing().paragraphMarginTop()))
                .bulletOffset(BULLET_GLYPH)
                .indentStrategy(DocumentTextIndent.ALL_LINES)
                .rich(rich -> MarkdownInline.append(rich, row.label(), nameStyle)));

        if (row.body().isBlank()) {
            return;
        }
        // Line 2 — body indented under name (not under bullet).
        section.addParagraph(p -> p
                .textStyle(base)
                .lineSpacing(theme.typography().bodyLineSpacing())
                .align(TextAlign.LEFT)
                .margin(DocumentInsets.zero())
                .bulletOffset(STACK_INDENT)
                .indentStrategy(DocumentTextIndent.ALL_LINES)
                .rich(rich -> MarkdownInline.append(rich, row.body(), base)));
    }

    /**
     * Wraps the label in markdown bold markers + trailing colon so the
     * existing inline-markdown parser emits one bold run for the label
     * and a regular run for the body without any extra typography
     * plumbing.
     */
    private static String labelColonValue(CvRow row) {
        StringBuilder source = new StringBuilder(
                row.label().length() + row.body().length() + 5);
        source.append("**").append(row.label()).append(":**");
        if (!row.body().isBlank()) {
            source.append(' ').append(row.body());
        }
        return source.toString();
    }
}
