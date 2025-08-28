package com.demcha.legacy.layout.layouts;

import com.demcha.components.containers.abstract_builders.Container;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.components.style.Margin;
import com.demcha.components.layout.Align;
import com.demcha.components.content.text.block.TextBlock;
import com.demcha.components.content.text.block.TextBlockMeasurer;
import com.demcha.legacy.components.data.text.TextData;
import com.demcha.legacy.core.Element;
import com.demcha.legacy.layout.*;

/**
 * A layout class that arranges child elements horizontally with a specified gap between them.
 * It supports different alignment options for the elements within the layout, such as top, center, baseline, and bottom alignment.
 *
 * <p>This layout is typically used for containers where the child elements are placed in a single row,
 * each separated by a gap, and aligned according to the specified alignment type.</p>
 *
 * <h2>Fields:</h2>
 * <ul>
 *     <li><strong>gap</strong> - The space between the child elements in the layout.</li>
 *     <li><strong>align</strong> - The alignment of the elements along the vertical axis (top, center, baseline, or bottom).</li>
 * </ul>
 *
 * <h2>Methods:</h2>
 * <h3>measure(Container c, MeasureCtx ctx)</h3>
 * <p>This method calculates the required width and height of the container based on its children.
 * It automatically adjusts the size if a child does not have a defined size and if it contains text data.</p>
 *
 * <h3>arrange(Container c, ArrangeCtx ctx)</h3>
 * <p>This method arranges the children of the container based on their calculated sizes and the specified alignment.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * HorizontalLayout layout = new HorizontalLayout(10, Align.CENTER);
 * layout.measure(container, measureCtx);
 * layout.arrange(container, arrangeCtx);
 * </pre>
 *
 * <h2>Supported Alignments:</h2>
 * <ul>
 *     <li><strong>TOP</strong> - Aligns children at the top of the container.</li>
 *     <li><strong>CENTER</strong> - Centers children vertically within the container.</li>
 *     <li><strong>BASELINE</strong> - Aligns children by their baseline.</li>
 *     <li><strong>BOTTOM</strong> - Aligns children at the bottom of the container.</li>
 * </ul>
 *
 * <p>This class makes use of the {@link OuterBoxSize}, {@link Position}, and {@link Margin} components
 * to handle positioning, sizing, and spacing of elements within the layout.</p>
 */
public class HorizontalLayout implements Layout {
    private final double gap;
    private final Align align;

    /**
     * Constructs a new HorizontalLayout.
     *
     * @param gap   The space between child elements in the layout.
     * @param align The vertical alignment of the child elements.
     */
    public HorizontalLayout(double gap, Align align) {
        this.gap = gap;
        this.align = align;
    }

    /**
     * Measures the required width and height for the container based on its child elements.
     * Automatically adjusts size if the child contains text data but no size defined.
     *
     * @param c   The container whose child elements are being measured.
     * @param ctx The context used for measurement, containing available space and other info.
     */
    @Override
    public void measure(Container c, MeasureCtx ctx) {
        double totalW = 0, maxH = 0;

        for (Element child : c.getChildren()) {
            // --- 1. Автоматическое измерение, если нет OuterBoxSize ---
            if (child.has(TextBlock.class)) {
                TextBlockMeasurer.ensureMeasured(child, ctx.availableWidth());
            } else if (child.has(TextData.class)) {
//                child.add(OuterBoxSize.textAutoSize(child)); // твоё старое поведение
            }

            // --- 2. Получаем размеры и отступы ---
            double w = child.get(OuterBoxSize.class).map(OuterBoxSize::width).orElse(0.0);
            double h = child.get(OuterBoxSize.class).map(OuterBoxSize::height).orElse(0.0);
            Margin m = child.get(Margin.class).orElse(Margin.zero());

            if (totalW > 0) totalW += gap;
            totalW += w + m.left() + m.right();
            maxH = Math.max(maxH, h + m.top() + m.bottom());
        }

        // --- 3. Записываем рассчитанный размер контейнера ---
        final double totalWfinal = totalW;
        final double maxHfinal = maxH;
        c.getElement().getOrAdd(OuterBoxSize.class, () -> new OuterBoxSize(totalWfinal, maxHfinal));
    }

    /**
     * Arranges the children of the container based on their calculated sizes and the specified alignment.
     *
     * @param c   The container whose child elements are being arranged.
     * @param ctx The context used for arranging, including the starting position and allocated space.
     */
    @Override
    public void arrange(Container c, ArrangeCtx ctx) {
        double x = ctx.startX();
        double topY = ctx.startY();
        double containerH = ctx.allocatedHeight() > 0
                ? ctx.allocatedHeight()
                : c.getElement().get(OuterBoxSize.class).map(OuterBoxSize::height).orElse(0.0);

        for (Element child : c.getChildren()) {
            double w = child.get(OuterBoxSize.class).map(OuterBoxSize::width).orElse(0.0);
            double h = child.get(OuterBoxSize.class).map(OuterBoxSize::height).orElse(0.0);
            Margin m = child.get(Margin.class).orElse(Margin.zero());

            // HorizontalLayout (cross-axis = vertical)
            double y = switch (align) {
                case TOP      -> topY - m.top();
                case CENTER   -> topY + (containerH - h) / 2.0;
                case BASELINE -> topY + (containerH - h) * 0.85;
                case BOTTOM   -> topY + (containerH - h) - m.bottom();
                default       -> topY; // LEFT/RIGHT ignored safely
            };


            // Устанавливаем позицию
            child.add(new Position(x + m.left(), y));
            x += m.left() + w + m.right() + gap;
        }
    }
}
