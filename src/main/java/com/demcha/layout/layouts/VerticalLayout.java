package com.demcha.layout.layouts;

import com.demcha.components.Margin;
import com.demcha.components.Position;
import com.demcha.components.Size;
import com.demcha.components.data.text.block.TextBlock;
import com.demcha.components.data.text.block.TextBlockMeasurer;
import com.demcha.components.data.text.TextData;
import com.demcha.core.Element;
import com.demcha.layout.*;

public class VerticalLayout implements Layout {
    private final double gap;
    private final Align align;

    public VerticalLayout(double gap, Align align) {
        this.gap = gap;
        this.align = align;
    }

    @Override
    public void measure(Container c, MeasureCtx ctx) {
        double totalH = 0, maxW = 0;

        for (Element child : c.getChildren()) {
            if (!child.has(Size.class)) {
                if (child.has(TextBlock.class)) {
                    TextBlockMeasurer.ensureMeasured(child, ctx.availableWidth());
                } else if (child.has(TextData.class)) {
                    child.add(Size.textAutoSize(child));
                }
            }

            double w = child.get(Size.class).map(Size::width).orElse(0.0);
            double h = child.get(Size.class).map(Size::height).orElse(0.0);
            Margin m = child.get(Margin.class).orElse(Margin.zero());

            if (totalH > 0) totalH += gap;
            totalH += h + m.top() + m.bottom();
            maxW = Math.max(maxW, w + m.left() + m.right());
        }
        final var fMaxW = maxW;
        final var fTotalH = totalH;

        c.getElement().getOrAdd(Size.class, () -> new Size(fMaxW, fTotalH));
    }

    @Override
    public void arrange(Container c, ArrangeCtx ctx) {
        double x0 = ctx.startX();
        double y = ctx.startY(); // top-left система

        double containerW = ctx.allocatedWidth() > 0
                ? ctx.allocatedWidth()
                : c.getElement().get(Size.class).map(Size::width).orElse(0.0);

        for (Element child : c.getChildren()) {
            double w = child.get(Size.class).map(Size::width).orElse(0.0);
            double h = child.get(Size.class).map(Size::height).orElse(0.0);
            Margin m = child.get(Margin.class).orElse(Margin.zero());

            // по горизонтали (cross-axis) — LEFT/CENTER/RIGHT
            double x = switch (align) {
                case RIGHT  -> x0 + containerW - w - m.right();
                case CENTER -> x0 + (containerW - w) / 2.0;
                default     -> x0 + m.left(); // LEFT (и остальные игнор)
            };

            // ставим TOP-LEFT позицию
            child.add(new Position(x, y - m.top()));

            // двигаем "курсор" вниз
            y -= (m.top() + h + m.bottom() + gap);
        }
    }
}
