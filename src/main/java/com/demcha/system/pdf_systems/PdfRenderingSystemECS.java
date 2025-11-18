package com.demcha.system.pdf_systems;

import com.demcha.components.components_builders.Canvas;
import com.demcha.components.content.shape.Stroke;
import com.demcha.components.core.Entity;
import com.demcha.core.EntityManager;
import com.demcha.system.GuidLineSettings;
import com.demcha.system.RenderStream;
import com.demcha.system.RenderingSystemECS;
import com.demcha.utils.page_brecker.PageBreaker;
import com.demcha.utils.page_brecker.PdfCanvas;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.awt.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PdfRenderingSystemECS — diagnostics & hardening
 * <p>
 * Goals:
 * 1) PROVE that margins are not sneaking into the drawn width/height.
 * 2) Avoid any chance that a buggy Padding.zero() leaks non-zero values.
 * 3) Atomically save without PDFBox overwrite warning.
 */
@Slf4j
@Getter
@Accessors(fluent = true)
public class PdfRenderingSystemECS implements RenderingSystemECS {
    private final PDDocument doc;
    private final Canvas canvas;
    private final PdfStream stream;
    private final GuidLineSettings guidLineSettings;
    private final PdfGuidesRenderer guidesRenderer;


    public PdfRenderingSystemECS(PDDocument doc, Canvas canvas) {
        this.doc = doc;
        this.canvas = canvas;
        this.stream = new PdfStream(doc, canvas);
        this.guidLineSettings = new GuidLineSettings();
        guidesRenderer = new PdfGuidesRenderer(this);
    }


    @Override
    public void process(EntityManager entityManager) {
        log.info("Processing PdfRenderingSystemECS");

        var entities = entityManager.getLayers();

        for (Map.Entry<Integer, List<UUID>> e : entities.entrySet()) {
            var entitiesUuid = e.getValue();
            LinkedHashMap<UUID, Entity> uuidEntityLinkedHashMap = PageBreaker.sortByYPositionToMap(entityManager, entitiesUuid);

            PageBreaker.sortByYPositionToMap(entityManager, entitiesUuid);

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


    void fillCircle(PDPageContentStream cs, float cx, float cy, float r, Color fill) throws IOException {
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





    /**
     * @return PdfGuidesRenderer
     */
    @Override
    public PdfGuidesRenderer guideRenderer() {
        return (PdfGuidesRenderer)guidesRenderer;
    }
}


