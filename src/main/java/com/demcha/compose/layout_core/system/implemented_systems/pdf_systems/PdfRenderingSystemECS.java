package com.demcha.compose.layout_core.system.implemented_systems.pdf_systems;

import com.demcha.compose.layout_core.core.Canvas;
import com.demcha.compose.layout_core.components.content.ImageData;
import com.demcha.compose.layout_core.components.content.shape.Side;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.GuidLineSettings;
import com.demcha.compose.layout_core.system.implemented_systems.RenderingSystemBase;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers.PdfBarcodeRenderHandler;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers.PdfBlockTextRenderHandler;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers.PdfCircleRenderHandler;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers.PdfChunkedBlockTextRenderHandler;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers.PdfContainerRenderHandler;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers.PdfElementRenderHandler;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers.PdfImageRenderHandler;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers.PdfLineRenderHandler;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers.PdfLinkRenderHandler;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers.PdfPageBreakRenderHandler;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers.PdfRectangleRenderHandler;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers.PdfTableRowRenderHandler;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers.PdfTextRenderHandler;
import com.demcha.compose.layout_core.system.interfaces.guides.GuidesRenderer;
import com.demcha.compose.layout_core.system.rendering.RenderHandler;
import com.demcha.compose.layout_core.system.utils.page_breaker.EntitySorter;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
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
    private final PdfImageCache imageCache;

    public PdfRenderingSystemECS(PDDocument doc, Canvas canvas) {
        super(
                canvas,
                new GuidLineSettings(),
                new PdfStream(doc, canvas)
        );
        this.doc = doc;
        this.imageCache = new PdfImageCache(doc);
        guidesRendererInitializer(new PdfGuidesRenderer(this));
        renderHandlers().register(new PdfBarcodeRenderHandler());
        renderHandlers().register(new PdfBlockTextRenderHandler());
        renderHandlers().register(new PdfCircleRenderHandler());
        renderHandlers().register(new PdfChunkedBlockTextRenderHandler());
        renderHandlers().register(new PdfContainerRenderHandler());
        renderHandlers().register(new PdfElementRenderHandler());
        renderHandlers().register(new PdfImageRenderHandler());
        renderHandlers().register(new PdfLineRenderHandler());
        renderHandlers().register(new PdfLinkRenderHandler());
        renderHandlers().register(new PdfPageBreakRenderHandler());
        renderHandlers().register(new PdfRectangleRenderHandler());
        renderHandlers().register(new PdfTextRenderHandler());
        renderHandlers().register(new PdfTableRowRenderHandler());
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
                    var guideLines = entity.isGuideLines();
                    try {
                        var render = entity.getRender();
                        var handler = renderHandlers().find(render);

                        if (handler.isPresent()) {
                            @SuppressWarnings("unchecked")
                            RenderHandler<com.demcha.compose.layout_core.system.interfaces.Render, PdfRenderingSystemECS> typedHandler =
                                    (RenderHandler<com.demcha.compose.layout_core.system.interfaces.Render, PdfRenderingSystemECS>) (RenderHandler<?, ?>) handler.get();
                            typedHandler.render(entityManager, entity, render, this, guideLines);
                        } else {
                            throw new IllegalStateException("No PDF render handler registered for " + render.getClass().getName());
                        }
                    } catch (IOException ex) {
                        log.error("Error processing pdf {}", ex, entity);
                        throw new RuntimeException(ex);
                    }
                } else {
                    log.error("{} has no Render component", entity);
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

    public PDImageXObject getOrCreateImageXObject(ImageData imageData) throws IOException {
        return imageCache.getOrCreateOriginal(imageData);
    }

    public PDImageXObject getOrCreateImageXObject(ImageData imageData, double targetWidth, double targetHeight) throws IOException {
        return imageCache.getOrCreateBestFit(imageData, targetWidth, targetHeight);
    }

    public ImageCacheStats imageCacheStats() {
        return imageCache.stats();
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


    public record ImageCacheStats(int originalCount, int scaledVariantCount) {
    }
}



