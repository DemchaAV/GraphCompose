package com.demcha.compose.engine.render.pdf.helpers;

import com.demcha.compose.engine.components.content.shape.FillColor;
import com.demcha.compose.engine.components.content.shape.Side;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.render.pdf.PdfRenderingSystemECS;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.awt.*;
import java.io.IOException;
import java.util.Set;

/**
 * PDF-only helper that paints a rectangle-like table cell box with selective
 * border ownership.
 */
public final class TableCellBox {
    public boolean render(PDPageContentStream stream,
                          PdfRenderingSystemECS renderingSystem,
                          double x,
                          double y,
                          double width,
                          double height,
                          FillColor fillColor,
                          Stroke stroke,
                          Set<Side> sides) throws IOException {
        return render(stream, renderingSystem, x, y, width, height, fillColor, stroke, Padding.zero(), sides);
    }

    public boolean render(PDPageContentStream stream,
                          PdfRenderingSystemECS renderingSystem,
                          double x,
                          double y,
                          double width,
                          double height,
                          FillColor fillColor,
                          Stroke stroke,
                          Padding fillInsets,
                          Set<Side> sides) throws IOException {
        if (width <= 0 || height <= 0) {
            return false;
        }

        if (fillColor != null && fillColor.color() != null) {
            fillRectangle(stream, x, y, width, height, fillInsets == null ? Padding.zero() : fillInsets, fillColor.color());
        }

        if (stroke != null && stroke.width() > 0 && sides != null && !sides.isEmpty()) {
            RenderCoordinateContext context = new RenderCoordinateContext(
                    x,
                    y,
                    width,
                    height,
                    0,
                    0,
                    stroke,
                    stroke.strokeColor().color()
            );
            renderingSystem.renderBorder(stream, context, false, sides);
        }

        return true;
    }

    private void fillRectangle(PDPageContentStream stream,
                               double x,
                               double y,
                               double width,
                               double height,
                               Padding fillInsets,
                               Color color) throws IOException {
        double fillX = x + fillInsets.left();
        double fillY = y + fillInsets.bottom();
        double fillWidth = Math.max(0, width - fillInsets.horizontal());
        double fillHeight = Math.max(0, height - fillInsets.vertical());

        if (fillWidth <= 0 || fillHeight <= 0) {
            return;
        }

        stream.saveGraphicsState();
        try {
            stream.setNonStrokingColor(color);
            stream.addRect((float) fillX, (float) fillY, (float) fillWidth, (float) fillHeight);
            stream.fill();
        } finally {
            stream.restoreGraphicsState();
        }
    }
}
