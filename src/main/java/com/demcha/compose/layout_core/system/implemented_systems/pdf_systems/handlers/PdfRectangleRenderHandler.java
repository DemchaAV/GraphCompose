package com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers;

import com.demcha.compose.layout_core.components.content.shape.CornerRadius;
import com.demcha.compose.layout_core.components.content.shape.FillColor;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.renderable.Rectangle;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.exceptions.ContentSizeNotFoundException;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import com.demcha.compose.layout_core.system.interfaces.guides.GuidesRenderer;
import com.demcha.compose.layout_core.system.rendering.RenderHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.EnumSet;

@Slf4j
public final class PdfRectangleRenderHandler implements RenderHandler<Rectangle, PdfRenderingSystemECS> {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING);

    @Override
    public Class<Rectangle> renderType() {
        return Rectangle.class;
    }

    @Override
    public boolean render(EntityManager manager,
                          Entity entity,
                          Rectangle renderComponent,
                          PdfRenderingSystemECS renderingSystem,
                          boolean guideLines) throws IOException {
        PDPageContentStream stream = renderingSystem.pageSurface(entity);
        boolean drawn = renderRectangle(entity, stream);
        if (guideLines) {
            renderingSystem.guidesRenderer().guidesRender(entity, stream, DEFAULT_GUIDES);
        }
        return drawn;
    }

    private boolean renderRectangle(Entity entity, PDPageContentStream stream) throws IOException {
        entity.getComponent(ContentSize.class)
                .orElseThrow(ContentSizeNotFoundException::new);

        Placement placement = entity.getComponent(Placement.class).orElseThrow();
        FillColor fillColor = entity.getComponent(FillColor.class).orElse(FillColor.defaultColor());
        Stroke stroke = entity.getComponent(Stroke.class).orElse(null);
        CornerRadius radius = entity.getComponent(CornerRadius.class).orElse(null);

        return drawRectangle(stream, placement.x(), placement.y(), stroke, fillColor, radius, placement.width(), placement.height());
    }

    private boolean drawRectangle(PDPageContentStream stream,
                                  double x,
                                  double y,
                                  Stroke stroke,
                                  FillColor fillColor,
                                  CornerRadius radius,
                                  double width,
                                  double height) throws IOException {
        if (width <= 0 || height <= 0) {
            log.debug("Skip rectangle: non-positive size ({}, {})", width, height);
            return false;
        }

        float fx = (float) x;
        float fy = (float) y;
        float fw = (float) width;
        float fh = (float) height;
        float clampedRadius = radius == null ? 0f : (float) Math.min(radius.radius(), Math.min(width, height) / 2.0);

        boolean hasFill = fillColor != null && fillColor.color() != null;
        boolean hasStroke = stroke != null && stroke.strokeColor() != null && stroke.width() > 0;

        if (!hasFill && !hasStroke) {
            log.debug("Skip rectangle: neither fill nor stroke specified.");
            return false;
        }

        stream.saveGraphicsState();
        try {
            if (hasStroke) {
                stream.setLineWidth((float) stroke.width());
                stream.setStrokingColor(stroke.strokeColor().color());
            }

            if (hasFill) {
                stream.setNonStrokingColor(fillColor.color());
            }

            if (clampedRadius > 0f) {
                drawRoundedRectangle(stream, fx, fy, fw, fh, clampedRadius);
            } else {
                stream.addRect(fx, fy, fw, fh);
            }

            if (hasFill && hasStroke) {
                stream.fillAndStroke();
            } else if (hasFill) {
                stream.fill();
            } else {
                stream.stroke();
            }
        } finally {
            stream.restoreGraphicsState();
        }

        return true;
    }

    private void drawRoundedRectangle(PDPageContentStream stream,
                                      float x,
                                      float y,
                                      float width,
                                      float height,
                                      float radius) throws IOException {
        float c = 0.552284749831f;
        stream.moveTo(x + radius, y + height);
        stream.lineTo(x + width - radius, y + height);
        stream.curveTo(x + width - radius + radius * c, y + height,
                x + width, y + height - radius + radius * c,
                x + width, y + height - radius);
        stream.lineTo(x + width, y + radius);
        stream.curveTo(x + width, y + radius - radius * c,
                x + width - radius + radius * c, y,
                x + width - radius, y);
        stream.lineTo(x + radius, y);
        stream.curveTo(x + radius - radius * c, y,
                x, y + radius - radius * c,
                x, y + radius);
        stream.lineTo(x, y + height - radius);
        stream.curveTo(x, y + height - radius + radius * c,
                x + radius - radius * c, y + height,
                x + radius, y + height);
        stream.closePath();
    }
}
