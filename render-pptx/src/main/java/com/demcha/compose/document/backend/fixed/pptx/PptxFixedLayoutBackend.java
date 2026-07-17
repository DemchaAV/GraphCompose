package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderer;
import com.demcha.compose.document.backend.fixed.SectionUnit;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxEllipseFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxLineFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxParagraphFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxShapeFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfFixedLayoutBackend;
import com.demcha.compose.document.backend.fixed.pdf.PdfMeasurementResources;
import com.demcha.compose.document.exceptions.UnsupportedNodeCapabilityException;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.PdfSemanticFragmentPayload;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Coordinate-exact PPTX render backend consuming a fully resolved
 * {@link LayoutGraph} — the same graph the PDF backend paints, so both
 * formats share identical geometry by construction.
 *
 * <p>One resolved page becomes one slide sized exactly like the page canvas;
 * fragments render in graph order through a per-payload-type handler registry
 * that mirrors the PDF backend's dispatch (exact class lookup, then an
 * {@code instanceof} fallback for payload subclasses; an unknown payload
 * throws {@link UnsupportedNodeCapabilityException}). Custom handlers replace
 * built-in defaults per payload type via {@link Builder#addHandler}.</p>
 *
 * <p>The backend currently renders paragraphs (including rich runs, chips and
 * inline graphics) plus vector shape, line, and ellipse fragments;
 * the remaining payload types arrive incrementally — see
 * {@code docs/architecture/backend-capability-matrix.md} for the live
 * per-capability status. Multi-section rendering and render-to-images are not
 * implemented yet and throw {@link UnsupportedOperationException}.</p>
 *
 * <p><b>Thread-safety:</b> immutable and reusable across renders; each render
 * pass owns its own slide show and environment.</p>
 *
 * @since 2.1.0
 */
public final class PptxFixedLayoutBackend implements FixedLayoutRenderer {

    private static final Logger RENDER_LOG = LoggerFactory.getLogger("com.demcha.compose.engine.render");

    private final Map<Class<?>, PptxFragmentRenderHandler<?>> handlers;

    /**
     * Raster-slide resolution in DPI; {@code 0} keeps the default editable
     * vector mode.
     */
    private final int rasterSlidesDpi;

    /**
     * Creates a backend with the default handler set.
     */
    public PptxFixedLayoutBackend() {
        this(builder());
    }

    private PptxFixedLayoutBackend(Builder builder) {
        Map<Class<?>, PptxFragmentRenderHandler<?>> merged = new LinkedHashMap<>();
        for (PptxFragmentRenderHandler<?> handler : defaultHandlers()) {
            merged.put(handler.payloadType(), handler);
        }
        for (PptxFragmentRenderHandler<?> handler : builder.customHandlers) {
            merged.put(handler.payloadType(), handler);
        }
        this.handlers = Map.copyOf(merged);
        this.rasterSlidesDpi = builder.rasterSlidesDpi;
    }

    /**
     * Returns a builder for a backend with custom handlers.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private static List<PptxFragmentRenderHandler<?>> defaultHandlers() {
        return List.of(
                new PptxShapeFragmentRenderHandler(),
                new PptxLineFragmentRenderHandler(),
                new PptxEllipseFragmentRenderHandler(),
                new PptxParagraphFragmentRenderHandler());
    }

    @Override
    public String name() {
        return "pptx-fixed-layout";
    }

    @Override
    public byte[] render(LayoutGraph graph, FixedLayoutRenderContext context) throws Exception {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(context, "context");

        long startNanos = System.nanoTime();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            renderToOutput(graph, context, output);
            byte[] bytes = output.toByteArray();
            if (context.outputFile() != null) {
                Files.write(context.outputFile(), bytes);
            }
            RENDER_LOG.debug(
                    "render.pptx.fixed.end pages={} fragments={} byteCount={} durationMs={}",
                    graph.totalPages(),
                    graph.fragments().size(),
                    bytes.length,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
            return bytes;
        } catch (Exception ex) {
            RENDER_LOG.error(
                    "render.pptx.fixed.failed pages={} fragments={} errorType={}",
                    graph.totalPages(),
                    graph.fragments().size(),
                    ex.getClass().getSimpleName(),
                    ex);
            throw ex;
        }
    }

    @Override
    public void write(LayoutGraph graph, FixedLayoutRenderContext context) throws Exception {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(context, "context");
        OutputStream output = Objects.requireNonNull(context.outputStream(), "context.outputStream");

        if (context.outputFile() == null) {
            renderToOutput(graph, context, output);
            return;
        }
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            renderToOutput(graph, context, buffer);
            byte[] bytes = buffer.toByteArray();
            Files.write(context.outputFile(), bytes);
            output.write(bytes);
        }
    }

    /**
     * Render-to-images is not implemented for the PPTX backend yet; render
     * the same session through the PDF backend when page images are needed.
     */
    @Override
    public List<BufferedImage> renderToImages(LayoutGraph graph,
                                              FixedLayoutRenderContext context,
                                              int dpi,
                                              boolean transparent,
                                              int pageIndex) {
        throw new UnsupportedOperationException(
                "The PPTX backend does not render pages to images yet — "
                        + "render through the PDF backend for page images.");
    }

    /**
     * Multi-section concatenation is not implemented for the PPTX backend yet.
     */
    @Override
    public byte[] renderSections(List<SectionUnit> sections) {
        throw new UnsupportedOperationException(
                "The PPTX backend does not render multi-section documents yet.");
    }

    /**
     * Multi-section concatenation is not implemented for the PPTX backend yet.
     */
    @Override
    public void writeSections(List<SectionUnit> sections, OutputStream output) {
        throw new UnsupportedOperationException(
                "The PPTX backend does not render multi-section documents yet.");
    }

    private void renderToOutput(LayoutGraph graph,
                                FixedLayoutRenderContext context,
                                OutputStream output) throws Exception {
        if (rasterSlidesDpi > 0) {
            renderRasterSlides(graph, context, output);
            return;
        }
        try (XMLSlideShow show = new XMLSlideShow();
             PdfMeasurementResources measurement =
                     PdfMeasurementResources.open(context.customFontFamilies())) {
            PptxRenderSession session = new PptxRenderSession(
                    show, graph.canvas().width(), graph.canvas().height(), graph.totalPages());
            PptxRenderEnvironment environment =
                    new PptxRenderEnvironment(show, session, 0, graph.canvas().height(),
                            measurement.fontLibrary(), context.customFontFamilies());
            for (PlacedFragment fragment : graph.fragments()) {
                renderFragment(fragment, environment);
            }
            show.write(output);
        }
    }

    /**
     * Raster-slide mode: every page is rendered through the PDF backend at the
     * configured DPI and placed as one full-slide picture — a pixel-exact copy
     * of the PDF/PNG output in .pptx form. Slides are not editable as text.
     */
    private void renderRasterSlides(LayoutGraph graph,
                                    FixedLayoutRenderContext context,
                                    OutputStream output) throws Exception {
        List<BufferedImage> pages = new PdfFixedLayoutBackend()
                .renderToImages(graph, context, rasterSlidesDpi, false, -1);
        try (XMLSlideShow show = new XMLSlideShow()) {
            PptxRenderSession session = new PptxRenderSession(
                    show, graph.canvas().width(), graph.canvas().height(), graph.totalPages());
            for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
                XSLFPictureData data = show.addPicture(
                        encodePng(pages.get(pageIndex)), PictureData.PictureType.PNG);
                XSLFPictureShape picture = session.slide(pageIndex).createPicture(data);
                picture.setAnchor(new Rectangle2D.Double(
                        0, 0, graph.canvas().width(), graph.canvas().height()));
            }
            show.write(output);
        }
    }

    private static byte[] encodePng(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", buffer);
            return buffer.toByteArray();
        }
    }

    private void renderFragment(PlacedFragment fragment, PptxRenderEnvironment environment) throws Exception {
        Object payload = fragment.payload();
        PptxFragmentRenderHandler<Object> handler = handlerFor(payload);
        handler.render(fragment, payload, environment);
        finishRenderedFragment(fragment, payload, environment);
    }

    /**
     * Generic post-render bookkeeping shared by every semantic payload:
     * fragment-rectangle links and bookmark records are collected here so no
     * individual handler has to remember navigation concerns.
     */
    private void finishRenderedFragment(PlacedFragment fragment,
                                        Object payload,
                                        PptxRenderEnvironment environment) {
        if (payload instanceof PdfSemanticFragmentPayload semanticPayload) {
            if (semanticPayload.linkTarget() != null) {
                environment.recordFragmentLink(fragment, semanticPayload.linkTarget());
            }
            if (semanticPayload.bookmarkOptions() != null) {
                environment.registerBookmark(fragment, semanticPayload.bookmarkOptions());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private PptxFragmentRenderHandler<Object> handlerFor(Object payload) {
        if (payload == null) {
            throw new UnsupportedNodeCapabilityException(
                    "The PPTX fixed-layout backend cannot render a fragment without a payload.");
        }
        PptxFragmentRenderHandler<?> handler = handlers.get(payload.getClass());
        if (handler == null) {
            for (Map.Entry<Class<?>, PptxFragmentRenderHandler<?>> entry : handlers.entrySet()) {
                if (entry.getKey().isInstance(payload)) {
                    handler = entry.getValue();
                    break;
                }
            }
        }
        if (handler == null) {
            throw new UnsupportedNodeCapabilityException(
                    "The PPTX fixed-layout backend has no render handler for payload type "
                    + payload.getClass().getName()
                    + " yet — see docs/architecture/backend-capability-matrix.md for supported capabilities.");
        }
        return (PptxFragmentRenderHandler<Object>) handler;
    }

    /**
     * Builder assembling a backend with custom fragment handlers.
     *
     * @since 2.1.0
     */
    public static final class Builder {

        private final List<PptxFragmentRenderHandler<?>> customHandlers = new ArrayList<>();
        private int rasterSlidesDpi;

        private Builder() {
        }

        /**
         * Switches the backend to raster-slide mode: every page renders
         * through the PDF backend at the given DPI and lands as one
         * full-slide picture — a pixel-exact copy of the PDF/PNG output for
         * decks that must look identical everywhere. The trade-off is that
         * slide content is a picture: text is not selectable or editable and
         * files grow with resolution. Leave unset for the default editable
         * vector mode.
         *
         * <p>Navigation is baked into the pixels: hyperlinks, bookmarks, and
         * custom fragment handlers do not apply in raster mode. Every page is
         * held in memory during the render, so memory grows with page count
         * and the square of the DPI; the resolution is capped at 600 DPI.</p>
         *
         * @param dpi raster resolution in dots per inch (72 = native size, max 600)
         * @return this builder
         * @throws IllegalArgumentException if {@code dpi} is not in [1, 600]
         */
        public Builder rasterSlides(int dpi) {
            if (dpi <= 0 || dpi > 600) {
                throw new IllegalArgumentException(
                        "Raster DPI must be between 1 and 600: " + dpi);
            }
            this.rasterSlidesDpi = dpi;
            return this;
        }

        /**
         * Registers a custom fragment handler. A handler reporting the same
         * payload type as a built-in default replaces that default.
         *
         * @param handler handler to register
         * @return this builder
         */
        public Builder addHandler(PptxFragmentRenderHandler<?> handler) {
            customHandlers.add(Objects.requireNonNull(handler, "handler"));
            return this;
        }

        /**
         * Builds the configured backend.
         *
         * @return immutable backend instance
         */
        public PptxFixedLayoutBackend build() {
            return new PptxFixedLayoutBackend(this);
        }
    }
}
