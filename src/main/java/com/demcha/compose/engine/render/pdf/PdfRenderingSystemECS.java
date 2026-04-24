package com.demcha.compose.engine.render.pdf;

import com.demcha.compose.engine.core.Canvas;
import com.demcha.compose.engine.components.content.ImageData;
import com.demcha.compose.engine.components.content.shape.Side;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.render.guides.GuidLineSettings;
import com.demcha.compose.engine.render.RenderingSystemBase;
import com.demcha.compose.engine.render.pdf.handlers.PdfBarcodeRenderHandler;
import com.demcha.compose.engine.render.pdf.handlers.PdfBlockTextRenderHandler;
import com.demcha.compose.engine.render.pdf.handlers.PdfCircleRenderHandler;
import com.demcha.compose.engine.render.pdf.handlers.PdfChunkedBlockTextRenderHandler;
import com.demcha.compose.engine.render.pdf.handlers.PdfContainerRenderHandler;
import com.demcha.compose.engine.render.pdf.handlers.PdfElementRenderHandler;
import com.demcha.compose.engine.render.pdf.handlers.PdfImageRenderHandler;
import com.demcha.compose.engine.render.pdf.handlers.PdfLineRenderHandler;
import com.demcha.compose.engine.render.pdf.handlers.PdfLinkRenderHandler;
import com.demcha.compose.engine.render.pdf.handlers.PdfPageBreakRenderHandler;
import com.demcha.compose.engine.render.pdf.handlers.PdfRectangleRenderHandler;
import com.demcha.compose.engine.render.pdf.handlers.PdfTableRowRenderHandler;
import com.demcha.compose.engine.render.pdf.handlers.PdfTextRenderHandler;
import com.demcha.compose.engine.render.guides.GuidesRenderer;
import com.demcha.compose.engine.render.RenderHandler;
import com.demcha.compose.engine.render.EntityRenderOrder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * PDF rendering system that keeps PDFBox lifecycle concerns inside the PDF
 * backend.
 *
 * <p>The shared engine interacts with this renderer through the backend-neutral
 * render-session SPI. This class owns the PDF-specific policy for opening one
 * {@link PdfRenderSession} per render pass and for exposing session-managed
 * page surfaces to handlers.</p>
 */
@Slf4j
@Getter
@Accessors(fluent = true)
public class PdfRenderingSystemECS extends RenderingSystemBase<PDPageContentStream> {
    private static final Logger LIFECYCLE_LOG = LoggerFactory.getLogger("com.demcha.compose.engine.render");

    private final PDDocument doc;
    private final PdfImageCache imageCache;

