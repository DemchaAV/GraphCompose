package com.demcha.system.implemented_systems.pdf_systems;

import com.demcha.components.components_builders.Canvas;
import com.demcha.components.content.shape.Side;
import com.demcha.components.content.shape.Stroke;
import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.core.EntityManager;
import com.demcha.system.GuidLineSettings;
import com.demcha.system.implemented_systems.RenderingSystemBase;
import com.demcha.system.interfaces.guides.GuidesRenderer;
import com.demcha.system.utils.page_breaker.EntitySorter;
import com.demcha.system.utils.page_breaker.PageBreaker;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * In current Class you can render a simple Figure like line rectangle cercl
 */
@Slf4j
@Getter
@Accessors(fluent = true)
public class PdfRenderingSystemECS extends RenderingSystemBase<PDPageContentStream> {
    private final PDDocument doc;

    public PdfRenderingSystemECS(PDDocument doc, Canvas canvas) {
        super(
                canvas,
                new GuidLineSettings(),
                new PdfStream(doc, canvas)
        );
        this.doc = doc;
        guidesRendererInitializer(new PdfGuidesRenderer(this));

    }


    @Override
    public void process(EntityManager entityManager) {
        log.info("Processing PdfRenderingSystemECS");

        var entities = entityManager.getLayers();

        for (Map.Entry<Integer, List<UUID>> e : entities.entrySet()) {
            var entitiesUuid = e.getValue();
            LinkedHashMap<UUID, Entity> uuidEntityLinkedHashMap = EntitySorter.sortByYPositionToMap(entityManager, entitiesUuid);


            uuidEntityLinkedHashMap.forEach((uuid, entity) -> {
                if (entity.hasRender()) {
                    if (PdfRender.class.isAssignableFrom(entity.getRender().getClass())) {
                        PdfRender render = (PdfRender) entity.getRender();
                        var guideLines = entity.isGuideLines();

                        try {
                            render.pdf(entity, this, guideLines);
                        } catch (IOException ex) {
                            log.error("Error processing pdf {}", ex, entity);
                            throw new RuntimeException(ex);
                        }

                    } else {
                        log.error("CurrentRender is not supported, Has to be PdfRender.class");
                    }


                } else {
                    log.error("{} has no PdfRender", entity);
                }
            });

        }

    }

    /**
     * @param guidesRenderer
     */
    @Override
    protected void guidesRendererInitializer(GuidesRenderer<PDPageContentStream> guidesRenderer) {
        guidesRenderer(guidesRenderer);
    }


    public Canvas pageSize(int pageIndex) {
        float width = doc.getPage(pageIndex).getMediaBox().getWidth();
        float height = doc.getPage(pageIndex).getMediaBox().getHeight();
        float x = doc.getPage(pageIndex).getMediaBox().getLowerLeftX();
        float y = doc.getPage(pageIndex).getMediaBox().getLowerLeftY();
        return new PdfCanvas(width, height, x, y);
    }

    @Override
    public Canvas canvas() {
        return canvas;
    }

    public boolean renderRectangle(Stroke stroke,
                                   PDPageContentStream cs,
                                   double x, double y,
                                   double w, double h,
                                   @NonNull Color color, boolean lineDash) throws IOException {

        if (w <= 0 || h <= 0) return false;

        cs.saveGraphicsState();
        try {
            PDExtendedGraphicsState gState = new PDExtendedGraphicsState();
            //  Using constant for opacity
            gState.setStrokingAlphaConstant(guidLineSettings.GUIDES_OPACITY());
            cs.setGraphicsStateParameters(gState);

            if (lineDash) {
                cs.setLineDashPattern(new float[]{3f}, 0);
            }
            if (stroke != null) cs.setLineWidth((float) stroke.width());

            cs.setStrokingColor(color);
            cs.addRect((float) x, (float) y, (float) w, (float) h);
            cs.stroke();
        } finally {
            cs.restoreGraphicsState();
        }
        return true;
    }

