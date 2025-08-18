package com.demcha.legacy.layout.layouts;

import com.demcha.components.geometry.BoxSize;
import com.demcha.components.style.Margin;
import com.demcha.components.layout.Position;
import com.demcha.components.layout.Align;
import com.demcha.legacy.components.data.text.block.TextBlock;
import com.demcha.legacy.components.data.text.block.TextBlockMeasurer;
import com.demcha.legacy.components.data.text.TextData;
import com.demcha.legacy.core.Element;
import com.demcha.legacy.layout.*;

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
            if (!child.has(BoxSize.class)) {
                if (child.has(TextBlock.class)) {
                    TextBlockMeasurer.ensureMeasured(child, ctx.availableWidth());
                } else if (child.has(TextData.class)) {
                    child.add(BoxSize.textAutoSize(child));
                }
            }

            double w = child.get(BoxSize.class).map(BoxSize::width).orElse(0.0);
            double h = child.get(BoxSize.class).map(BoxSize::height).orElse(0.0);
            Margin m = child.get(Margin.class).orElse(Margin.zero());

            if (totalH > 0) totalH += gap;
            totalH += h + m.top() + m.bottom();
            maxW = Math.max(maxW, w + m.left() + m.right());
        }
        final var fMaxW = maxW;
        final var fTotalH = totalH;

        c.getElement().getOrAdd(BoxSize.class, () -> new BoxSize(fMaxW, fTotalH));
    }

    @Override
    public void arrange(Container c, ArrangeCtx ctx) {
        double x0 = ctx.startX();
        double y = ctx.startY(); // top-left система

        double containerW = ctx.allocatedWidth() > 0
                ? ctx.allocatedWidth()
                : c.getElement().get(BoxSize.class).map(BoxSize::width).orElse(0.0);

        for (Element child : c.getChildren()) {
            double w = child.get(BoxSize.class).map(BoxSize::width).orElse(0.0);
            double h = child.get(BoxSize.class).map(BoxSize::height).orElse(0.0);
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
