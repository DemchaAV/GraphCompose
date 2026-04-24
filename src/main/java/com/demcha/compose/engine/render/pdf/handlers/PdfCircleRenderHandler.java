package com.demcha.compose.engine.render.pdf.handlers;

import com.demcha.compose.engine.components.content.shape.FillColor;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.components.renderable.Circle;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.render.pdf.PdfRenderingSystemECS;
import com.demcha.compose.engine.render.guides.GuidesRenderer;
import com.demcha.compose.engine.render.RenderHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.EnumSet;

@Slf4j
public final class PdfCircleRenderHandler implements RenderHandler<Circle, PdfRenderingSystemECS> {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING, GuidesRenderer.Guide.BOX);
    private static final float BEZIER_CIRCLE = 0.552284749831f;

    @Override
    public Class<Circle> renderType() {
        return Circle.class;
    }

    @Override
    public boolean render(EntityManager manager,
                          Entity entity,
                          Circle renderComponent,
                          PdfRenderingSystemECS renderingSystem,
                          boolean guideLines) throws IOException {
        Placement placement = entity.getComponent(Placement.class).orElse(null);
        if (placement == null) {
            log.warn("Skipping circle render because Placement is missing for {}", entity);
            return false;
        }

        Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());
        double x = placement.x() + padding.left();
        double y = placement.y() + padding.bottom();
        double width = Math.max(0.0, placement.width() - padding.horizontal());
        double height = Math.max(0.0, placement.height() - padding.vertical());

        PDPageContentStream stream = renderingSystem.pageSurface(entity);
        boolean drawn = renderCircle(stream, entity, x, y, width, height);
        if (guideLines) {
            renderingSystem.guidesRenderer().guidesRender(entity, stream, DEFAULT_GUIDES);
        }
        return drawn;
    }

    private boolean renderCircle(PDPageContentStream stream,
                                 Entity entity,
                                 double x,
                                 double y,
                                 double width,
                                 double height) throws IOException {
        FillColor fillColor = entity.getComponent(FillColor.class).orElse(FillColor.defaultColor());
        Stroke stroke = entity.getComponent(Stroke.class).orElse(null);

        if (width <= 0 || height <= 0) {
            log.debug("Skip circle: non-positive size ({}, {})", width, height);
            return false;
        }

        boolean hasFill = fillColor != null && fillColor.color() != null;
        boolean hasStroke = stroke != null && stroke.strokeColor() != null && stroke.width() > 0;

        if (!hasFill && !hasStroke) {
            log.debug("Skip circle: neither fill nor stroke specified.");
            return false;
        }

        double diameter = Math.min(width, height);
        double offsetX = x + (width - diameter) / 2.0;
        double offsetY = y + (height - diameter) / 2.0;

        float fx = (float) offsetX;
        float fy = (float) offsetY;
        float radius = (float) (diameter / 2.0);
        float centerX = fx + radius;
        float centerY = fy + radius;
        float control = radius * BEZIER_CIRCLE;

        stream.saveGraphicsState();
        try {
            if (hasStroke) {
                stream.setLineWidth((float) stroke.width());
                stream.setStrokingColor(stroke.strokeColor().color());
            }

            if (hasFill) {
                stream.setNonStrokingColor(fillColor.color());
            }

            stream.moveTo(centerX + radius, centerY);
            stream.curveTo(centerX + radius, centerY + control,
                    centerX + control, centerY + radius,
                    centerX, centerY + radius);
            stream.curveTo(centerX - control, centerY + radius,
                    centerX - radius, centerY + control,
                    centerX - radius, centerY);
            stream.curveTo(centerX - radius, centerY - control,
                    centerX - control, centerY - radius,
                    centerX, centerY - radius);
            stream.curveTo(centerX + control, centerY - radius,
                    centerX + radius, centerY - control,
                    centerX + radius, centerY);
            stream.closePath();

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
}
