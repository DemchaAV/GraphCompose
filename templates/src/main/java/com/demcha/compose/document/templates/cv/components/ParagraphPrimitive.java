package com.demcha.compose.document.templates.cv.components;

import com.demcha.compose.document.templates.core.text.MarkdownInline;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.core.theme.BrandTheme;

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

    /**
     * The gap between a bullet and its text, as a share of the type size.
     *
     * <p>A quarter of the size, which is what the trailing space inside
     * {@code Decoration.bulletGlyph} was approximating — at the sizes these sheets
     * set, one space of their body faces measures within a fraction of a point of
     * it. A share rather than a value, so the gap follows the type instead of
     * needing one number per theme.</p>
     */
    private static final double BULLET_GAP_EM = 0.25;

    private ParagraphPrimitive() {
    }

    /**
     * Body-style paragraph with the theme's default top margin, body
     * line spacing, no bullet. Used for prose paragraphs, entry body
     * lines, and plain rows.
     */
    static void writeBody(SectionBuilder host, String text,
                          DocumentTextStyle style, BrandTheme theme) {
        write(host, text, style,
                DocumentInsets.top((float) theme.spacing().paragraphMarginTop()),
                theme.typography().bodyLineSpacing());
    }

    /**
     * One bulleted row, as a list of one item.
     *
     * <p>A real list, so the marker sits in its own measured column and every
     * visual line of the row — the ones it wraps onto, the ones that continue on
     * a later page — starts at one content origin. It was a paragraph with the
     * glyph as {@code bulletOffset} under {@code DocumentTextIndent.ALL_LINES},
     * which indents continuation lines with a run of spaces measured to clear the
     * glyph; a whole number of spaces rarely equals a bullet, and that
     * approximation is what the marker column replaces.</p>
     *
     * <p>The content is inline runs, so the bold label of a {@code label: body}
     * row is part of one item rather than a row of two columns pretending to be
     * one.</p>
     */
    static void writeBulleted(SectionBuilder host, String text,
                              DocumentTextStyle style,
                              String bulletGlyph,
                              DocumentInsets margin,
                              BrandTheme theme) {
        host.addList(list -> list
                .textStyle(style)
                .align(TextAlign.LEFT)
                .lineSpacing(theme.typography().bodyLineSpacing())
                .marker(bulletGlyph)
                .hangingIndent(true)
                .markerGap(style.size() * BULLET_GAP_EM)
                .normalizeMarkers(false)
                .margin(margin)
                .addItem(rich -> MarkdownInline.append(rich, text, style)));
    }

    /**
     * A bulleted name with its body hanging under it — the two lines of a
     * stacked row, as one list.
     *
     * <p>The body is a <b>markerless child</b> of the name's item, which is what
     * makes it begin where the name's text begins: a child's marker column opens
     * at its parent's content origin, and a child with no marker takes no marker
     * width and no gap, so its content starts exactly there. That is the
     * alignment the row is drawn for and could not state.</p>
     *
     * <p>It was two paragraphs, the second prefixed with
     * {@code Decoration.stackedIndent}, whose own contract is that it "must
     * visually occupy the same width as bulletGlyph". Two spaces do not: measured
     * on the canonical document the body began between 0.720pt and 3.078pt to the
     * left of the name above it, the amount depending on the theme's type size.
     * The indent is geometry now, so the two agree by construction.</p>
     */
    static void writeBulletedPair(SectionBuilder host,
                                  String name, String body,
                                  DocumentTextStyle nameStyle,
                                  DocumentTextStyle bodyStyle,
                                  String bulletGlyph,
                                  DocumentInsets margin,
                                  BrandTheme theme) {
        host.addList(list -> {
            // The list's style is the name's, because the marker is drawn in it and
            // the bullet in front of a bold name was bold before this. Both items
            // carry their own runs, so the body is unaffected by the choice.
            list.textStyle(nameStyle)
                    .align(TextAlign.LEFT)
                    .lineSpacing(theme.typography().bodyLineSpacing())
                    .marker(bulletGlyph)
                    .markerFor(1, ListMarker.none())
                    .hangingIndent(true)
                    .markerGap(nameStyle.size() * BULLET_GAP_EM)
                    .normalizeMarkers(false)
                    // The name and the body were two children of the host section,
                    // so the host's own spacing sat between them. One list is one
                    // child, so that gap is stated here or the row closes up — and
                    // this change is meant to move the body sideways, not to
                    // re-space the sheet.
                    .itemSpacing(theme.spacing().sectionBodySpacing())
                    .margin(margin);
            if (body == null || body.isBlank()) {
                list.addItem(rich -> MarkdownInline.append(rich, name, nameStyle));
                return;
            }
            list.addItem(rich -> MarkdownInline.append(rich, name, nameStyle),
                    child -> child.addItem(
                            rich -> MarkdownInline.append(rich, body, bodyStyle)));
        });
    }

    /**
     * Subtitle-style paragraph — caller-supplied margin, no
     * lineSpacing override, no bullet. Used for the italic
     * employer/institution line under an entry title.
     */
    static void writeSubtitle(SectionBuilder host, String text,
                              DocumentTextStyle style) {
        write(host, text, style, DocumentInsets.zero(), null);
    }

    /**
     * Full-control variant — every knob exposed. Reserved for callers
     * that need a one-off combination not covered by the convenience
     * methods above.
     *
     * <p>It used to take a bullet glyph too, applied as {@code bulletOffset}
     * under {@code DocumentTextIndent.ALL_LINES}. Bulleted rows are lists now,
     * so nothing passed one and the branch was reachable only with null.</p>
     *
     * @param margin      paragraph margin
     * @param lineSpacing optional lineSpacing override; null = use
     *                    engine default
     */
    static void write(SectionBuilder host, String text,
                      DocumentTextStyle style,
                      DocumentInsets margin,
                      Double lineSpacing) {
        host.addParagraph(p -> {
            p.textStyle(style)
                    .align(TextAlign.LEFT)
                    .margin(margin)
                    .rich(rich -> MarkdownInline.append(rich, text, style));
            if (lineSpacing != null) {
                p.lineSpacing(lineSpacing);
            }
        });
    }
}
