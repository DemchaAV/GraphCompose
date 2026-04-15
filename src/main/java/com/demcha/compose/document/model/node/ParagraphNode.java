package com.demcha.compose.document.model.node;

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
        Padding padding,
        Margin margin
) implements DocumentNode {
    public ParagraphNode {
        name = name == null ? "" : name;
        text = Objects.requireNonNullElse(text, "");
        textStyle = textStyle == null ? TextStyle.DEFAULT_STYLE : textStyle;
        align = align == null ? TextAlign.LEFT : align;
        padding = padding == null ? Padding.zero() : padding;
        margin = margin == null ? Margin.zero() : margin;
        if (lineSpacing < 0 || Double.isNaN(lineSpacing) || Double.isInfinite(lineSpacing)) {
            throw new IllegalArgumentException("lineSpacing must be finite and non-negative: " + lineSpacing);
        }
    }
}


