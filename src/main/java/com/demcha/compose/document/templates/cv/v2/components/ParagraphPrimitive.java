package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

/**
 * Internal primitive that owns the shared
 * {@code section.addParagraph(p -> p.textStyle(...).lineSpacing(...).align(LEFT).margin(...).rich(...))}
 * skeleton used by every body / row / entry renderer in this package.
 *
 * <p>Higher-level renderers
 * ({@link ParagraphRenderer}, {@link RowRenderer},
 * {@link EntryRenderer}) compose their output by calling one of the
 * three short methods below — no one re-writes the paragraph DSL
 * configuration by hand, no two renderers can disagree about the
 * default alignment, and changing the markdown helper or the default
 * text alignment is a one-line edit.</p>
 *
 * <p>Not part of the public v2 API — package-private deliberately.
 * Renderers that consume {@link ParagraphPrimitive} are the public
 * surface; this class is their plumbing.</p>
 */
final class ParagraphPrimitive {

    private ParagraphPrimitive() {
    }

    /**
     * Body-style paragraph with the theme's default top margin, body
     * line spacing, no bullet. Used for prose paragraphs, entry body
     * lines, and plain rows.
     */
    static void writeBody(SectionBuilder host, String text,
                          DocumentTextStyle style, CvTheme theme) {
        write(host, text, style,
                DocumentInsets.top((float) theme.spacing().paragraphMarginTop()),
                theme.typography().bodyLineSpacing(),
                null);
    }

    /**
     * Body-style paragraph plus a bullet glyph + hanging indent. The
     * glyph is supplied by the caller (typically
     * {@code theme.decoration().bulletGlyph()} or
     * {@code theme.decoration().stackedIndent()} for the stacked
     * second line).
     */
    static void writeBulleted(SectionBuilder host, String text,
                              DocumentTextStyle style,
                              String bulletGlyph,
                              DocumentInsets margin,
                              CvTheme theme) {
        write(host, text, style,
                margin,
                theme.typography().bodyLineSpacing(),
                bulletGlyph);
    }

    /**
     * Subtitle-style paragraph — caller-supplied margin, no
     * lineSpacing override, no bullet. Used for the italic
     * employer/institution line under an entry title.
     */
    static void writeSubtitle(SectionBuilder host, String text,
                              DocumentTextStyle style) {
        write(host, text, style, DocumentInsets.zero(), null, null);
    }

    /**
     * Full-control variant — every knob exposed. Reserved for callers
     * that need a one-off combination not covered by the convenience
     * methods above.
     *
     * @param margin       paragraph margin
     * @param lineSpacing  optional lineSpacing override; null = use
     *                     engine default
     * @param bulletGlyph  optional bullet prefix; null = no bullet
     */
    static void write(SectionBuilder host, String text,
                      DocumentTextStyle style,
                      DocumentInsets margin,
                      Double lineSpacing,
                      String bulletGlyph) {
        host.addParagraph(p -> {
            p.textStyle(style)
                    .align(TextAlign.LEFT)
                    .margin(margin)
                    .rich(rich -> MarkdownInline.append(rich, text, style));
            if (lineSpacing != null) {
                p.lineSpacing(lineSpacing);
            }
            if (bulletGlyph != null) {
                p.bulletOffset(bulletGlyph)
                        .indentStrategy(DocumentTextIndent.ALL_LINES);
            }
        });
    }
}
