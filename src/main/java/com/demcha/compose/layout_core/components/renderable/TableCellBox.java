package com.demcha.compose.layout_core.components.renderable;

import com.demcha.compose.layout_core.components.content.shape.BorderSides;
import com.demcha.compose.layout_core.components.content.shape.FillColor;
import com.demcha.compose.layout_core.components.content.shape.Side;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRender;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import com.demcha.compose.layout_core.system.interfaces.guides.GuidesRenderer;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.awt.*;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

/**
 * Rectangle-like box with selective border ownership used by table cells.
 */
public class TableCellBox implements PdfRender {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING);

    @Override
    public boolean pdf(EntityManager manager, Entity e, PdfRenderingSystemECS renderingSystem, boolean guideLines) throws IOException {
        if (!e.hasAssignable(TableCellBox.class)) {
            return false;
        }

        Placement placement = e.getComponent(Placement.class).orElseThrow();
        FillColor fillColor = e.getComponent(FillColor.class).orElse(null);
        Stroke stroke = e.getComponent(Stroke.class).orElse(null);
        Set<Side> sides = e.getComponent(BorderSides.class).map(BorderSides::sides).orElse(Set.of(Side.ALL));

        try (PDPageContentStream stream = renderingSystem.stream().openContentStream(e)) {
            render(stream, renderingSystem, placement.x(), placement.y(), placement.width(), placement.height(), fillColor, stroke, sides);
            if (guideLines) {
                renderingSystem.guidesRenderer().guidesRender(e, stream, DEFAULT_GUIDES);
            }
        }
        return true;
    }

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
