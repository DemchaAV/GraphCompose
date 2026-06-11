package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.backend.fixed.FixedLayoutBackend;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext;
import com.demcha.compose.document.backend.fixed.pdf.handlers.*;
import com.demcha.compose.document.backend.fixed.pdf.options.*;
import com.demcha.compose.document.exceptions.UnsupportedNodeCapabilityException;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.*;
import com.demcha.compose.font.FontLibrary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Handler-based fixed-layout PDF backend for the canonical semantic document API.
 *
 * <p>This backend consumes a fully resolved {@link LayoutGraph} and delegates
 * fragment painting to payload-specific handlers. The backend itself is
 * responsible for document lifecycle, page creation, page-scoped stream
 * management, and shared resource caches such as decoded images and resolved
 * fonts.</p>
 *
 * <p><b>Thread-safety:</b> instances are immutable after construction and can be
 * reused, but each {@link #render(LayoutGraph, FixedLayoutRenderContext)} call
 * creates a new page-scoped render session.</p>
 *
 * @author Artem Demchyshyn
 */
public final class PdfFixedLayoutBackend implements FixedLayoutBackend<byte[]> {
    private static final Logger RENDER_LOG = LoggerFactory.getLogger("com.demcha.compose.engine.render");

    private final Map<Class<?>, PdfFragmentRenderHandler<?>> handlers;
    private final PdfDebugOptions debug;
    private final PdfMetadataOptions metadataOptions;
    private final PdfWatermarkOptions watermarkOptions;
    private final PdfProtectionOptions protectionOptions;
    private final List<PdfHeaderFooterOptions> headerFooterOptions;

    /**
     * Creates a backend with the built-in paragraph, shape, image, and table handlers.
     */
    public PdfFixedLayoutBackend() {
        this(defaultHandlers(), PdfDebugOptions.none(), null, null, null, List.of());
    }

    PdfFixedLayoutBackend(Collection<? extends PdfFragmentRenderHandler<?>> handlers) {
        this(handlers, PdfDebugOptions.none(), null, null, null, List.of());
    }

    private PdfFixedLayoutBackend(Collection<? extends PdfFragmentRenderHandler<?>> handlers,
                                  PdfDebugOptions debug,
                                  PdfMetadataOptions metadataOptions,
                                  PdfWatermarkOptions watermarkOptions,
                                  PdfProtectionOptions protectionOptions,
                                  Collection<PdfHeaderFooterOptions> headerFooterOptions) {
        Map<Class<?>, PdfFragmentRenderHandler<?>> registry = new LinkedHashMap<>();
        for (PdfFragmentRenderHandler<?> handler : handlers) {
            PdfFragmentRenderHandler<?> previous = registry.put(handler.payloadType(), Objects.requireNonNull(handler, "handler"));
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate PDF handler for payload type " + handler.payloadType().getName());
            }
        }
        this.handlers = Map.copyOf(registry);
        this.debug = debug == null ? PdfDebugOptions.none() : debug;
        this.metadataOptions = metadataOptions;
        this.watermarkOptions = watermarkOptions;
        this.protectionOptions = protectionOptions;
        this.headerFooterOptions = List.copyOf(headerFooterOptions);
    }

    /**
     * Returns a builder for PDF-specific render options.
     *
     * @return PDF backend builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private static List<PdfFragmentRenderHandler<?>> defaultHandlers() {
        return List.of(
                new PdfBarcodeFragmentRenderHandler(),
                new PdfParagraphFragmentRenderHandler(),
                new PdfShapeFragmentRenderHandler(),
                new PdfLineFragmentRenderHandler(),
                new PdfEllipseFragmentRenderHandler(),
                new PdfPolygonFragmentRenderHandler(),
                new PdfImageFragmentRenderHandler(),
                new PdfTableRowFragmentRenderHandler(),
                new PdfShapeClipBeginRenderHandler(),
                new PdfShapeClipEndRenderHandler(),
                new PdfTransformBeginRenderHandler(),
                new PdfTransformEndRenderHandler());
    }

    private static PdfLinkAnnotationWriter.PlacedPdfRect spanLinkRectangle(ParagraphSpan span,
                                                                           double spanX,
                                                                           double lineTop,
                                                                           double lineHeight,
                                                                           double textAscent,
                                                                           double baselineOffsetFromBottom) {
        com.demcha.compose.document.node.InlineImageAlignment alignment;
        double graphicHeight;
        double baselineOffset;
        if (span instanceof ParagraphImageSpan imageSpan) {
            alignment = imageSpan.alignment();
            graphicHeight = imageSpan.height();
            baselineOffset = imageSpan.baselineOffset();
        } else if (span instanceof com.demcha.compose.document.layout.payloads.ParagraphShapeSpan shapeSpan) {
            alignment = shapeSpan.alignment();
            graphicHeight = shapeSpan.height();
            baselineOffset = shapeSpan.baselineOffset();
        } else {
            // Text spans cover the full line box.
            return new PdfLinkAnnotationWriter.PlacedPdfRect(
                    spanX,
                    lineTop - lineHeight,
                    span.width(),
                    lineHeight);
        }
        // Inline-graphic baseline placement, kept in lockstep with
        // PdfParagraphFragmentRenderHandler.resolveInlineGraphicBottom — both
        // place an inline image or shape on the text baseline identically.
        double baselineY = lineTop - lineHeight + baselineOffsetFromBottom;
        double lineBottom = baselineY - baselineOffsetFromBottom;
        double base = switch (alignment == null
                ? com.demcha.compose.document.node.InlineImageAlignment.CENTER
                : alignment) {
            case BASELINE -> baselineY;
            case CENTER -> lineBottom + (lineHeight - graphicHeight) / 2.0;
            case TEXT_TOP -> baselineY + textAscent - graphicHeight;
            case TEXT_BOTTOM -> lineBottom;
        };
        return new PdfLinkAnnotationWriter.PlacedPdfRect(
                spanX,
                base + baselineOffset,
                span.width(),
                graphicHeight);
    }

    private static List<PdfFragmentRenderHandler<?>> mergeHandlers(
            List<PdfFragmentRenderHandler<?>> defaults,
            List<PdfFragmentRenderHandler<?>> additions) {
        if (additions.isEmpty()) {
            return defaults;
        }
        Map<Class<?>, PdfFragmentRenderHandler<?>> byPayloadType = new LinkedHashMap<>();
        for (PdfFragmentRenderHandler<?> handler : defaults) {
            byPayloadType.put(handler.payloadType(), handler);
        }
        for (PdfFragmentRenderHandler<?> handler : additions) {
            PdfFragmentRenderHandler<?> previous = byPayloadType.put(handler.payloadType(), handler);
            if (previous != null) {
                RENDER_LOG.debug(
                        "render.pdf.handler.replaced payloadType={} previous={} replacement={}",
                        handler.payloadType().getName(),
                        previous.getClass().getName(),
                        handler.getClass().getName());
            }
        }
        return List.copyOf(byPayloadType.values());
    }

    @Override
    public String name() {
        return "pdf-fixed-layout";
    }

    /**
     * Renders the resolved layout graph into PDF bytes and optionally writes the
     * result to the configured output file.
     *
     * @param graph   resolved layout graph produced by the semantic compiler
     * @param context fixed-layout render configuration including page canvas and output target
     * @return rendered PDF document bytes
     * @throws Exception if PDF creation, rendering, or saving fails
     */
    @Override
    public byte[] render(LayoutGraph graph, FixedLayoutRenderContext context) throws Exception {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(context, "context");

        long startNanos = System.nanoTime();
        RENDER_LOG.debug(
                "render.pdf.fixed.start pages={} fragments={} outputConfigured={} streamConfigured={} debug={}",
                graph.totalPages(),
                graph.fragments().size(),
                context.outputFile() != null,
                context.outputStream() != null,
                debug);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            int pageCount = renderToOutput(graph, context, output);
            byte[] bytes = output.toByteArray();
            if (context.outputFile() != null) {
                Files.write(context.outputFile(), bytes);
            }
            RENDER_LOG.debug(
                    "render.pdf.fixed.end pages={} fragments={} byteCount={} durationMs={}",
                    pageCount,
                    graph.fragments().size(),
                    bytes.length,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
            return bytes;
        } catch (Exception ex) {
            RENDER_LOG.error(
                    "render.pdf.fixed.failed pages={} fragments={} errorType={}",
                    graph.totalPages(),
                    graph.fragments().size(),
                    ex.getClass().getSimpleName(),
                    ex);
            throw ex;
        }
    }

    /**
     * Streams the resolved layout graph into the caller-owned output stream.
     *
     * <p>The backend writes the complete PDF document to the supplied stream but
     * never closes it. The caller remains responsible for HTTP/file/S3 stream
     * lifecycle and backpressure.</p>
     *
     * @param graph   resolved layout graph produced by the semantic compiler
     * @param context fixed-layout render configuration with a non-null output stream
     * @throws Exception if PDF creation, rendering, or saving fails
     */
    public void write(LayoutGraph graph, FixedLayoutRenderContext context) throws Exception {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(context, "context");
        OutputStream output = Objects.requireNonNull(context.outputStream(), "context.outputStream");

        long startNanos = System.nanoTime();
        RENDER_LOG.debug(
                "render.pdf.fixed.stream.start pages={} fragments={} outputConfigured={} debug={}",
                graph.totalPages(),
                graph.fragments().size(),
                context.outputFile() != null,
                debug);
        try {
            int pageCount;
            if (context.outputFile() == null) {
                pageCount = renderToOutput(graph, context, output);
            } else {
                try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    pageCount = renderToOutput(graph, context, buffer);
                    byte[] bytes = buffer.toByteArray();
                    Files.write(context.outputFile(), bytes);
                    output.write(bytes);
                }
            }
            RENDER_LOG.debug(
                    "render.pdf.fixed.stream.end pages={} fragments={} durationMs={}",
                    pageCount,
                    graph.fragments().size(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        } catch (Exception ex) {
            RENDER_LOG.error(
                    "render.pdf.fixed.stream.failed pages={} fragments={} errorType={}",
                    graph.totalPages(),
                    graph.fragments().size(),
                    ex.getClass().getSimpleName(),
                    ex);
            throw ex;
        }
    }

    private int renderToOutput(LayoutGraph graph, FixedLayoutRenderContext context, OutputStream output) throws Exception {
        try (PDDocument document = new PDDocument()) {
            FontLibrary fonts = PdfFontLibraryFactory.library(document, context.customFontFamilies());
            List<PDPage> pages = createPages(document, graph);

            try (PdfRenderSession session = new PdfRenderSession(document, pages)) {
                PdfRenderEnvironment environment = new PdfRenderEnvironment(document, fonts, session);
                Map<String, Map<Integer, PdfGuideLinesRenderer.Bounds>> ownerBounds = debug.enabled()
                        ? PdfGuideLinesRenderer.computeOwnerBounds(graph.fragments())
                        : Map.of();
                Set<String> labelKeys = debug.showNodeLabels() ? new HashSet<>() : Set.of();
                PdfFragmentRenderHandler<?> tableRowHandler = handlers.get(TableRowFragmentPayload.class);
                for (int index = 0; index < graph.fragments().size(); index++) {
                    PlacedFragment fragment = graph.fragments().get(index);
                    if (fragment.payload() instanceof TableRowFragmentPayload
                        && tableRowHandler instanceof PdfTableRowFragmentRenderHandler tableHandler) {
                        index = renderTableRowGroup(graph.fragments(), index, tableHandler, environment, ownerBounds, labelKeys);
                        continue;
                    }
                    renderFragment(fragment, environment, ownerBounds, labelKeys);
                }
                PdfBookmarkOutlineWriter.apply(document, environment.bookmarkRecords());
            }

            PdfDocumentPostProcessor.apply(
                    document,
                    context.canvas(),
                    metadataOptions,
                    watermarkOptions,
                    protectionOptions,
                    headerFooterOptions);

            document.save(output);
            return pages.size();
        }
    }

    private int renderTableRowGroup(List<PlacedFragment> fragments,
                                    int startIndex,
                                    PdfTableRowFragmentRenderHandler handler,
                                    PdfRenderEnvironment environment,
                                    Map<String, Map<Integer, PdfGuideLinesRenderer.Bounds>> ownerBounds,
                                    Set<String> labelKeys) throws Exception {
        String tablePath = fragments.get(startIndex).path();
        int endExclusive = startIndex;
        while (endExclusive < fragments.size()
               && Objects.equals(fragments.get(endExclusive).path(), tablePath)
               && fragments.get(endExclusive).payload() instanceof TableRowFragmentPayload) {
            endExclusive++;
        }

        for (int index = startIndex; index < endExclusive; index++) {
            PlacedFragment fragment = fragments.get(index);
            handler.renderFills(
                    fragment,
                    (TableRowFragmentPayload) fragment.payload(),
                    environment);
        }
        for (int index = startIndex; index < endExclusive; index++) {
            PlacedFragment fragment = fragments.get(index);
            TableRowFragmentPayload payload =
                    (TableRowFragmentPayload) fragment.payload();
            handler.renderBordersAndText(fragment, payload, environment);
            finishRenderedFragment(fragment, payload, environment, ownerBounds, labelKeys);
        }

        return endExclusive - 1;
    }

    private List<PDPage> createPages(PDDocument document, LayoutGraph graph) {
        int pageCount = Math.max(graph.totalPages(), 1);
        PDRectangle pageSize = new PDRectangle((float) graph.canvas().width(), (float) graph.canvas().height());
        List<PDPage> pages = new ArrayList<>(pageCount);
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            PDPage page = new PDPage(pageSize);
            document.addPage(page);
            pages.add(page);
        }
        return List.copyOf(pages);
    }

    private void renderFragment(PlacedFragment fragment,
                                PdfRenderEnvironment environment,
                                Map<String, Map<Integer, PdfGuideLinesRenderer.Bounds>> ownerBounds,
                                Set<String> labelKeys) throws Exception {
        Object payload = fragment.payload();
        PdfFragmentRenderHandler<Object> handler = handlerFor(payload);
        handler.render(fragment, payload, environment);
        finishRenderedFragment(fragment, payload, environment, ownerBounds, labelKeys);
    }

    private void finishRenderedFragment(PlacedFragment fragment,
                                        Object payload,
                                        PdfRenderEnvironment environment,
                                        Map<String, Map<Integer, PdfGuideLinesRenderer.Bounds>> ownerBounds,
                                        Set<String> labelKeys) throws Exception {
        if (payload instanceof ParagraphFragmentPayload paragraphPayload) {
            addParagraphLinks(fragment, paragraphPayload, environment);
        }
        if (payload instanceof PdfSemanticFragmentPayload semanticPayload) {
            // Paragraph-level link emission is handled above with per-line
            // rects tight to the rendered text (alignment-aware). Other
            // semantic payloads (shapes, table rows) still use the full
            // fragment rect as their clickable area.
            if (semanticPayload.linkOptions() != null && !(payload instanceof ParagraphFragmentPayload)) {
                PdfLinkAnnotationWriter.addUriLink(
                        environment.document().getPage(fragment.pageIndex()),
                        new PdfLinkAnnotationWriter.PlacedPdfRect(fragment.x(), fragment.y(), fragment.width(), fragment.height()),
                        semanticPayload.linkOptions());
            }
            if (semanticPayload.bookmarkOptions() != null) {
                environment.registerBookmark(fragment, semanticPayload.bookmarkOptions());
            }
        }
        if (debug.showGuides()) {
            PdfGuideLinesRenderer.draw(fragment, payload, environment, ownerBounds);
        }
        if (debug.showNodeLabels()) {
            PdfNodeLabelRenderer.draw(fragment, environment, ownerBounds, labelKeys, debug.labelText());
        }
    }

    private void addParagraphLinks(PlacedFragment fragment,
                                   ParagraphFragmentPayload payload,
                                   PdfRenderEnvironment environment) throws Exception {
        var paragraphLink = payload.linkOptions();
        double innerX = fragment.x() + payload.padding().left();
        double innerWidth = Math.max(0.0, fragment.width() - payload.padding().horizontal());
        double contentTop = fragment.y() + fragment.height() - payload.padding().top();

        double cursorTop = contentTop;
        for (int lineIndex = 0; lineIndex < payload.lines().size(); lineIndex++) {
            ParagraphLine line = payload.lines().get(lineIndex);
            double lineTop = cursorTop;
            double resolvedLineHeight = line.lineHeight();
            double lineX = switch (payload.align()) {
                case RIGHT -> innerX + innerWidth - line.width();
                case CENTER -> innerX + (innerWidth - line.width()) / 2.0;
                case LEFT -> innerX;
            };

            // Paragraph-level link covers each rendered line tightly. Without
            // this, right- or center-aligned paragraphs leaked clickable area
            // across the empty alignment gap, so neighbouring contact rows
            // (LinkedIn / GitHub icon paragraphs) hijacked each other's
            // clicks.
            if (paragraphLink != null && line.width() > 0.0) {
                PdfLinkAnnotationWriter.addUriLink(
                        environment.document().getPage(fragment.pageIndex()),
                        new PdfLinkAnnotationWriter.PlacedPdfRect(
                                lineX,
                                lineTop - resolvedLineHeight,
                                line.width(),
                                resolvedLineHeight),
                        paragraphLink);
            }

            double spanX = lineX;
            for (ParagraphSpan span : line.spans()) {
                if (span.linkOptions() != null && span.width() > 0.0) {
                    PdfLinkAnnotationWriter.PlacedPdfRect rect = spanLinkRectangle(
                            span,
                            spanX,
                            lineTop,
                            resolvedLineHeight,
                            line.textAscent(),
                            line.baselineOffsetFromBottom());
                    PdfLinkAnnotationWriter.addUriLink(
                            environment.document().getPage(fragment.pageIndex()),
                            rect,
                            span.linkOptions());
                }
                spanX += span.width();
            }
            cursorTop = lineTop - resolvedLineHeight - payload.lineGap();
        }
    }

    @SuppressWarnings("unchecked")
    private PdfFragmentRenderHandler<Object> handlerFor(Object payload) {
        if (payload == null) {
            throw new UnsupportedNodeCapabilityException("PDF backend does not support null fragment payloads.");
        }

        PdfFragmentRenderHandler<?> direct = handlers.get(payload.getClass());
        if (direct != null) {
            return (PdfFragmentRenderHandler<Object>) direct;
        }

        if (payload instanceof ParagraphFragmentPayload) {
            return (PdfFragmentRenderHandler<Object>) handlers.get(ParagraphFragmentPayload.class);
        }
        if (payload instanceof ShapeFragmentPayload) {
            return (PdfFragmentRenderHandler<Object>) handlers.get(ShapeFragmentPayload.class);
        }
        if (payload instanceof ImageFragmentPayload) {
            return (PdfFragmentRenderHandler<Object>) handlers.get(ImageFragmentPayload.class);
        }
        if (payload instanceof BarcodeFragmentPayload) {
            return (PdfFragmentRenderHandler<Object>) handlers.get(BarcodeFragmentPayload.class);
        }
        if (payload instanceof TableRowFragmentPayload) {
            return (PdfFragmentRenderHandler<Object>) handlers.get(TableRowFragmentPayload.class);
        }

        throw new UnsupportedNodeCapabilityException("PDF backend does not support fragment payload: " + payload.getClass().getName());
    }

    /**
     * Fluent builder for PDF-specific render options.
     */
    public static final class Builder {
        private final List<PdfHeaderFooterOptions> headerFooterOptions = new ArrayList<>();
        private final List<PdfFragmentRenderHandler<?>> additionalHandlers = new ArrayList<>();
        private PdfDebugOptions debug = PdfDebugOptions.none();
        private PdfMetadataOptions metadataOptions;
        private PdfWatermarkOptions watermarkOptions;
        private PdfProtectionOptions protectionOptions;

        private Builder() {
        }

        /**
         * Registers a custom {@link PdfFragmentRenderHandler}.
         *
         * <p>If the supplied handler reports a {@link PdfFragmentRenderHandler#payloadType()
         * payload type} that is already covered by a built-in default, the
         * custom handler replaces the default for the resulting backend
         * instance. Adding two custom handlers for the same payload type is
         * not supported &mdash; the second call rejects the duplicate.</p>
         *
         * <p>This method is the canonical extension point for adding new
         * payload types or overriding built-in rendering behaviour without
         * forking the backend.</p>
         *
         * @param handler non-{@code null} handler implementation
         * @return this builder
         * @throws IllegalArgumentException if {@code handler} reports the same
         *                                  payload type as another custom handler already registered
         *                                  on this builder
         * @since 1.6.0
         */
        public Builder addHandler(PdfFragmentRenderHandler<?> handler) {
            Objects.requireNonNull(handler, "handler");
            for (PdfFragmentRenderHandler<?> existing : additionalHandlers) {
                if (existing.payloadType().equals(handler.payloadType())) {
                    throw new IllegalArgumentException(
                            "Duplicate custom PDF handler for payload type "
                            + handler.payloadType().getName()
                            + "; remove the previous addHandler() call before registering another");
                }
            }
            this.additionalHandlers.add(handler);
            return this;
        }

        /**
         * Enables or disables guide-line overlays in rendered PDFs.
         *
         * <p>Convenience switch equivalent to toggling
         * {@link PdfDebugOptions#withGuides(boolean)} on the current debug
         * configuration; node-label settings made via {@link #debug(PdfDebugOptions)}
         * are preserved.</p>
         *
         * @param enabled {@code true} to draw guide lines
         * @return this builder
         */
        public Builder guideLines(boolean enabled) {
            this.debug = this.debug.withGuides(enabled);
            return this;
        }

        /**
         * Configures debug overlays (guide lines and semantic node labels).
         *
         * <p>Replaces the whole debug configuration; {@code null} resets to
         * {@link PdfDebugOptions#none()}.</p>
         *
         * @param options debug overlay options, or {@code null} to disable all
         * @return this builder
         * @since 1.8.0
         */
        public Builder debug(PdfDebugOptions options) {
            this.debug = options == null ? PdfDebugOptions.none() : options;
            return this;
        }

        /**
         * Configures PDF metadata.
         *
         * @param options metadata options, or {@code null} to clear
         * @return this builder
         */
        public Builder metadata(PdfMetadataOptions options) {
            this.metadataOptions = options;
            return this;
        }

        /**
         * Configures a document-wide PDF watermark.
         *
         * @param options watermark options, or {@code null} to clear
         * @return this builder
         */
        public Builder watermark(PdfWatermarkOptions options) {
            this.watermarkOptions = options;
            return this;
        }

        /**
         * Configures PDF protection and permissions.
         *
         * @param options protection options, or {@code null} to clear
         * @return this builder
         */
        public Builder protect(PdfProtectionOptions options) {
            this.protectionOptions = options;
            return this;
        }

        /**
         * Registers a repeating PDF page header.
         *
         * @param options header options
         * @return this builder
         */
        public Builder header(PdfHeaderFooterOptions options) {
            this.headerFooterOptions.add(Objects.requireNonNull(options, "options")
                    .withZone(PdfHeaderFooterZone.HEADER));
            return this;
        }

        /**
         * Registers a repeating PDF page footer.
         *
         * @param options footer options
         * @return this builder
         */
        public Builder footer(PdfHeaderFooterOptions options) {
            this.headerFooterOptions.add(Objects.requireNonNull(options, "options")
                    .withZone(PdfHeaderFooterZone.FOOTER));
            return this;
        }

        /**
         * Creates an immutable PDF backend instance with the configured options.
         *
         * <p>If any handlers were registered via
         * {@link #addHandler(PdfFragmentRenderHandler)}, they are merged with
         * the built-in defaults: a custom handler whose payload type matches
         * a default replaces the default, and a non-matching custom handler
         * extends the registry.</p>
         *
         * @return configured PDF fixed-layout backend
         */
        public PdfFixedLayoutBackend build() {
            return new PdfFixedLayoutBackend(
                    mergeHandlers(defaultHandlers(), additionalHandlers),
                    debug,
                    metadataOptions,
                    watermarkOptions,
                    protectionOptions,
                    headerFooterOptions);
        }
    }
}
