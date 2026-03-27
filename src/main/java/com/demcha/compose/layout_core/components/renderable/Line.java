package com.demcha.compose.layout_core.components.renderable;

import com.demcha.compose.layout_core.components.content.shape.LinePath;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.style.Padding;
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
public class Line implements PdfRender {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING, GuidesRenderer.Guide.BOX);

    @Override
    public boolean pdf(EntityManager manager, Entity e, PdfRenderingSystemECS renderingSystemECS, boolean guideLines)
            throws IOException {
        Placement placement = e.getComponent(Placement.class).orElse(null);
        if (placement == null) {
            log.warn("Skipping line render because Placement is missing for {}", e);
            return false;
        }

        Padding padding = e.getComponent(Padding.class).orElse(Padding.zero());
        double x = placement.x() + padding.left();
        double y = placement.y() + padding.bottom();
        double width = Math.max(0.0, placement.width() - padding.horizontal());
        double height = Math.max(0.0, placement.height() - padding.vertical());

        boolean drawn;
        try (PDPageContentStream cs = renderingSystemECS.stream().openContentStream(e)) {
            drawn = pdfRenderObject(e, cs, x, y, width, height);
            if (guideLines) {
                renderingSystemECS.guidesRenderer().guidesRender(e, cs, DEFAULT_GUIDES);
            }
        }
        return drawn;
    }

    private boolean pdfRenderObject(Entity e,
                                    PDPageContentStream cs,
                                    double x,
                                    double y,
                                    double width,
                                    double height) throws IOException {
        if (!e.hasAssignable(Line.class)) {
            log.debug("No Line on {}", e);
            return false;
        }

        Stroke stroke = e.getComponent(Stroke.class).orElse(new Stroke());
        LinePath path = e.getComponent(LinePath.class).orElse(LinePath.horizontal());

        if (stroke.strokeColor() == null || stroke.strokeColor().color() == null || stroke.width() <= 0) {
            log.debug("Skip line: stroke is missing or invalid on {}", e);
            return false;
        }

        double startX = x + width * path.startX();
        double startY = y + height * path.startY();
        double endX = x + width * path.endX();
        double endY = y + height * path.endY();

        if (Double.compare(startX, endX) == 0 && Double.compare(startY, endY) == 0) {
            log.debug("Skip line: degenerate path for {}", e);
            return false;
        }

        cs.saveGraphicsState();
        try {
            cs.setLineWidth((float) stroke.width());
            cs.setStrokingColor(stroke.strokeColor().color());
            cs.moveTo((float) startX, (float) startY);
            cs.lineTo((float) endX, (float) endY);
            cs.stroke();
        } finally {
            cs.restoreGraphicsState();
        }

        return true;
    }
}
