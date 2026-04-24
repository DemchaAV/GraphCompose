package com.demcha.compose.document.node;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfBookmarkOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfLinkOptions;
import com.demcha.compose.engine.components.content.text.TextIndentStrategy;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

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
        TextStyle textStyle,
        TextAlign align,
        double lineSpacing,
        String bulletOffset,
        TextIndentStrategy indentStrategy,
        PdfLinkOptions linkOptions,
        PdfBookmarkOptions bookmarkOptions,
        Padding padding,
        Margin margin
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
        textStyle = textStyle == null ? TextStyle.DEFAULT_STYLE : textStyle;
        align = align == null ? TextAlign.LEFT : align;
        bulletOffset = Objects.requireNonNullElse(bulletOffset, "");
        indentStrategy = indentStrategy == null ? TextIndentStrategy.NONE : indentStrategy;
        padding = padding == null ? Padding.zero() : padding;
        margin = margin == null ? Margin.zero() : margin;
        if (lineSpacing < 0 || Double.isNaN(lineSpacing) || Double.isInfinite(lineSpacing)) {
            throw new IllegalArgumentException("lineSpacing must be finite and non-negative: " + lineSpacing);
        }
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
                         TextStyle textStyle,
                         TextAlign align,
                         double lineSpacing,
                         String bulletOffset,
                         TextIndentStrategy indentStrategy,
                         PdfLinkOptions linkOptions,
                         PdfBookmarkOptions bookmarkOptions,
                         Padding padding,
                         Margin margin) {
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
                         TextStyle textStyle,
                         TextAlign align,
                         double lineSpacing,
                         String bulletOffset,
                         TextIndentStrategy indentStrategy,
                         Padding padding,
                         Margin margin) {
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
                         TextStyle textStyle,
                         TextAlign align,
                         double lineSpacing,
                         Padding padding,
                         Margin margin) {
        this(name, text, textStyle, align, lineSpacing, "", TextIndentStrategy.NONE, null, null, padding, margin);
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


