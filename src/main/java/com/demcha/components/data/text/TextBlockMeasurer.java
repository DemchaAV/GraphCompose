package com.demcha.components.data.text;

import com.demcha.components.Size;
import com.demcha.core.Element;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;

public final class TextBlockMeasurer {
    private TextBlockMeasurer() {}

    public static void ensureMeasured(Element e, double availableWidth)  {
        var tbOpt = e.get(TextBlock.class);
        if (tbOpt.isEmpty()) return;

        var tb = tbOpt.get();
        var style = tb.style();
        var font = style.font();
        float fontSize = style.size();
        double wrap = tb.wrapWidth() > 0 ? tb.wrapWidth() : availableWidth;

        TextLines lines = TextBreaker.breakIntoLines(tb.text(), font, fontSize, wrap, tb.lineSpacingFactor());
        // ширина элемента = либо wrap, либо фактическая максимальная ширина строк
        double width = Math.min(wrap, Math.max(lines.maxLineWidth(), (float) wrap));
        double height = lines.lines().size() * lines.lineHeight();

        e.add(lines);
        e.add(new Size(width, height));
    }
}

