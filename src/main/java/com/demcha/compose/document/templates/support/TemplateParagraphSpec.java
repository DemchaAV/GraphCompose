package com.demcha.compose.document.templates.support;

import com.demcha.compose.document.model.node.TextAlign;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

/**
 * Immutable paragraph instruction used by shared template scene composers.
 */
public record TemplateParagraphSpec(
        String name,
        String text,
        TextStyle style,
        TextAlign align,
        double lineSpacing,
        Padding padding,
        Margin margin
) {
    public TemplateParagraphSpec {
        name = name == null ? "" : name;
        text = text == null ? "" : text;
        style = style == null ? TextStyle.DEFAULT_STYLE : style;
        align = align == null ? TextAlign.LEFT : align;
        padding = padding == null ? Padding.zero() : padding;
        margin = margin == null ? Margin.zero() : margin;
    }
}