    /**
     * Creates the PDF renderer, its render stream/session factory, and the
     * built-in PDF render-handler registry.
     *
     * @param doc target PDF document
     * @param canvas logical page canvas used by the layout/render pipeline
     */
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
    /**
     * Runs one PDF render pass for all entities currently registered in the entity
     * manager.
     *
     * <p>
     * The renderer opens one {@link PdfRenderSession} for the whole pass, walks
     * layout layers in depth order, sorts each layer through
     * {@link EntityRenderOrder}, and dispatches every renderable entity to its
     * registered PDF handler.
     * </p>
     *
     * @param entityManager entity registry containing laid-out entities
     */
    public void process(EntityManager entityManager) {
        long startNanos = System.nanoTime();
        LIFECYCLE_LOG.debug(
                "render.pdf.ecs.start entities={} layers={}",
                entityManager.getEntities().size(),
                entityManager.getLayers().size());
        log.info("Processing PdfRenderingSystemECS");

        var layers = entityManager.getLayers();
        try (var renderSession = stream().openRenderPass()) {
            activeRenderSession = renderSession;

            for (Map.Entry<Integer, List<UUID>> layerEntry : layers.entrySet()) {
                int layerDepth = layerEntry.getKey();
                var layerEntityIds = layerEntry.getValue();
                LinkedHashMap<UUID, Entity> orderedLayerEntities = EntityRenderOrder.sortByRenderingPosition(entityManager, layerEntityIds);

                log.debug("Rendering layer {} with {} entities", layerDepth, orderedLayerEntities.size());
                orderedLayerEntities.forEach((uuid, entity) -> {
                    if (entity.hasRender()) {
                        var guideLines = entity.isGuideLines();
                        try {
                            var render = entity.getRender();
                            var handler = renderHandlers().find(render);

                            if (handler.isPresent()) {
                                @SuppressWarnings("unchecked")
                                RenderHandler<com.demcha.compose.engine.render.Render, PdfRenderingSystemECS> typedHandler =
                                        (RenderHandler<com.demcha.compose.engine.render.Render, PdfRenderingSystemECS>) (RenderHandler<?, ?>) handler.get();
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
            LIFECYCLE_LOG.debug(
                    "render.pdf.ecs.end entities={} layers={} durationMs={}",
                    entityManager.getEntities().size(),
                    layers.size(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        } catch (RuntimeException ex) {
            LIFECYCLE_LOG.error(
                    "render.pdf.ecs.failed entities={} layers={} errorType={}",
                    entityManager.getEntities().size(),
                    layers.size(),
                    ex.getClass().getSimpleName(),
                    ex);
            throw ex;
        } catch (IOException ex) {
            LIFECYCLE_LOG.error(
                    "render.pdf.ecs.failed entities={} layers={} errorType={}",
                    entityManager.getEntities().size(),
                    layers.size(),
                    ex.getClass().getSimpleName(),
                    ex);
            throw new RuntimeException("Failed to open or close PDF render session", ex);
        } finally {
            activeRenderSession = null;
        }
    }

    /**
     * Installs the backend-specific guides renderer used by the PDF pipeline.
     *
     * @param guidesRenderer PDF guides renderer implementation
     */
    @Override
    protected void guidesRendererInitializer(GuidesRenderer<PDPageContentStream> guidesRenderer) {
        guidesRenderer(guidesRenderer);
    }


    /**
     * Returns the PDF media box for a concrete page as a {@link Canvas}.
     *
     * @param pageIndex zero-based PDF page index
     * @return canvas representing the page media box
     */
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

    /**
     * Resolves or creates the cached original-sized PDF image object for the given
     * image payload.
     *
     * @param imageData source image payload
     * @return cached or newly created PDF image object
     * @throws IOException when PDFBox image creation fails
     */
    public PDImageXObject getOrCreateImageXObject(ImageData imageData) throws IOException {
        return imageCache.getOrCreateOriginal(imageData);
    }

    /**
     * Resolves or creates a cached best-fit PDF image object for the requested
     * target dimensions.
     *
     * @param imageData source image payload
     * @param targetWidth requested render width
     * @param targetHeight requested render height
     * @return cached or newly created PDF image object variant
     * @throws IOException when PDFBox image creation fails
     */
    public PDImageXObject getOrCreateImageXObject(ImageData imageData, double targetWidth, double targetHeight) throws IOException {
        return imageCache.getOrCreateBestFit(imageData, targetWidth, targetHeight);
    }

    /**
     * Returns current image-cache counters for diagnostics and benchmarks.
     *
     * @return snapshot of original/scaled image cache counts
     */
    public ImageCacheStats imageCacheStats() {
        return imageCache.stats();
    }

    /**
     * Returns the session-owned PDF content stream for the target page.
     *
     * <p>Handlers must restore graphics/text state before returning and must
     * never close the returned stream. The enclosing {@link PdfRenderSession}
     * closes it once at the end of the render pass.</p>
     */
    public PDPageContentStream pageSurface(int pageIndex) throws IOException {
        return renderSession().pageSurface(pageIndex);
    }

    /**
     * Convenience helper for handlers that render on exactly one resolved page.
     *
     * @param entity entity whose placement defines the target page
     * @return session-owned page surface for that page
     * @throws IOException when page surface acquisition fails
     */
    public PDPageContentStream pageSurface(Entity entity) throws IOException {
        return renderSession().pageSurface(entity);
    }

    /**
     * Ensures the target page exists before attaching PDF structures such as
     * annotations.
     *
     * <p>When called outside an active render pass this opens a short-lived
     * render session, keeping page creation policy in the PDF backend rather
     * than leaking PDFBox document management into handlers.</p>
     */
    public void ensurePage(int pageIndex) throws IOException {
        var session = activeRenderSession();
        if (session.isPresent()) {
            session.get().ensurePage(pageIndex);
            return;
        }
        try (var temporarySession = stream().openRenderPass()) {
            temporarySession.ensurePage(pageIndex);
        }
    }

    /**
     * Draws a stroked rectangle on the supplied PDF content stream.
     *
     * @param stroke optional stroke settings
     * @param cs target content stream
     * @param x rectangle X coordinate
     * @param y rectangle Y coordinate
     * @param w rectangle width
     * @param h rectangle height
     * @param color stroke color
     * @param lineDash whether to apply a dashed line pattern
     * @return {@code true} when a rectangle was drawn, {@code false} for
     *         non-positive dimensions
     * @throws IOException when PDF drawing fails
     */
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
    /**
     * Draws a filled circle using Bezier approximation.
     *
     * @param cs target content stream
     * @param cx circle center X
     * @param cy circle center Y
     * @param r circle radius
     * @param fill fill color
     * @throws IOException when PDF drawing fails
     */
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
    /**
     * Convenience overload that renders borders from a resolved render coordinate
     * context.
     *
     * @param stream target content stream
     * @param context resolved drawing context
     * @param lineDash whether to apply a dashed line pattern
     * @param sides selected rectangle sides to stroke
     * @return {@code true} when at least one side was drawn
     * @throws IOException when PDF drawing fails
     */
    public boolean renderBorder(PDPageContentStream stream, RenderCoordinateContext context, boolean lineDash, Set<Side> sides) throws IOException {
        return renderBorder(stream, context.x(), context.y(), context.width(), context.height(), context.stroke(), context.color(), lineDash, sides);
    }

    @Override
    /**
     * Convenience overload that renders a stroked rectangle from a resolved render
     * coordinate context.
     *
     * @param stream target content stream
     * @param context resolved drawing context
     * @param lineDash whether to apply a dashed line pattern
     * @return {@code true} when a rectangle was drawn
     * @throws IOException when PDF drawing fails
     */
    public boolean renderRectangle(PDPageContentStream stream, RenderCoordinateContext context, boolean lineDash) throws IOException {
        return renderRectangle(context.stroke(), stream, context.x(), context.y(), context.width(), context.height(), context.color(), lineDash);
    }

    /**
     * Draws only the selected border sides of a rectangle.
     *
     * @param stream target content stream
     * @param x rectangle X coordinate
     * @param y rectangle Y coordinate
     * @param w rectangle width
     * @param h rectangle height
     * @param stroke optional stroke settings
     * @param color stroke color
     * @param lineDash whether to apply a dashed line pattern
     * @param sides selected sides to stroke
     * @return {@code true} when at least one side was drawn, {@code false} for
     *         invalid geometry or empty side sets
     * @throws IOException when PDF drawing fails
     */
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


    /**
     * Lightweight cache statistics returned for diagnostics and benchmark output.
     */
    public record ImageCacheStats(int originalCount, int scaledVariantCount) {
    }
}



