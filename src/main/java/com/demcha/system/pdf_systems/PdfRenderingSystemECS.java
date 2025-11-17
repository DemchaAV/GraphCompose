package com.demcha.system.pdf_systems;

import com.demcha.components.components_builders.Canvas;
import com.demcha.components.content.shape.Stroke;
import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.layout.RenderCoordinate;
import com.demcha.components.layout.coordinator.PaddingCoordinate;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import com.demcha.exeptions.RenderGuideLinesException;
import com.demcha.system.GuidLineSettings;
import com.demcha.system.RenderStream;
import com.demcha.system.RenderingSystemECS;
import com.demcha.utils.page_brecker.Breakable;
import com.demcha.utils.page_brecker.PageBreaker;
import com.demcha.utils.page_brecker.PdfCanvas;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

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
    private final RenderStream<PDPageContentStream> stream;
    private GuidLineSettings guidLineSettings;


    public PdfRenderingSystemECS(PDDocument doc, Canvas canvas) {
        this.doc = doc;
        this.canvas = canvas;
        this.stream = new PdfStream(doc, canvas);
        this.guidLineSettings = new GuidLineSettings();

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
            try (PDPageContentStream pdPageContentStream = stream.openContentStream(placement.endPage())) {
                renderGuides(e, pdPageContentStream, guides);
                log.info("Render topParts has been completed.");

            } catch (IOException exception) {
                throw new RenderGuideLinesException("Rendering guideLine process was incorporated");


            }

            //bottom part
            try (PDPageContentStream pdPageContentStream = stream.openContentStream(placement.startPage())) {
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
        RenderCoordinateContext coordinateContext = null;
        Optional<RenderCoordinateContext> coordinateOpt =
                resolveCoordinateContext(e, guidLineSettings, Margin.class, Margin::zero);

        if (coordinateOpt.isEmpty()) {
            return false;
        } else {
            coordinateContext = coordinateOpt.get();
        }


        double x = coordinateContext.x() >= 0 ? coordinateContext.x() : 0;
        double y;
        double width;
        double height;
        y = (coordinateContext.y() >= 0) ? coordinateContext.y() : 0;
        if (canvas != null) {
            height = ((y + coordinateContext.height()) > canvas.boundingTopLine())
                    ? canvas.boundingTopLine() - y
                    : coordinateContext.height();
        }
        height = coordinateContext.height();
        width = coordinateContext.width();


        //  Using constants for configuration
        try {
            renderMarkers(cs, x, y, width, height, guidLineSettings.MARGIN_COLOR());
            renderRectangle(guidLineSettings.MARGIN_STROKE(), cs, x, y, width, height, guidLineSettings.MARGIN_COLOR(), true);
        } catch (IOException ex) {
            rethrowAsGuideLinesException(ex, "An error occurred while rendering margin guide lines.");

        }
        return true;
    }

    private <T extends RenderCoordinate & Component>
    Optional<RenderCoordinateContext> resolveCoordinateContext(
            Entity e,
            @NonNull GuidLineSettings guidLineSettings,
            Class<T> componentClass,
            Supplier<T> defaultSupplier
    ) {
        if (!guidLineSettings.showOnlySetGuide()) {
            return Optional.empty();
        }

        // Берём компонент или default (Margin.zero() / Padding.zero())
        T context = e.getComponent(componentClass)
                .orElseGet(()->{
                    log.info("{} is {} ", componentClass, defaultSupplier.get());
                    return defaultSupplier.get();
                });

        return context.renderCoordinate(e);
    }

    private boolean renderPaddingFromStream(Entity e, PDPageContentStream cs) throws RenderGuideLinesException {


        RenderCoordinateContext coordinateContext;
        Optional<RenderCoordinateContext> coordinateOpt =
                resolveCoordinateContext(e, guidLineSettings, Padding.class, Padding::zero);

        if (coordinateOpt.isEmpty()) {
            return false;
        } else {
            coordinateContext = coordinateOpt.get();
        }


        double x = coordinateContext.x();
        double y = coordinateContext.y();
        double width = coordinateContext.width();
        double height = coordinateContext.height();


        try {
            renderRectangle(guidLineSettings.PADDING_STROKE(), cs, x, y, width, height, guidLineSettings.PADDING_COLOR(), true);
            renderMarkers(cs, x, y, width, height, guidLineSettings.PADDING_COLOR());
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
        var margin = e.getComponent(Margin.class).orElse(Margin.zero());
        var placement = e.getComponent(Placement.class).orElseThrow();
//TODO x должен быть просто placement.x() так как это и есть коробка
        double x = placement.x() + margin.left();
        double y = placement.y();
        double w = boxSize.width();
        double h = boxSize.height();

        //  Using constants for configuration

        try {
            return renderRectangle(guidLineSettings.BOX_STROKE(), cs, x, y, w, h, guidLineSettings.BOX_COLOR(), false);
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
    public <T extends RenderingSystemECS> boolean boxRender(Entity e) throws RenderGuideLinesException {

        try (PDPageContentStream cs = stream.openContentStream(e)) {
            return boxRenderFromStream(e, cs);
        } catch (IOException ex) {
            String msg = "Error opening content stream for Box render";
            log.error(msg, ex);
            throw rethrowAsGuideLinesException(ex, msg);

        }

    }

    @Override
    public <T extends RenderingSystemECS> boolean renderPadding(Entity e) throws RenderGuideLinesException {

        try (PDPageContentStream cs = stream.openContentStream(e)) {
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
        try (PDPageContentStream cs = stream.openContentStream(e)) {
            return renderMarginFromStream(e, cs);

        } catch (IOException ex) {
            String msg = "Error opening content stream for marginRender";
            log.error(msg, ex);
            throw rethrowAsGuideLinesException(ex, msg); // Stop execution
        }

    }


    public <T extends Component> boolean renderGuides(Entity entity, EnumSet<Guide> defaultGuides, T component) throws RenderGuideLinesException {
        try (PDPageContentStream pdPageContentStream = stream.openContentStream(entity)) {
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
        PDPageContentStream newStream = stream.openContentStream(currentPage);
        newStream.saveGraphicsState();
        newStream.setFont(font, fontSize);
        newStream.setNonStrokingColor(color);
        newStream.beginText();
        return newStream;
    }


}


