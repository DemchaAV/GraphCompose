package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.data.CvRow;
import com.demcha.compose.document.templates.cv.v2.data.RowStyle;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

/**
 * Unified renderer for a {@link CvRow} under any {@link RowStyle}.
 *
 * <p>One public entry point dispatches on the style enum. All actual
 * paragraph drawing is delegated to {@link ParagraphPrimitive} so no
 * paragraph configuration is duplicated between this class and the
 * other renderers. Bullet glyphs and stacked-indent strings come
 * from {@code theme.decoration()} — changing them is a theme-level
 * decision, not a code change.</p>
 */
public final class RowRenderer {

    private RowRenderer() {
    }

    /**
     * Draws one row with the given decoration.
     *
     * @param section host
     * @param row     content (label + body)
     * @param style   decoration toggle
     * @param theme   active theme — supplies typography, palette,
     *                bullet glyph, and stacked indent
     */
    public static void render(SectionBuilder section, CvRow row,
                              RowStyle style, CvTheme theme) {
        switch (style) {
            case PLAIN -> inline(section, row, null, theme);
            case BULLETED -> inline(section, row, theme.decoration().bulletGlyph(), theme);
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
        DocumentInsets margin = DocumentInsets.top(
                (float) theme.spacing().paragraphMarginTop());
        if (bulletGlyph == null) {
            ParagraphPrimitive.writeBody(section, source, base, theme);
        } else {
            ParagraphPrimitive.writeBulleted(section, source, base,
                    bulletGlyph, margin, theme);
        }
    }

    /**
     * Renders the row as two stacked paragraphs: bullet + bold label
     * on line 1, body indented under the label on line 2. Both
     * paragraphs draw through {@link ParagraphPrimitive#writeBulleted}
     * so only the bullet glyph / margin differs.
     */
    private static void stacked(SectionBuilder section, CvRow row, CvTheme theme) {
        DocumentTextStyle base = theme.bodyStyle();
        DocumentTextStyle nameStyle = theme.bodyBoldStyle();
        DocumentInsets topMargin = DocumentInsets.top(
                (float) theme.spacing().paragraphMarginTop());

        // Line 1 — bullet + bold name.
        ParagraphPrimitive.writeBulleted(section, row.label(), nameStyle,
                theme.decoration().bulletGlyph(), topMargin, theme);

        if (row.body().isBlank()) {
            return;
        }
        // Line 2 — body indented under name (not under bullet).
        ParagraphPrimitive.writeBulleted(section, row.body(), base,
                theme.decoration().stackedIndent(), DocumentInsets.zero(), theme);
    }

    /**
     * Wraps the label in markdown bold markers + trailing colon so the
     * shared markdown helper emits one bold run for the label and a
     * regular run for the body without any extra typography plumbing.
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
