package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextAutoSize;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.List;
import java.util.Objects;

/**
 * Semantic paragraph block that may split across pages.
 *
 * @param name            node name used in snapshots and layout graph paths
 * @param text            paragraph text when inline runs are not supplied
 * @param inlineRuns      optional inline runs in source order; may mix text and
 *                        image runs and is wrapped on a single baseline
 * @param textStyle       base paragraph text style
 * @param align           horizontal text alignment
 * @param lineSpacing     extra space between wrapped lines
 * @param bulletOffset    first-line prefix used by list-style paragraph paths
 * @param indentStrategy  hanging/first-line indent strategy
 * @param linkTarget      optional node-level link target (external URI or internal anchor)
 * @param bookmarkOptions optional node-level bookmark metadata
 * @param padding         inner padding
 * @param margin          outer margin
 * @param autoSize        optional automatic font down-scaling policy
 * @param verticalAlign   vertical seating of the text within its line box
 *                        ({@link TextVerticalAlign#DEFAULT} keeps baseline seating)
 * @param anchor          optional in-document navigation anchor name declared at the
 *                        paragraph's top-left, or {@code null} for none
 * @param direction       writing direction; {@link TextDirection#LTR} when omitted
 * @author Artem Demchyshyn
 */
public record ParagraphNode(
        String name,
        String text,
        List<InlineRun> inlineRuns,
        DocumentTextStyle textStyle,
        TextAlign align,
        double lineSpacing,
        String bulletOffset,
        DocumentTextIndent indentStrategy,
        DocumentLinkTarget linkTarget,
        DocumentBookmarkOptions bookmarkOptions,
        DocumentInsets padding,
        DocumentInsets margin,
        DocumentTextAutoSize autoSize,
        TextVerticalAlign verticalAlign,
        String anchor,
        TextDirection direction
) implements DocumentNode {
    /**
     * Normalizes optional text, inline runs, style, alignment, spacing, and
     * indentation defaults for a canonical paragraph.
     */
    public ParagraphNode {
        name = name == null ? "" : name;
        inlineRuns = normalizeInlineRuns(inlineRuns);
        text = Objects.requireNonNullElse(text, "");
        if (text.isBlank() && !inlineRuns.isEmpty()) {
            StringBuilder concatenated = new StringBuilder();
            for (InlineRun run : inlineRuns) {
                if (run instanceof InlineTextRun textRun) {
                    concatenated.append(textRun.text());
                } else if (run instanceof InlineHighlightRun highlight) {
                    concatenated.append(highlight.text());
                }
            }
            text = concatenated.toString();
        }
        textStyle = textStyle == null ? DocumentTextStyle.DEFAULT : textStyle;
        align = align == null ? TextAlign.LEFT : align;
        bulletOffset = Objects.requireNonNullElse(bulletOffset, "");
        indentStrategy = indentStrategy == null ? DocumentTextIndent.NONE : indentStrategy;
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
        verticalAlign = verticalAlign == null ? TextVerticalAlign.DEFAULT : verticalAlign;
        anchor = anchor == null || anchor.isBlank() ? null : anchor.trim();
        direction = direction == null ? TextDirection.LTR : direction;
        if (lineSpacing < 0 || Double.isNaN(lineSpacing) || Double.isInfinite(lineSpacing)) {
            throw new IllegalArgumentException("lineSpacing must be finite and non-negative: " + lineSpacing);
        }
    }

    /**
     * Backwards-compatible 15-arg constructor without a writing direction; defaults
     * {@link TextDirection#LTR}.
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param text            paragraph text when inline runs are not supplied
     * @param inlineRuns      optional inline runs in source order
     * @param textStyle       base paragraph text style
     * @param align           horizontal text alignment
     * @param lineSpacing     extra space between wrapped lines
     * @param bulletOffset    first-line prefix used by list-style paragraph paths
     * @param indentStrategy  hanging/first-line indent strategy
     * @param linkTarget      optional node-level link target
     * @param bookmarkOptions optional node-level bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     * @param autoSize        optional automatic font down-scaling policy
     * @param verticalAlign   vertical seating of the text within its line box
     * @param anchor          optional in-document navigation anchor name
     */
    public ParagraphNode(String name,
                         String text,
                         List<InlineRun> inlineRuns,
                         DocumentTextStyle textStyle,
                         TextAlign align,
                         double lineSpacing,
                         String bulletOffset,
                         DocumentTextIndent indentStrategy,
                         DocumentLinkTarget linkTarget,
                         DocumentBookmarkOptions bookmarkOptions,
                         DocumentInsets padding,
                         DocumentInsets margin,
                         DocumentTextAutoSize autoSize,
                         TextVerticalAlign verticalAlign,
                         String anchor) {
        this(name, text, inlineRuns, textStyle, align, lineSpacing, bulletOffset, indentStrategy,
                linkTarget, bookmarkOptions, padding, margin, autoSize, verticalAlign, anchor,
                TextDirection.LTR);
    }

    /**
     * Backwards-compatible 14-arg constructor taking external
     * {@link DocumentLinkOptions} (wrapped into an {@link ExternalLinkTarget})
     * and no navigation anchor.
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param text            paragraph text when inline runs are not supplied
     * @param inlineRuns      optional inline runs in source order
     * @param textStyle       base paragraph text style
     * @param align           horizontal text alignment
     * @param lineSpacing     extra space between wrapped lines
     * @param bulletOffset    first-line prefix used by list-style paragraph paths
     * @param indentStrategy  hanging/first-line indent strategy
     * @param linkOptions     optional external link metadata
     * @param bookmarkOptions optional node-level bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     * @param autoSize        optional automatic font down-scaling policy
     * @param verticalAlign   vertical seating of the text within its line box
     */
    public ParagraphNode(String name,
                         String text,
                         List<InlineRun> inlineRuns,
                         DocumentTextStyle textStyle,
                         TextAlign align,
                         double lineSpacing,
                         String bulletOffset,
                         DocumentTextIndent indentStrategy,
                         DocumentLinkOptions linkOptions,
                         DocumentBookmarkOptions bookmarkOptions,
                         DocumentInsets padding,
                         DocumentInsets margin,
                         DocumentTextAutoSize autoSize,
                         TextVerticalAlign verticalAlign) {
        this(name, text, inlineRuns, textStyle, align, lineSpacing, bulletOffset, indentStrategy,
                linkOptions == null ? null : new ExternalLinkTarget(linkOptions),
                bookmarkOptions, padding, margin, autoSize, verticalAlign, null);
    }

    /**
     * Backwards-compatible 13-arg constructor without a vertical-alignment
     * override; defaults {@link TextVerticalAlign#DEFAULT}.
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param text            paragraph text when inline runs are not supplied
     * @param inlineRuns      optional inline runs in source order
     * @param textStyle       base paragraph text style
     * @param align           horizontal text alignment
     * @param lineSpacing     extra space between wrapped lines
     * @param bulletOffset    first-line prefix used by list-style paragraph paths
     * @param indentStrategy  hanging/first-line indent strategy
     * @param linkOptions     optional node-level link metadata
     * @param bookmarkOptions optional node-level bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     * @param autoSize        optional automatic font down-scaling policy
     */
    public ParagraphNode(String name,
                         String text,
                         List<InlineRun> inlineRuns,
                         DocumentTextStyle textStyle,
                         TextAlign align,
                         double lineSpacing,
                         String bulletOffset,
                         DocumentTextIndent indentStrategy,
                         DocumentLinkOptions linkOptions,
                         DocumentBookmarkOptions bookmarkOptions,
                         DocumentInsets padding,
                         DocumentInsets margin,
                         DocumentTextAutoSize autoSize) {
        this(name, text, inlineRuns, textStyle, align, lineSpacing, bulletOffset, indentStrategy,
                linkOptions, bookmarkOptions, padding, margin, autoSize, TextVerticalAlign.DEFAULT);
    }

    /**
     * Backwards-compatible 12-arg canonical constructor without auto-size.
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param text            paragraph text when inline runs are not supplied
     * @param inlineRuns      optional inline runs in source order; may mix text and
     *                        image runs and is wrapped on a single baseline
     * @param textStyle       base paragraph text style
     * @param align           horizontal text alignment
     * @param lineSpacing     extra space between wrapped lines
     * @param bulletOffset    first-line prefix used by list-style paragraph paths
     * @param indentStrategy  hanging/first-line indent strategy
     * @param linkOptions     optional node-level link metadata
     * @param bookmarkOptions optional node-level bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     */
    public ParagraphNode(String name,
                         String text,
                         List<InlineRun> inlineRuns,
                         DocumentTextStyle textStyle,
                         TextAlign align,
                         double lineSpacing,
                         String bulletOffset,
                         DocumentTextIndent indentStrategy,
                         DocumentLinkOptions linkOptions,
                         DocumentBookmarkOptions bookmarkOptions,
                         DocumentInsets padding,
                         DocumentInsets margin) {
        this(name, text, inlineRuns, textStyle, align, lineSpacing, bulletOffset, indentStrategy,
                linkOptions, bookmarkOptions, padding, margin, null);
    }

    /**
     * Creates a paragraph from plain text with optional link and bookmark metadata.
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param text            paragraph text
     * @param textStyle       paragraph style
     * @param align           horizontal alignment
     * @param lineSpacing     extra spacing between wrapped lines
     * @param bulletOffset    first-line marker or prefix
     * @param indentStrategy  text indent strategy
     * @param linkOptions     optional link metadata
     * @param bookmarkOptions optional bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     */
    public ParagraphNode(String name,
                         String text,
                         DocumentTextStyle textStyle,
                         TextAlign align,
                         double lineSpacing,
                         String bulletOffset,
                         DocumentTextIndent indentStrategy,
                         DocumentLinkOptions linkOptions,
                         DocumentBookmarkOptions bookmarkOptions,
                         DocumentInsets padding,
                         DocumentInsets margin) {
        this(name, text, List.of(), textStyle, align, lineSpacing, bulletOffset, indentStrategy, linkOptions, bookmarkOptions, padding, margin);
    }

    /**
     * Creates a paragraph from plain text without link or bookmark metadata.
     *
     * @param name           node name used in snapshots and layout graph paths
     * @param text           paragraph text
     * @param textStyle      paragraph style
     * @param align          horizontal alignment
     * @param lineSpacing    extra spacing between wrapped lines
     * @param bulletOffset   first-line marker or prefix
     * @param indentStrategy text indent strategy
     * @param padding        inner padding
     * @param margin         outer margin
     */
    public ParagraphNode(String name,
                         String text,
                         DocumentTextStyle textStyle,
                         TextAlign align,
                         double lineSpacing,
                         String bulletOffset,
                         DocumentTextIndent indentStrategy,
                         DocumentInsets padding,
                         DocumentInsets margin) {
        this(name, text, textStyle, align, lineSpacing, bulletOffset, indentStrategy, null, null, padding, margin);
    }

    /**
     * Creates a paragraph without marker indentation.
     *
     * @param name        node name used in snapshots and layout graph paths
     * @param text        paragraph text
     * @param textStyle   paragraph style
     * @param align       horizontal alignment
     * @param lineSpacing extra spacing between wrapped lines
     * @param padding     inner padding
     * @param margin      outer margin
     */
    public ParagraphNode(String name,
                         String text,
                         DocumentTextStyle textStyle,
                         TextAlign align,
                         double lineSpacing,
                         DocumentInsets padding,
                         DocumentInsets margin) {
        this(name, text, textStyle, align, lineSpacing, "", DocumentTextIndent.NONE, null, null, padding, margin);
    }

    private static List<InlineRun> normalizeInlineRuns(List<InlineRun> runs) {
        if (runs == null || runs.isEmpty()) {
            return List.of();
        }
        List<InlineRun> normalized = new java.util.ArrayList<>(runs.size());
        for (InlineRun run : runs) {
            if (run == null) {
                continue;
            }
            if (run instanceof InlineTextRun textRun && textRun.text().isEmpty()) {
                continue;
            }
            if (run instanceof InlineHighlightRun highlight && highlight.text().isEmpty()) {
                continue;
            }
            normalized.add(run);
        }
        return List.copyOf(normalized);
    }

    /**
     * Returns inline text runs in source order, filtering out image runs.
     *
     * <p>Provided for callers that only consume textual content (e.g. the
     * DOCX semantic backend or text-only tests). Image / shape / SVG runs are
     * silently dropped; a highlight chip degrades to a plain text run (its
     * background is a PDF-only decoration) so the text survives. Use
     * {@link #inlineRuns()} to access the full mixed list.</p>
     *
     * @return inline text runs in source order
     */
    public List<InlineTextRun> inlineTextRuns() {
        if (inlineRuns.isEmpty()) {
            return List.of();
        }
        List<InlineTextRun> textRuns = new java.util.ArrayList<>(inlineRuns.size());
        for (InlineRun run : inlineRuns) {
            if (run instanceof InlineTextRun textRun) {
                textRuns.add(textRun);
            } else if (run instanceof InlineHighlightRun highlight) {
                // Collapse newlines to spaces to match how the PDF tokenizer
                // lowers a chip (it stays one line), so both text surfaces agree.
                String chipText = highlight.text().replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ');
                textRuns.add(new InlineTextRun(chipText, highlight.textStyle(), highlight.linkTarget()));
            }
        }
        return List.copyOf(textRuns);
    }

}