    @Override
    public void fillCircle(PDPageContentStream cs, float cx, float cy, float r, Color fill) throws IOException {
        if (r <= 0) return;

        final float k = 0.552284749831f;

        cs.saveGraphicsState();
        try {
            PDExtendedGraphicsState gState = new PDExtendedGraphicsState();
            //  Using constant for opacity
            gState.setNonStrokingAlphaConstant(guidLineSettings.GUIDES_OPACITY());
            cs.setGraphicsStateParameters(gState);

            cs.setNonStrokingColor(fill);
            cs.moveTo(cx + r, cy);
            cs.curveTo(cx + r, cy + k * r, cx + k * r, cy + r, cx, cy + r);
            cs.curveTo(cx - k * r, cy + r, cx - r, cy + k * r, cx - r, cy);
            cs.curveTo(cx - r, cy - k * r, cx - k * r, cy - r, cx, cy - r);
            cs.curveTo(cx + k * r, cy - r, cx + r, cy - k * r, cx + r, cy);
            cs.closePath();
            cs.fill();
        } finally {
            cs.restoreGraphicsState();
        }
    }


    @Override
    public boolean renderBorder(PDPageContentStream stream, RenderCoordinateContext context, boolean lineDash, Set<Side> sides) throws IOException {
        return renderBorder(stream, context.x(), context.y(), context.width(), context.height(), context.stroke(), context.color(), lineDash, sides);
    }

    @Override
    public boolean renderRectangle(PDPageContentStream stream, RenderCoordinateContext context, boolean lineDash) throws IOException {
        return renderRectangle(context.stroke(), stream, context.x(), context.y(), context.width(), context.height(), context.color(), lineDash);
    }

    public boolean renderBorder(PDPageContentStream stream, double x, double y, double w, double h, Stroke stroke,
                                Color color, boolean lineDash,
                                Set<Side> sides) throws IOException { // Changed argument

        // 1. Validation
        if (w <= 0 || h <= 0 || sides == null || sides.isEmpty()) return false;

        stream.saveGraphicsState();
        try {
            // 2. Graphics Setup (Opacity, Dash, Width, Color)
            var gState = new PDExtendedGraphicsState();
            // Assuming guidLineSettings is available in context, otherwise pass it or hardcode
            gState.setStrokingAlphaConstant(1.0f); // Example value
            stream.setGraphicsStateParameters(gState);

            if (lineDash) {
                stream.setLineDashPattern(new float[]{3f}, 0);
            }

            float lineWidth = (stroke != null) ? (float) stroke.width() : 1.0f;
            stream.setLineWidth(lineWidth);
            stream.setStrokingColor(color);

            // 3. Drawing Logic
            // Cast to float once for readability
            float fx = (float) x;
            float fy = (float) y;
            float fw = (float) w;
            float fh = (float) h;

            // TOP: (x, y+h) -> (x+w, y+h)
            if (sides.contains(Side.TOP) || sides.contains(Side.ALL)) {
                stream.moveTo(fx, fy + fh);
                stream.lineTo(fx + fw, fy + fh);
            }

            // BOTTOM: (x, y) -> (x+w, y)
            if (sides.contains(Side.BOTTOM) || sides.contains(Side.ALL)) {
                stream.moveTo(fx, fy);
                stream.lineTo(fx + fw, fy);
            }

            // LEFT: (x, y) -> (x, y+h)
            if (sides.contains(Side.LEFT) || sides.contains(Side.ALL)) {
                stream.moveTo(fx, fy);
                stream.lineTo(fx, fy + fh);
            }

            // RIGHT: (x+w, y) -> (x+w, y+h)
            if (sides.contains(Side.RIGHT) || sides.contains(Side.ALL)) {
                stream.moveTo(fx + fw, fy);
                stream.lineTo(fx + fw, fy + fh);
            }

            // 4. Apply Stroke
            stream.stroke();

        } finally {
            stream.restoreGraphicsState();
        }
        return true;
    }


}


