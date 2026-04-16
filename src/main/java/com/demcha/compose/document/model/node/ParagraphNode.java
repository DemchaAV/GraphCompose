package com.demcha.compose.document.model.node;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfBookmarkOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfLinkOptions;
import com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.document.model.node.DocumentNode;

import java.util.Objects;

/**
 * Semantic paragraph block that may split across pages.
 */
public record ParagraphNode(
        String name,
        String text,
        TextStyle textStyle,
        TextAlign align,
        double lineSpacing,
        String bulletOffset,
        BlockIndentStrategy indentStrategy,
        PdfLinkOptions linkOptions,
        PdfBookmarkOptions bookmarkOptions,
        Padding padding,
        Margin margin
) implements DocumentNode {
    public ParagraphNode {
        name = name == null ? "" : name;
        text = Objects.requireNonNullElse(text, "");
        textStyle = textStyle == null ? TextStyle.DEFAULT_STYLE : textStyle;
        align = align == null ? TextAlign.LEFT : align;
        bulletOffset = Objects.requireNonNullElse(bulletOffset, "");
        indentStrategy = indentStrategy == null ? BlockIndentStrategy.NONE : indentStrategy;
        padding = padding == null ? Padding.zero() : padding;
        margin = margin == null ? Margin.zero() : margin;
        if (lineSpacing < 0 || Double.isNaN(lineSpacing) || Double.isInfinite(lineSpacing)) {
            throw new IllegalArgumentException("lineSpacing must be finite and non-negative: " + lineSpacing);
        }
    }

    public ParagraphNode(String name,
                         String text,
                         TextStyle textStyle,
                         TextAlign align,
                         double lineSpacing,
                         String bulletOffset,
                         BlockIndentStrategy indentStrategy,
                         Padding padding,
                         Margin margin) {
        this(name, text, textStyle, align, lineSpacing, bulletOffset, indentStrategy, null, null, padding, margin);
    }

    public ParagraphNode(String name,
                         String text,
                         TextStyle textStyle,
                         TextAlign align,
                         double lineSpacing,
                         Padding padding,
                         Margin margin) {
        this(name, text, textStyle, align, lineSpacing, "", BlockIndentStrategy.NONE, null, null, padding, margin);
    }
}


