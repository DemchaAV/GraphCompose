package com.demcha.compose.document.node;

import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextAutoSize;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.List;
import java.util.Objects;

/**
 * Semantic paragraph block that may split across pages.
 *
 * @param name node name used in snapshots and layout graph paths
 * @param text paragraph text when inline runs are not supplied
 * @param inlineTextRuns optional styled inline runs in source order
 * @param textStyle base paragraph text style
 * @param align horizontal text alignment
 * @param lineSpacing extra space between wrapped lines
 * @param bulletOffset first-line prefix used by list-style paragraph paths
 * @param indentStrategy hanging/first-line indent strategy
 * @param linkOptions optional node-level link metadata
 * @param bookmarkOptions optional node-level bookmark metadata
 * @param padding inner padding
 * @param margin outer margin
 * @author Artem Demchyshyn
 */
public record ParagraphNode(
        String name,
        String text,
        List<InlineTextRun> inlineTextRuns,
        DocumentTextStyle textStyle,
        TextAlign align,
        double lineSpacing,
        String bulletOffset,
        DocumentTextIndent indentStrategy,
        DocumentLinkOptions linkOptions,
        DocumentBookmarkOptions bookmarkOptions,
        DocumentInsets padding,
        DocumentInsets margin,
        DocumentTextAutoSize autoSize
) implements DocumentNode {
    /**
     * Normalizes optional text, inline runs, style, alignment, spacing, and
     * indentation defaults for a canonical paragraph.
     */
    public ParagraphNode {
        name = name == null ? "" : name;
        inlineTextRuns = normalizeInlineRuns(inlineTextRuns);
        text = Objects.requireNonNullElse(text, "");
        if (text.isBlank() && !inlineTextRuns.isEmpty()) {
            text = inlineTextRuns.stream()
                    .map(InlineTextRun::text)
                    .reduce("", String::concat);
        }
        textStyle = textStyle == null ? DocumentTextStyle.DEFAULT : textStyle;
        align = align == null ? TextAlign.LEFT : align;
        bulletOffset = Objects.requireNonNullElse(bulletOffset, "");
        indentStrategy = indentStrategy == null ? DocumentTextIndent.NONE : indentStrategy;
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
        if (lineSpacing < 0 || Double.isNaN(lineSpacing) || Double.isInfinite(lineSpacing)) {
            throw new IllegalArgumentException("lineSpacing must be finite and non-negative: " + lineSpacing);
        }
    }

    /**
     * Backwards-compatible 12-arg canonical constructor without auto-size.
     */
    public ParagraphNode(String name,
                         String text,
                         List<InlineTextRun> inlineTextRuns,
                         DocumentTextStyle textStyle,
                         TextAlign align,
                         double lineSpacing,
                         String bulletOffset,
                         DocumentTextIndent indentStrategy,
                         DocumentLinkOptions linkOptions,
                         DocumentBookmarkOptions bookmarkOptions,
                         DocumentInsets padding,
                         DocumentInsets margin) {
        this(name, text, inlineTextRuns, textStyle, align, lineSpacing, bulletOffset, indentStrategy,
                linkOptions, bookmarkOptions, padding, margin, null);
    }

    /**
     * Creates a paragraph from plain text with optional link and bookmark metadata.
     *
     * @param name node name used in snapshots and layout graph paths
     * @param text paragraph text
     * @param textStyle paragraph style
     * @param align horizontal alignment
     * @param lineSpacing extra spacing between wrapped lines
     * @param bulletOffset first-line marker or prefix
     * @param indentStrategy text indent strategy
     * @param linkOptions optional link metadata
     * @param bookmarkOptions optional bookmark metadata
     * @param padding inner padding
     * @param margin outer margin
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
     * @param name node name used in snapshots and layout graph paths
     * @param text paragraph text
     * @param textStyle paragraph style
     * @param align horizontal alignment
     * @param lineSpacing extra spacing between wrapped lines
     * @param bulletOffset first-line marker or prefix
     * @param indentStrategy text indent strategy
     * @param padding inner padding
     * @param margin outer margin
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
     * @param name node name used in snapshots and layout graph paths
     * @param text paragraph text
     * @param textStyle paragraph style
     * @param align horizontal alignment
     * @param lineSpacing extra spacing between wrapped lines
     * @param padding inner padding
     * @param margin outer margin
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

    private static List<InlineTextRun> normalizeInlineRuns(List<InlineTextRun> runs) {
        if (runs == null || runs.isEmpty()) {
            return List.of();
        }
        return runs.stream()
                .filter(Objects::nonNull)
                .filter(run -> !run.text().isEmpty())
                .toList();
    }
}


