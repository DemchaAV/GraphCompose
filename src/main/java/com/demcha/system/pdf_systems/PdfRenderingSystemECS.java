package com.demcha.system.pdf_systems;

import com.demcha.components.components_builders.Canvas;
import com.demcha.components.content.shape.Stroke;
import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.coordinator.PaddingCoordinate;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.layout.coordinator.RenderingPosition;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import com.demcha.exeptions.RenderGuideLinesException;
import com.demcha.system.RenderingSystemECS;
import com.demcha.utils.page_brecker.Breakable;
import com.demcha.utils.page_brecker.PageBreaker;
import com.demcha.utils.page_brecker.PdfCanvas;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

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
public class PdfRenderingSystemECS implements RenderingSystemECS {
    private final PDDocument doc;
    private final Canvas canvas;
    /**
     * The opacity for all guide elements. Value between 0.0f (transparent) and 1.0f (opaque).
     */
    float GUIDES_OPACITY = 0.8f;
    // --- Margin Guide ---
    Color MARGIN_COLOR = new Color(0, 110, 255);
    com.demcha.components.content.shape.Stroke MARGIN_STROKE = new com.demcha.components.content.shape.Stroke(0.5);
    // --- Padding Guide ---
    Color PADDING_COLOR = new Color(255, 140, 0);
    com.demcha.components.content.shape.Stroke PADDING_STROKE = new com.demcha.components.content.shape.Stroke(0.5);
    // --- Content Box Guide ---
    Color BOX_COLOR = new Color(150, 150, 150); // Using a slightly lighter gray
    com.demcha.components.content.shape.Stroke BOX_STROKE = new com.demcha.components.content.shape.Stroke(1.0);
    //viability
    boolean showOnlySetGuide = true;

    public PdfRenderingSystemECS(PDDocument doc, Canvas canvas) {
        this.doc = doc;
        this.canvas = canvas;
    }

