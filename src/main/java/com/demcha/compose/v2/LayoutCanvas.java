package com.demcha.compose.v2;

import com.demcha.compose.layout_core.components.style.Margin;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

/**
 * Page geometry used by the v2 compiler and backends.
 */
public record LayoutCanvas(double width, double height, double innerWidth, double innerHeight, Margin margin) {

    public static LayoutCanvas from(PDRectangle pageSize, Margin margin) {
        Margin safeMargin = margin == null ? Margin.zero() : margin;
        double width = pageSize.getWidth();
        double height = pageSize.getHeight();
        return new LayoutCanvas(
                width,
                height,
                Math.max(0.0, width - safeMargin.horizontal()),
                Math.max(0.0, height - safeMargin.vertical()),
                safeMargin);
    }
}
