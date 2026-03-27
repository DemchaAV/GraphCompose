package com.demcha.compose.layout_core.components.renderable;

import com.demcha.compose.layout_core.components.content.shape.FillColor;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRender;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import com.demcha.compose.layout_core.system.interfaces.guides.GuidesRenderer;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.EnumSet;

@Slf4j
@EqualsAndHashCode
public class Circle implements PdfRender {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING);

    private static final float BEZIER_CIRCLE = 0.552284749831f;

    @Override
    public boolean pdf(EntityManager manager, Entity e, PdfRenderingSystemECS renderingSystemECS, boolean guideLines)
            throws IOException {
        boolean drawn;
        try (PDPageContentStream cs = renderingSystemECS.stream().openContentStream(e)) {
            drawn = pdfRenderObject(e, cs);
            if (guideLines) {
                renderingSystemECS.guidesRenderer().guidesRender(e, cs, DEFAULT_GUIDES);
            }
        }
        return drawn;
    }

    private boolean pdfRenderObject(Entity e, PDPageContentStream cs) throws IOException {
        if (!e.hasAssignable(Circle.class)) {
            log.debug("No Circle on {}", e);
            return false;
        }

        Placement placement = e.getComponent(Placement.class).orElseThrow();
        FillColor fillColor = e.getComponent(FillColor.class).orElse(FillColor.defaultColor());
        Stroke stroke = e.getComponent(Stroke.class).orElse(null);

        return pdfRenderCircle(
                cs,
                placement.x(),
                placement.y(),
                placement.width(),
                placement.height(),
                stroke,
                fillColor);
    }

    private boolean pdfRenderCircle(PDPageContentStream cs,
                                    double x,
                                    double y,
                                    double width,
                                    double height,
                                    Stroke stroke,
                                    FillColor fillColor) throws IOException {
        if (width <= 0 || height <= 0) {
            log.debug("Skip circle: non-positive size ({}, {})", width, height);
            return false;
        }

        final boolean hasFill = fillColor != null && fillColor.color() != null;
        final boolean hasStroke = stroke != null && stroke.strokeColor() != null && stroke.width() > 0;

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

        cs.saveGraphicsState();
        try {
            if (hasStroke) {
                cs.setLineWidth((float) stroke.width());
                cs.setStrokingColor(stroke.strokeColor().color());
            }

            if (hasFill) {
                cs.setNonStrokingColor(fillColor.color());
            }

            cs.moveTo(centerX + radius, centerY);
            cs.curveTo(centerX + radius, centerY + control,
                    centerX + control, centerY + radius,
                    centerX, centerY + radius);
            cs.curveTo(centerX - control, centerY + radius,
                    centerX - radius, centerY + control,
                    centerX - radius, centerY);
            cs.curveTo(centerX - radius, centerY - control,
                    centerX - control, centerY - radius,
                    centerX, centerY - radius);
            cs.curveTo(centerX + control, centerY - radius,
                    centerX + radius, centerY - control,
                    centerX + radius, centerY);
            cs.closePath();

            if (hasFill && hasStroke) {
                cs.fillAndStroke();
            } else if (hasFill) {
                cs.fill();
            } else {
                cs.stroke();
            }
        } finally {
            cs.restoreGraphicsState();
        }
        return true;
    }
}
