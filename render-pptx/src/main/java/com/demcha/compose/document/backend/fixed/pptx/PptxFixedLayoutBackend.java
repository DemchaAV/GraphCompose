package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderer;
import com.demcha.compose.document.backend.fixed.SectionUnit;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxEllipseFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxLineFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxShapeFragmentRenderHandler;
import com.demcha.compose.document.exceptions.UnsupportedNodeCapabilityException;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.PdfSemanticFragmentPayload;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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
 * <p>The backend currently renders vector shape, line, and ellipse fragments;
 * the remaining payload types arrive incrementally — see
 * {@code docs/architecture/backend-capability-matrix.md} for the live
 * per-capability status. Multi-section rendering and rasterization are not
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
                new PptxEllipseFragmentRenderHandler());
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
            renderToOutput(graph, output);
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
            renderToOutput(graph, output);
            return;
        }
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            renderToOutput(graph, buffer);
            byte[] bytes = buffer.toByteArray();
            Files.write(context.outputFile(), bytes);
            output.write(bytes);
        }
    }

    /**
     * Rasterization is not implemented for the PPTX backend yet; render the
     * same session through the PDF backend when images are needed.
     */
    @Override
    public List<BufferedImage> renderToImages(LayoutGraph graph,
                                              FixedLayoutRenderContext context,
                                              int dpi,
                                              boolean transparent,
                                              int pageIndex) {
        throw new UnsupportedOperationException(
                "The PPTX backend does not rasterize yet — render through the PDF backend for images.");
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

    private void renderToOutput(LayoutGraph graph, OutputStream output) throws Exception {
        try (XMLSlideShow show = new XMLSlideShow()) {
            PptxRenderSession session = new PptxRenderSession(
                    show, graph.canvas().width(), graph.canvas().height(), graph.totalPages());
            PptxRenderEnvironment environment =
                    new PptxRenderEnvironment(show, session, 0, graph.canvas().height());
            for (PlacedFragment fragment : graph.fragments()) {
                renderFragment(fragment, environment);
            }
            show.write(output);
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

        private Builder() {
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
