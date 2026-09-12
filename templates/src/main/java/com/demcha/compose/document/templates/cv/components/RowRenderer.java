package com.demcha.compose.document.templates.cv.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.data.CvRow;
import com.demcha.compose.document.templates.cv.data.RowStyle;
import com.demcha.compose.document.templates.core.theme.BrandTheme;

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
                              RowStyle style, BrandTheme theme) {
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
                               String bulletGlyph, BrandTheme theme) {
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
     * Renders the row as a bulleted bold name with its body hanging under the
     * name rather than under the bullet.
     *
     * <p>The two are one list: the name is its item and the body a markerless
     * child of that item, so the body begins at the name's own content origin.
     * {@code Decoration.stackedIndent} is no longer read for this — a run of
     * spaces cannot equal a measured marker column, which is exactly why the
     * body used to start a little to the left of the name it hangs under.</p>
     */
    private static void stacked(SectionBuilder section, CvRow row, BrandTheme theme) {
        ParagraphPrimitive.writeBulletedPair(section,
                row.label(), row.body(),
                theme.bodyBoldStyle(), theme.bodyStyle(),
                theme.decoration().bulletGlyph(),
                DocumentInsets.top((float) theme.spacing().paragraphMarginTop()),
                theme);
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
