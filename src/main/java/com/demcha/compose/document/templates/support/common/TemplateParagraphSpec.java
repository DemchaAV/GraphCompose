package com.demcha.compose.document.templates.support.common;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfLinkOptions;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.engine.components.content.text.TextIndentStrategy;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

import java.util.List;

/**
 * Immutable paragraph instruction used by shared template scene composers.
 *
 * @param name semantic paragraph name used in snapshots
 * @param text paragraph text when inline runs are not supplied
 * @param inlineTextRuns optional styled inline runs in source order
 * @param style paragraph text style
 * @param align horizontal text alignment
 * @param lineSpacing extra space between wrapped lines
 * @param bulletOffset first-line prefix for list-like paragraph paths
 * @param indentStrategy hanging/first-line indent strategy
 * @param linkOptions optional paragraph link metadata
 * @param padding inner padding
 * @param margin outer margin
 * @author Artem Demchyshyn
 */
public record TemplateParagraphSpec(
        String name,
        String text,
        List<InlineTextRun> inlineTextRuns,
        TextStyle style,
        TextAlign align,
        double lineSpacing,
        String bulletOffset,
        TextIndentStrategy indentStrategy,
        PdfLinkOptions linkOptions,
        Padding padding,
        Margin margin
) {
    /**
     * Normalizes paragraph defaults for shared template composition.
     */
    public TemplateParagraphSpec {
        name = name == null ? "" : name;
        inlineTextRuns = inlineTextRuns == null ? List.of() : List.copyOf(inlineTextRuns);
        text = text == null ? "" : text;
        style = style == null ? TextStyle.DEFAULT_STYLE : style;
        align = align == null ? TextAlign.LEFT : align;
        bulletOffset = bulletOffset == null ? "" : bulletOffset;
        indentStrategy = indentStrategy == null ? TextIndentStrategy.NONE : indentStrategy;
        padding = padding == null ? Padding.zero() : padding;
        margin = margin == null ? Margin.zero() : margin;
    }

    /**
     * Creates a paragraph instruction from plain text.
     *
     * @param name semantic paragraph name
     * @param text paragraph text
     * @param style paragraph text style
     * @param align horizontal text alignment
     * @param lineSpacing extra line spacing
     * @param bulletOffset first-line marker or prefix
     * @param indentStrategy text indent strategy
     * @param linkOptions optional link metadata
     * @param padding inner padding
     * @param margin outer margin
     */
    public TemplateParagraphSpec(String name,
                                 String text,
                                 TextStyle style,
                                 TextAlign align,
                                 double lineSpacing,
                                 String bulletOffset,
                                 TextIndentStrategy indentStrategy,
                                 PdfLinkOptions linkOptions,
                                 Padding padding,
                                 Margin margin) {
        this(name, text, List.of(), style, align, lineSpacing, bulletOffset, indentStrategy, linkOptions, padding, margin);
    }
}
