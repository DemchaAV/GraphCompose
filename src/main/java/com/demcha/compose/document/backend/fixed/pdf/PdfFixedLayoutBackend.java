package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.backend.fixed.FixedLayoutBackend;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext;
import com.demcha.compose.document.backend.fixed.pdf.handlers.PdfBarcodeFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.handlers.PdfImageFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.handlers.PdfParagraphFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.handlers.PdfShapeFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.handlers.PdfTableRowFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterZone;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfMetadataOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfProtectionOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkOptions;
import com.demcha.compose.document.exceptions.UnsupportedNodeCapabilityException;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.font.FontLibrary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final boolean guideLines;
    private final PdfMetadataOptions metadataOptions;
    private final PdfWatermarkOptions watermarkOptions;
    private final PdfProtectionOptions protectionOptions;
    private final List<PdfHeaderFooterOptions> headerFooterOptions;

    /**
     * Creates a backend with the built-in paragraph, shape, image, and table handlers.
     */
    public PdfFixedLayoutBackend() {
        this(defaultHandlers(), false, null, null, null, List.of());
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
                new PdfImageFragmentRenderHandler(),
                new PdfTableRowFragmentRenderHandler());
    }

    PdfFixedLayoutBackend(Collection<? extends PdfFragmentRenderHandler<?>> handlers) {
        this(handlers, false, null, null, null, List.of());
    }

    private PdfFixedLayoutBackend(Collection<? extends PdfFragmentRenderHandler<?>> handlers,
                                  boolean guideLines,
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
        this.guideLines = guideLines;
        this.metadataOptions = metadataOptions;
        this.watermarkOptions = watermarkOptions;
        this.protectionOptions = protectionOptions;
        this.headerFooterOptions = List.copyOf(headerFooterOptions);
    }

    @Override
    public String name() {
        return "pdf-fixed-layout";
    }

    /**
     * Renders the resolved layout graph into PDF bytes and optionally writes the
     * result to the configured output file.
     *
     * @param graph resolved layout graph produced by the semantic compiler
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
                "render.pdf.fixed.start pages={} fragments={} outputConfigured={} streamConfigured={} guideLines={}",
                graph.totalPages(),
                graph.fragments().size(),
                context.outputFile() != null,
                context.outputStream() != null,
                guideLines);
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
     * @param graph resolved layout graph produced by the semantic compiler
     * @param context fixed-layout render configuration with a non-null output stream
     * @throws Exception if PDF creation, rendering, or saving fails
     */
    public void write(LayoutGraph graph, FixedLayoutRenderContext context) throws Exception {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(context, "context");
        OutputStream output = Objects.requireNonNull(context.outputStream(), "context.outputStream");

        long startNanos = System.nanoTime();
        RENDER_LOG.debug(
                "render.pdf.fixed.stream.start pages={} fragments={} outputConfigured={} guideLines={}",
                graph.totalPages(),
                graph.fragments().size(),
                context.outputFile() != null,
                guideLines);
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
                for (PlacedFragment fragment : graph.fragments()) {
                    renderFragment(fragment, environment, guideLines);
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

    private void renderFragment(PlacedFragment fragment, PdfRenderEnvironment environment, boolean guideLines) throws Exception {
        Object payload = fragment.payload();
        PdfFragmentRenderHandler<Object> handler = handlerFor(payload);
        handler.render(fragment, payload, environment);
        if (payload instanceof BuiltInNodeDefinitions.ParagraphFragmentPayload paragraphPayload) {
            addParagraphSpanLinks(fragment, paragraphPayload, environment);
        }
        if (payload instanceof BuiltInNodeDefinitions.PdfSemanticFragmentPayload semanticPayload) {
            if (semanticPayload.linkOptions() != null) {
                PdfLinkAnnotationWriter.addUriLink(
                        environment.document().getPage(fragment.pageIndex()),
                        new PdfLinkAnnotationWriter.PlacedPdfRect(fragment.x(), fragment.y(), fragment.width(), fragment.height()),
                        semanticPayload.linkOptions());
            }
            if (semanticPayload.bookmarkOptions() != null) {
                environment.registerBookmark(fragment, semanticPayload.bookmarkOptions());
            }
        }
        if (guideLines) {
            PdfGuideLinesRenderer.draw(fragment, payload, environment);
        }
    }

    private void addParagraphSpanLinks(PlacedFragment fragment,
                                       BuiltInNodeDefinitions.ParagraphFragmentPayload payload,
                                       PdfRenderEnvironment environment) throws Exception {
        double innerX = fragment.x() + payload.padding().left();
        double innerWidth = Math.max(0.0, fragment.width() - payload.padding().horizontal());
        double contentTop = fragment.y() + fragment.height() - payload.padding().top();

        for (int lineIndex = 0; lineIndex < payload.lines().size(); lineIndex++) {
            BuiltInNodeDefinitions.ParagraphLine line = payload.lines().get(lineIndex);
            double lineTop = contentTop - lineIndex * (payload.lineHeight() + payload.lineGap());
            double lineX = switch (payload.align()) {
                case RIGHT -> innerX + innerWidth - line.width();
                case CENTER -> innerX + (innerWidth - line.width()) / 2.0;
                case LEFT -> innerX;
            };
            double spanX = lineX;
            for (BuiltInNodeDefinitions.ParagraphSpan span : line.spans()) {
                if (span.linkOptions() != null && span.width() > 0.0) {
                    PdfLinkAnnotationWriter.addUriLink(
                            environment.document().getPage(fragment.pageIndex()),
                            new PdfLinkAnnotationWriter.PlacedPdfRect(
                                    spanX,
                                    lineTop - payload.lineHeight(),
                                    span.width(),
                                    payload.lineHeight()),
                            span.linkOptions());
                }
                spanX += span.width();
            }
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

        if (payload instanceof BuiltInNodeDefinitions.ParagraphFragmentPayload) {
            return (PdfFragmentRenderHandler<Object>) handlers.get(BuiltInNodeDefinitions.ParagraphFragmentPayload.class);
        }
        if (payload instanceof BuiltInNodeDefinitions.ShapeFragmentPayload) {
            return (PdfFragmentRenderHandler<Object>) handlers.get(BuiltInNodeDefinitions.ShapeFragmentPayload.class);
        }
        if (payload instanceof BuiltInNodeDefinitions.ImageFragmentPayload) {
            return (PdfFragmentRenderHandler<Object>) handlers.get(BuiltInNodeDefinitions.ImageFragmentPayload.class);
        }
        if (payload instanceof BuiltInNodeDefinitions.BarcodeFragmentPayload) {
            return (PdfFragmentRenderHandler<Object>) handlers.get(BuiltInNodeDefinitions.BarcodeFragmentPayload.class);
        }
        if (payload instanceof BuiltInNodeDefinitions.TableRowFragmentPayload) {
            return (PdfFragmentRenderHandler<Object>) handlers.get(BuiltInNodeDefinitions.TableRowFragmentPayload.class);
        }

        throw new UnsupportedNodeCapabilityException("PDF backend does not support fragment payload: " + payload.getClass().getName());
    }

    /**
     * Fluent builder for PDF-specific render options.
     */
    public static final class Builder {
        private boolean guideLines;
        private PdfMetadataOptions metadataOptions;
        private PdfWatermarkOptions watermarkOptions;
        private PdfProtectionOptions protectionOptions;
        private final List<PdfHeaderFooterOptions> headerFooterOptions = new ArrayList<>();

        private Builder() {
        }

        /**
         * Enables or disables guide-line overlays in rendered PDFs.
         *
         * @param enabled {@code true} to draw guide lines
         * @return this builder
         */
        public Builder guideLines(boolean enabled) {
            this.guideLines = enabled;
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
         * @return configured PDF fixed-layout backend
         */
        public PdfFixedLayoutBackend build() {
            return new PdfFixedLayoutBackend(
                    defaultHandlers(),
                    guideLines,
                    metadataOptions,
                    watermarkOptions,
                    protectionOptions,
                    headerFooterOptions);
        }
    }
}
