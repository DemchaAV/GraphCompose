package com.demcha.components.content.text.block;

import com.demcha.components.geometry.BoxSize;
import com.demcha.components.content.text.TextLines;

public final class TextBlockMeasurer {
    private TextBlockMeasurer() {}

    //TODO переделать для нашего в коменте рабочий код
//    public static void ensureMeasured(Element e, double availableWidth)  {
//        var tbOpt = e.get(TextBlock.class);
//        if (tbOpt.isEmpty()) return;
//
//        var tb = tbOpt.get();
//        var style = tb.style();
//        var font = style.font();
//        float fontSize = style.size();
//        double wrap = tb.wrapWidth() > 0 ? tb.wrapWidth() : availableWidth;
//
//        TextLines lines = TextBreaker.breakIntoLines(tb.text(), font, fontSize, wrap, tb.lineSpacingFactor());
//        // ширина элемента = либо wrap, либо фактическая максимальная ширина строк
//        double width = Math.min(wrap, Math.max(lines.maxLineWidth(), (float) wrap));
//        double height = lines.lines().size() * lines.lineHeight();
//
//        e.add(lines);
//        e.add(new BoxSize(width, height));
//    }
}