    private static RenderGuideLinesException rethrowAsGuideLinesException(IOException io, String message) throws RenderGuideLinesException {
        return new RenderGuideLinesException(message, io);
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

    public PDPageContentStream openContentStream(int pageIndex) throws IOException {
        int numberOfPages = doc.getNumberOfPages();
        if (numberOfPages - 1 < pageIndex) {
            for (int i = 0; i < pageIndex + 1; i++) {
                doc.addPage(new PDPage(new PDRectangle(canvas.x(), canvas.y(), (float) canvas.width(), (float) canvas.height())));
            }
        }
        return new PDPageContentStream(
                doc, doc.getPage(pageIndex),
                PDPageContentStream.AppendMode.APPEND,   // keep existing content if any
                true,                                    // compress
                true                                     // resetContext: isolates graphics state (PDFBox 3)
        );
    }

    public PDPageContentStream openContentStream(Entity entity) throws IOException {
        int startPageIndex = entity.getComponent(Placement.class).orElseThrow().startPage();
        int endPageIndex = entity.getComponent(Placement.class).orElseThrow().endPage();
        if (endPageIndex != startPageIndex) {
            throw new IllegalStateException("endPageIndex should be the same as page startPageIndex");
        }
        return openContentStream(startPageIndex);

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

    public boolean renderGuides(Entity e, PDPageContentStream cs, EnumSet<Guide> guides) throws RenderGuideLinesException {
        boolean any = false;

        if (guides.contains(Guide.MARGIN)) any |= renderMarginFromStream(e, cs);
        if (guides.contains(Guide.PADDING)) any |= renderPaddingFromStream(e, cs);
        if (guides.contains(Guide.BOX)) any |= boxRenderFromStream(e, cs);
        return any;
    }


    public boolean renderGuides(Entity e, PdfRenderingSystemECS renderingSystemECS, EnumSet<Guide> guides) throws RenderGuideLinesException {
        if (e.hasAssignable(Breakable.class)) {
            Placement placement = e.getComponent(Placement.class)
                    .orElseThrow(() -> new IllegalArgumentException("Didn't find any Placement"));
            //top part;
            try (PDPageContentStream pdPageContentStream = renderingSystemECS.openContentStream(placement.endPage())) {
                renderGuides(e, pdPageContentStream, guides);
                log.info("Render topParts has been completed.");

            } catch (IOException exception) {
                throw new RenderGuideLinesException("Rendering guideLine process was incorporated");


            }

            //bottom part
            try (PDPageContentStream pdPageContentStream = renderingSystemECS
                    .openContentStream(placement.startPage())) {
                renderGuides(e, pdPageContentStream, guides);
                log.info("Render bottom has been completed.");
                return true;
            } catch (IOException exception) {
                throw new RenderGuideLinesException("Rendering guideLine process was incorporated");


            }


        }

        return false;
    }


    private boolean renderMarginFromStream(Entity e, PDPageContentStream cs) throws RenderGuideLinesException {
        Margin margin = null;
        if (showOnlySetGuide) {
            margin = e.getComponent(Margin.class).orElse(Margin.zero());
            if (margin.equals(Margin.zero())) return false;
        }


        var pos = e.getComponent(Placement.class).orElseThrow();
        var outer = OuterBoxSize.from(e).orElseThrow();

        double outerX = OuterBoxSize.getX(pos, margin);
        double outerY = OuterBoxSize.getY(pos, margin);

        double x = outerX ;        //TODO возможно нужно убрать -margin.bottom()
        double y;
        double width;
        double height;
        y = (outerY >= 0) ? outerY : 0;
        if (canvas != null) {
            height = ((y + outer.height()) > canvas.boundingTopLine())
                    ? canvas.boundingTopLine() - y
                    : outer.height();
        }
        height = outer.height();
        width = outer.width();


        //  Using constants for configuration
        try {
            renderMarkers(cs, x, y, width, height, MARGIN_COLOR);
            renderRectangle(MARGIN_STROKE, cs, x, y, width, height, MARGIN_COLOR, true);
        } catch (IOException ex) {
            rethrowAsGuideLinesException(ex, "An error occurred while rendering margin guide lines.");

        }
        return true;
    }

    private boolean renderPaddingFromStream(Entity e, PDPageContentStream cs) throws RenderGuideLinesException {
        if (showOnlySetGuide) {
            var padding = e.getComponent(Padding.class).orElse(Padding.zero());
            if (padding.equals(Padding.zero())) return false;
        }


        var pad = PaddingCoordinate.from(e);
        var inner = InnerBoxSize.from(e).orElseThrow();

        double x = pad.x();
        double y = pad.y();
        double width = inner.width();
        double height = inner.hight();
        try {
            renderRectangle(PADDING_STROKE, cs, x, y, width, height, PADDING_COLOR, true);


            renderMarkers(cs, x, y, width, height, PADDING_COLOR);
        } catch (IOException io) {
            rethrowAsGuideLinesException(io, "An error occurred while rendering padding guide lines.");
        }


        return true;
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
            gState.setStrokingAlphaConstant(GUIDES_OPACITY);
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

    private void renderMarkers(PDPageContentStream cs, double x, double y, double w, double h, Color color) throws IOException {
        final float radius = 3.5f;
        float cx = (float) x;
        float cy = (float) y;
        fillCircle(cs, cx, cy, radius, color);
        fillCircle(cs, cx, cy + (float) h, radius, color);
        fillCircle(cs, cx + (float) w, cy, radius, color);
        fillCircle(cs, cx + (float) w, cy + (float) h, radius, color);
    }

    private boolean boxRenderFromStream(@NonNull Entity e, @NonNull PDPageContentStream cs) throws RenderGuideLinesException {
        var boxSize = e.getComponent(ContentSize.class).orElseThrow();
        var rp = RenderingPosition.from(e).orElseThrow();

        double x = rp.x();
        double y = rp.y();
        double w = boxSize.width();
        double h = boxSize.height();

        //  Using constants for configuration

        try {
            return renderRectangle(BOX_STROKE, cs, x, y, w, h, BOX_COLOR, false);
        } catch (IOException ex) {
            throw rethrowAsGuideLinesException(ex, "An error occurred while rendering box guide lines.");
        }

    }

    private void fillCircle(PDPageContentStream cs, float cx, float cy, float r, Color fill) throws IOException {
        if (r <= 0) return;

        final float k = 0.552284749831f;

        cs.saveGraphicsState();
        try {
            PDExtendedGraphicsState gState = new PDExtendedGraphicsState();
            //  Using constant for opacity
            gState.setNonStrokingAlphaConstant(GUIDES_OPACITY);
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
    public <T extends RenderingSystemECS> boolean boxRender(Entity e) throws RenderGuideLinesException {

        try (PDPageContentStream cs = openContentStream(e)) {
            return boxRenderFromStream(e, cs);
        } catch (IOException ex) {
            String msg = "Error opening content stream for Box render";
            log.error(msg, ex);
            throw rethrowAsGuideLinesException(ex, msg);

        }

    }

    @Override
    public <T extends RenderingSystemECS> boolean renderPadding(Entity e) throws RenderGuideLinesException {

        try (PDPageContentStream cs = openContentStream(e)) {
            return renderPaddingFromStream(e, cs);
        } catch (IOException ex) {
            String msg = "Error opening content stream for Padding render";
            log.error(msg, ex);
            throw rethrowAsGuideLinesException(ex, msg); // Stop execution
        }

    }

    @Override
    public <T extends RenderingSystemECS> boolean renderMargin(Entity e) throws RenderGuideLinesException {

        // 3. Prepare the PDF "canvas"
        try (PDPageContentStream cs = openContentStream(e)) {
            return renderMarginFromStream(e, cs);

        } catch (IOException ex) {
            String msg = "Error opening content stream for marginRender";
            log.error(msg, ex);
            throw rethrowAsGuideLinesException(ex, msg); // Stop execution
        }

    }


    public <T extends Component> boolean renderGuides(Entity entity, EnumSet<Guide> defaultGuides, T component) throws RenderGuideLinesException {
        try (PDPageContentStream pdPageContentStream = openContentStream(entity)) {
            return renderGuides(entity, pdPageContentStream, defaultGuides);
        } catch (IOException e) {
            throw rethrowAsGuideLinesException(e, "Error opening content stream for Guides render component :" + component.getClass().getSimpleName());
        }
    }

    public PDPageContentStream reopenContentStreamForTextData(PDPageContentStream cs, int currentPage, PDFont font, float fontSize, Color color) throws IOException {
        if (cs != null) {
            cs.endText();
            cs.restoreGraphicsState();
            cs.close();
        }
        return openContentSteamForTextData(currentPage, font, fontSize, color);
    }

    public PDPageContentStream openContentSteamForTextData(int currentPage, PDFont font, float fontSize, Color color) throws IOException {
        PDPageContentStream newStream = openContentStream(currentPage);
        newStream.saveGraphicsState();
        newStream.setFont(font, fontSize);
        newStream.setNonStrokingColor(color);
        newStream.beginText();
        return newStream;
    }


}


