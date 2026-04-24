package com.demcha.compose.engine.render.pdf.handlers;

import com.demcha.compose.engine.components.content.shape.LinePath;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.components.renderable.Line;
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
public final class PdfLineRenderHandler implements RenderHandler<Line, PdfRenderingSystemECS> {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING, GuidesRenderer.Guide.BOX);

    @Override
    public Class<Line> renderType() {
        return Line.class;
    }

    @Override
    public boolean render(EntityManager manager,
                          Entity entity,
                          Line renderComponent,
                          PdfRenderingSystemECS renderingSystem,
                          boolean guideLines) throws IOException {
        Placement placement = entity.getComponent(Placement.class).orElse(null);
        if (placement == null) {
            log.warn("Skipping line render because Placement is missing for {}", entity);
            return false;
        }

        Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());
        double x = placement.x() + padding.left();
        double y = placement.y() + padding.bottom();
        double width = Math.max(0.0, placement.width() - padding.horizontal());
        double height = Math.max(0.0, placement.height() - padding.vertical());

        PDPageContentStream stream = renderingSystem.pageSurface(entity);
        boolean drawn = renderLine(stream, entity, x, y, width, height);
        if (guideLines) {
            renderingSystem.guidesRenderer().guidesRender(entity, stream, DEFAULT_GUIDES);
        }
        return drawn;
    }

    private boolean renderLine(PDPageContentStream stream,
                               Entity entity,
                               double x,
                               double y,
                               double width,
                               double height) throws IOException {
        Stroke stroke = entity.getComponent(Stroke.class).orElse(new Stroke());
        LinePath path = entity.getComponent(LinePath.class).orElse(LinePath.horizontal());

        if (stroke.strokeColor() == null || stroke.strokeColor().color() == null || stroke.width() <= 0) {
            log.debug("Skip line: stroke is missing or invalid on {}", entity);
            return false;
        }

        double startX = x + width * path.startX();
        double startY = y + height * path.startY();
        double endX = x + width * path.endX();
        double endY = y + height * path.endY();

        if (Double.compare(startX, endX) == 0 && Double.compare(startY, endY) == 0) {
            log.debug("Skip line: degenerate path for {}", entity);
            return false;
        }

        stream.saveGraphicsState();
        try {
            stream.setLineWidth((float) stroke.width());
            stream.setStrokingColor(stroke.strokeColor().color());
            stream.moveTo((float) startX, (float) startY);
            stream.lineTo((float) endX, (float) endY);
            stream.stroke();
        } finally {
            stream.restoreGraphicsState();
        }

        return true;
    }
}
