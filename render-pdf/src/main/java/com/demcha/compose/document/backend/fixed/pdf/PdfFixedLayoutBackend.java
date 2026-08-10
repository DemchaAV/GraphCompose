package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.api.Beta;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderer;
import com.demcha.compose.document.backend.fixed.SectionUnit;
import com.demcha.compose.document.backend.fixed.pdf.handlers.*;
import com.demcha.compose.document.backend.fixed.pdf.options.*;
import com.demcha.compose.document.exceptions.UnsupportedNodeCapabilityException;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;
import com.demcha.compose.document.layout.payloads.*;
import com.demcha.compose.document.node.DocumentLinkTarget;
import com.demcha.compose.document.node.ExternalLinkTarget;
import com.demcha.compose.document.node.InternalLinkTarget;
import com.demcha.compose.document.output.DocumentDebugOptions;
import com.demcha.compose.font.FontLibrary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import org.apache.pdfbox.Loader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
public final class PdfFixedLayoutBackend implements FixedLayoutRenderer {
    private static final Logger RENDER_LOG = LoggerFactory.getLogger("com.demcha.compose.engine.render");

    private final Map<Class<?>, PdfFragmentRenderHandler<?>> handlers;
    private final DocumentDebugOptions debug;
    private final PdfMetadataOptions metadataOptions;
    private final PdfWatermarkOptions watermarkOptions;
    private final PdfProtectionOptions protectionOptions;
    private final PdfViewerPreferencesOptions viewerPreferencesOptions;
    private final List<PdfHeaderFooterOptions> headerFooterOptions;

    /**
     * Default timestamp for {@link Builder#deterministic(boolean) deterministic(true)}
     * when no explicit instant is supplied. Any fixed constant works; a pinned epoch
     * is what keeps the output byte-identical across runs.
     */
    private static final Instant DEFAULT_DETERMINISTIC_INSTANT = Instant.parse("2000-01-01T00:00:00Z");

    /**
     * When non-null, deterministic output is on: the document CreationDate / ModDate
     * are pinned to this instant and the PDF {@code /ID} is derived from the document
     * metadata rather than PDFBox's time-seeded default, so the same document renders
     * to byte-identical output across runs ({@code null} = off).
     */
    private final Instant deterministicTimestamp;

    /**
     * Creates a backend with the built-in paragraph, shape, image, and table handlers.
     */
    public PdfFixedLayoutBackend() {
        this(defaultHandlers(), DocumentDebugOptions.none(), null, null, null, null, List.of(), null);
    }

    PdfFixedLayoutBackend(Collection<? extends PdfFragmentRenderHandler<?>> handlers) {
        this(handlers, DocumentDebugOptions.none(), null, null, null, null, List.of(), null);
    }

    private PdfFixedLayoutBackend(Collection<? extends PdfFragmentRenderHandler<?>> handlers,
                                  DocumentDebugOptions debug,
                                  PdfMetadataOptions metadataOptions,
                                  PdfWatermarkOptions watermarkOptions,
                                  PdfProtectionOptions protectionOptions,
                                  PdfViewerPreferencesOptions viewerPreferencesOptions,
                                  Collection<PdfHeaderFooterOptions> headerFooterOptions,
                                  Instant deterministicTimestamp) {
        Map<Class<?>, PdfFragmentRenderHandler<?>> registry = new LinkedHashMap<>();
        for (PdfFragmentRenderHandler<?> handler : handlers) {
            PdfFragmentRenderHandler<?> previous = registry.put(handler.payloadType(), Objects.requireNonNull(handler, "handler"));
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate PDF handler for payload type " + handler.payloadType().getName());
            }
        }
        this.handlers = Map.copyOf(registry);
        this.debug = debug == null ? DocumentDebugOptions.none() : debug;
        this.metadataOptions = metadataOptions;
        this.watermarkOptions = watermarkOptions;
        this.protectionOptions = protectionOptions;
        this.viewerPreferencesOptions = viewerPreferencesOptions;
        this.headerFooterOptions = List.copyOf(headerFooterOptions);
        this.deterministicTimestamp = deterministicTimestamp;
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
                new PdfPathFragmentRenderHandler(),
                new PdfImageFragmentRenderHandler(),
                new PdfTableRowFragmentRenderHandler(),
                new PdfShapeClipBeginRenderHandler(),
                new PdfShapeClipEndRenderHandler(),
                new PdfTransformBeginRenderHandler(),
                new PdfTransformEndRenderHandler(),
                new PdfAnchorMarkerRenderHandler(),
                new PdfBookmarkMarkerRenderHandler());
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
        } else if (span instanceof com.demcha.compose.document.layout.payloads.ParagraphSvgSpan svgSpan) {
            alignment = svgSpan.alignment();
            graphicHeight = svgSpan.height();
            baselineOffset = svgSpan.baselineOffset();
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
    @Override
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
        try (PDDocument document = buildDocument(graph, context)) {
            document.save(output);
            return document.getNumberOfPages();
        }
    }

    /**
     * Renders the document to one rasterized image per page (or a single page
     * when {@code pageIndex >= 0}). The document is saved to an in-memory buffer
     * and reloaded before rasterization: PDFBox writes embedded font subsets only
     * during {@code save()}, so rendering the unsaved {@link PDDocument} draws
     * documents that use binary font families (any non-standard-14 file) with
     * fallback glyphs instead of the embedded program. Standard-14-only documents
     * pay the extra serialization too — correctness over the marginal copy; the
     * rasterization itself dominates the cost.
     *
     * @param graph       resolved layout graph
     * @param context     fixed-layout render configuration (output stream/file ignored)
     * @param dpi         target resolution in dots per inch (72 = native)
     * @param transparent {@code true} for an ARGB image (transparent background), {@code false} for opaque RGB
     * @param pageIndex   zero-based page to render, or a negative value for all pages
     * @return one image per rendered page, in page order
     * @throws Exception                 if PDF creation, rendering, or rasterization fails
     * @throws IndexOutOfBoundsException if {@code pageIndex} is out of range
     */
    @Override
    public List<BufferedImage> renderToImages(LayoutGraph graph,
                                              FixedLayoutRenderContext context,
                                              int dpi,
                                              boolean transparent,
                                              int pageIndex) throws Exception {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(context, "context");
        byte[] documentBytes;
        try (PDDocument document = buildDocument(graph, context);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            document.save(buffer);
            documentBytes = buffer.toByteArray();
        }
        try (PDDocument document = Loader.loadPDF(documentBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            ImageType imageType = transparent ? ImageType.ARGB : ImageType.RGB;
            int pageCount = document.getNumberOfPages();
            if (pageIndex >= 0) {
                if (pageIndex >= pageCount) {
                    throw new IndexOutOfBoundsException(
                            "pageIndex " + pageIndex + " is out of bounds for " + pageCount + " page(s)");
                }
                return List.of(renderer.renderImageWithDPI(pageIndex, (float) dpi, imageType));
            }
            List<BufferedImage> images = new ArrayList<>(pageCount);
            for (int page = 0; page < pageCount; page++) {
                images.add(renderer.renderImageWithDPI(page, (float) dpi, imageType));
            }
            return images;
        }
    }

    /**
     * Builds the fully-rendered, post-processed {@link PDDocument} (pages drawn,
     * links and bookmarks resolved, metadata / watermark / protection /
     * header-footer applied) but does NOT save or close it — the caller owns the
     * returned open document. On any build failure the document is closed and the
     * exception rethrown, so the resource never leaks.
     */
    private PDDocument buildDocument(LayoutGraph graph, FixedLayoutRenderContext context) throws Exception {
        PDDocument document = new PDDocument();
        try {
            FontLibrary fonts = PdfFontLibraryFactory.library(document, context.customFontFamilies());
            List<PDPage> pages = createPages(document, graph);

            try (PdfRenderSession session = new PdfRenderSession(document, pages)) {
                PdfRenderEnvironment environment = new PdfRenderEnvironment(document, fonts, session);
                renderGraph(graph, environment);
                PdfBookmarkOutlineWriter.apply(document, environment.bookmarkRecords());
                // Pass B of internal-link resolution: every anchor is now placed,
                // so deferred go-to links (incl. forward references) can resolve.
                PdfInternalLinkWriter.apply(
                        document,
                        environment.anchorDestinations(),
                        environment.deferredInternalLinks());
            }

            PdfDocumentPostProcessor.apply(
                    document,
                    context.canvas(),
                    metadataOptions,
                    watermarkOptions,
                    protectionOptions,
                    headerFooterOptions);
            PdfDocumentPostProcessor.applyViewerPreferences(document, viewerPreferencesOptions);
            if (deterministicTimestamp != null) {
                PdfDeterminismWriter.apply(document, deterministicTimestamp);
            }

            return document;
        } catch (Exception ex) {
            document.close();
            throw ex;
        }
    }

    /**
     * Paints every fragment of one graph onto the current render environment's
     * pages, in fragment order, grouping table rows so fills land beneath
     * borders and text. Shared by the single-section and multi-section paths.
     */
    private void renderGraph(LayoutGraph graph, PdfRenderEnvironment environment) throws Exception {
        Map<String, Map<Integer, PdfGuideLinesRenderer.Bounds>> ownerBounds = debug.enabled()
                ? PdfGuideLinesRenderer.computeOwnerBounds(graph.fragments())
                : Map.of();
        PdfFragmentRenderHandler<?> tableRowHandler = handlers.get(TableRowFragmentPayload.class);
        for (int index = 0; index < graph.fragments().size(); index++) {
            PlacedFragment fragment = graph.fragments().get(index);
            if (fragment.payload() instanceof TableRowFragmentPayload
                && tableRowHandler instanceof PdfTableRowFragmentRenderHandler tableHandler) {
                index = renderTableRowGroup(graph.fragments(), index, tableHandler, environment, ownerBounds);
                continue;
            }
            renderFragment(fragment, environment, ownerBounds);
        }
        // Node labels paint as one post-pass so badges always land on
        // top of the content they annotate, in deterministic order.
        if (debug.showNodeLabels()) {
            PdfNodeLabelRenderer.drawAll(ownerBounds, environment, debug.labelText());
        }
    }

    /**
     * Concatenates several {@link SectionUnit sections} into one PDF and returns its
     * bytes. Each section keeps its own page geometry, fonts, and chrome
     * (watermark, header/footer); anchors, links, and bookmarks resolve across
     * section boundaries against the combined document.
     *
     * <p>This is the low-level assembly seam;
     * {@link com.demcha.compose.document.api.MultiSectionDocument} (via
     * {@link com.demcha.compose.GraphCompose#documents()}) is the public entry
     * point that builds the {@link SectionUnit}s for you.</p>
     *
     * @param sections ordered, non-empty list of sections
     * @return rendered combined-document bytes
     * @throws Exception if PDF creation, rendering, or saving fails
     * @since 1.9.0
     */
    @Beta
    @Override
    public byte[] renderSections(List<SectionUnit> sections) throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            writeSections(sections, output);
            return output.toByteArray();
        }
    }

    /**
     * Concatenates several {@link SectionUnit sections} into one PDF written to the
     * caller-owned stream (never closed by the backend).
     *
     * @param sections ordered, non-empty list of sections
     * @param output   caller-owned output stream
     * @throws Exception if PDF creation, rendering, or saving fails
     * @since 1.9.0
     */
    @Beta
    @Override
    public void writeSections(List<SectionUnit> sections, OutputStream output) throws Exception {
        Objects.requireNonNull(output, "output");
        try (PDDocument document = buildSectionsDocument(sections)) {
            document.save(output);
        }
    }

    private PDDocument buildSectionsDocument(List<SectionUnit> sections) throws Exception {
        Objects.requireNonNull(sections, "sections");
        if (sections.isEmpty()) {
            throw new IllegalArgumentException("A multi-section document needs at least one section.");
        }
        PDDocument document = new PDDocument();
        try {
            FontLibrary fonts = PdfFontLibraryFactory.library(document, unionCustomFonts(sections));
            Map<String, PdfRenderEnvironment.AnchorDestination> anchors = new LinkedHashMap<>();
            List<PdfRenderEnvironment.DeferredInternalLink> links = new ArrayList<>();
            List<PdfRenderEnvironment.BookmarkRecord> bookmarks = new ArrayList<>();
            int pageOffset = 0;
            for (SectionUnit section : sections) {
                LayoutGraph graph = section.graph();
                PdfFixedLayoutBackend chrome = (PdfFixedLayoutBackend) section.chrome();
                List<PDPage> pages = createPages(document, graph);
                try (PdfRenderSession renderSession = new PdfRenderSession(document, pages)) {
                    // Each section renders with its OWN backend's handlers/debug, but
                    // records navigation against the combined document via the page offset.
                    PdfRenderEnvironment environment =
                            new PdfRenderEnvironment(document, fonts, renderSession, pageOffset);
                    chrome.renderGraph(graph, environment);
                    bookmarks.addAll(environment.bookmarkRecords());
                    links.addAll(environment.deferredInternalLinks());
                    environment.anchorDestinations().forEach((name, destination) -> {
                        if (anchors.put(name, destination) != null) {
                            RENDER_LOG.warn(
                                    "render.pdf.multisection.anchor.duplicate name={} — last section wins", name);
                        }
                    });
                }
                PdfDocumentPostProcessor.applySectionChrome(
                        document,
                        section.canvas(),
                        chrome.watermarkOptions,
                        chrome.headerFooterOptions,
                        pageOffset,
                        pages.size());
                pageOffset += pages.size();
            }
            // Every anchor is now placed, so cross-section go-to links and the
            // combined outline resolve in a single pass over the merged maps.
            PdfBookmarkOutlineWriter.apply(document, List.copyOf(bookmarks));
            PdfInternalLinkWriter.apply(document, Map.copyOf(anchors), List.copyOf(links));
            applyDocumentMetadataAndProtection(document, sections);
            if (deterministicTimestamp != null) {
                PdfDeterminismWriter.apply(document, deterministicTimestamp);
            }
            return document;
        } catch (Exception ex) {
            document.close();
            throw ex;
        }
    }

    private static void applyDocumentMetadataAndProtection(PDDocument document, List<SectionUnit> sections)
            throws IOException {
        // Metadata, protection, and viewer preferences are document-global in PDF;
        // the first section that declares each wins for the combined document.
        PdfMetadataOptions metadata = null;
        PdfProtectionOptions protection = null;
        PdfViewerPreferencesOptions viewerPreferences = null;
        for (SectionUnit section : sections) {
            PdfFixedLayoutBackend chrome = (PdfFixedLayoutBackend) section.chrome();
            if (metadata == null) {
                metadata = chrome.metadataOptions;
            }
            if (protection == null) {
                protection = chrome.protectionOptions;
            }
            if (viewerPreferences == null) {
                viewerPreferences = chrome.viewerPreferencesOptions;
            }
        }
        PdfDocumentPostProcessor.applyDocumentMetadataAndProtection(document, metadata, protection);
        PdfDocumentPostProcessor.applyViewerPreferences(document, viewerPreferences);
    }

    private static List<FontFamilyDefinition> unionCustomFonts(List<SectionUnit> sections) {
        Map<FontName, FontFamilyDefinition> byName = new LinkedHashMap<>();
        for (SectionUnit section : sections) {
            for (FontFamilyDefinition family : section.customFonts()) {
                byName.putIfAbsent(family.name(), family);
            }
        }
        return List.copyOf(byName.values());
    }

    private int renderTableRowGroup(List<PlacedFragment> fragments,
                                    int startIndex,
                                    PdfTableRowFragmentRenderHandler handler,
                                    PdfRenderEnvironment environment,
                                    Map<String, Map<Integer, PdfGuideLinesRenderer.Bounds>> ownerBounds) throws Exception {
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
            finishRenderedFragment(fragment, payload, environment, ownerBounds);
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
                                Map<String, Map<Integer, PdfGuideLinesRenderer.Bounds>> ownerBounds) throws Exception {
        Object payload = fragment.payload();
        PdfFragmentRenderHandler<Object> handler = handlerFor(payload);
        handler.render(fragment, payload, environment);
        finishRenderedFragment(fragment, payload, environment, ownerBounds);
    }

    private void finishRenderedFragment(PlacedFragment fragment,
                                        Object payload,
                                        PdfRenderEnvironment environment,
                                        Map<String, Map<Integer, PdfGuideLinesRenderer.Bounds>> ownerBounds) throws Exception {
        if (payload instanceof ParagraphFragmentPayload paragraphPayload) {
            addParagraphLinks(fragment, paragraphPayload, environment);
        }
        if (payload instanceof PdfSemanticFragmentPayload semanticPayload) {
            // Paragraph-level link emission is handled above with per-line
            // rects tight to the rendered text (alignment-aware). Other
            // semantic payloads (shapes, table rows) still use the full
            // fragment rect as their clickable area.
            if (semanticPayload.linkTarget() != null && !(payload instanceof ParagraphFragmentPayload)) {
                emitLinkTarget(
                        environment,
                        fragment.pageIndex(),
                        new PdfLinkAnnotationWriter.PlacedPdfRect(fragment.x(), fragment.y(), fragment.width(), fragment.height()),
                        semanticPayload.linkTarget());
            }
            if (semanticPayload.bookmarkOptions() != null) {
                environment.registerBookmark(fragment, semanticPayload.bookmarkOptions());
            }
        }
        if (debug.showGuides()) {
            PdfGuideLinesRenderer.draw(fragment, payload, environment, ownerBounds);
        }
    }

    /**
     * Emits a link target on the resolved rectangle: external URIs are written
     * inline as {@code URI} annotations, while internal anchor links are
     * deferred for go-to resolution once every anchor is placed.
     */
    private void emitLinkTarget(PdfRenderEnvironment environment,
                                int pageIndex,
                                PdfLinkAnnotationWriter.PlacedPdfRect rectangle,
                                DocumentLinkTarget target) throws IOException {
        if (target instanceof ExternalLinkTarget external) {
            PdfLinkAnnotationWriter.addUriLink(
                    environment.documentPage(pageIndex),
                    rectangle,
                    external.options());
        } else if (target instanceof InternalLinkTarget internal) {
            environment.deferInternalLink(pageIndex, rectangle, internal.anchor());
        }
    }

    private void addParagraphLinks(PlacedFragment fragment,
                                   ParagraphFragmentPayload payload,
                                   PdfRenderEnvironment environment) throws Exception {
        var paragraphLink = payload.linkTarget();
        double innerX = fragment.x() + payload.padding().left();
        double innerWidth = Math.max(0.0, fragment.width() - payload.padding().horizontal());
        double contentTop = fragment.y() + fragment.height() - payload.padding().top();

        double cursorTop = contentTop;
        for (int lineIndex = 0; lineIndex < payload.lines().size(); lineIndex++) {
            ParagraphLine line = payload.lines().get(lineIndex);
            double lineTop = cursorTop;
            double resolvedLineHeight = line.lineHeight();
            double lineX = ParagraphLineGeometry.lineStartX(
                    payload.align(), innerX, innerWidth, line.width());

            // Paragraph-level link covers each rendered line tightly. Without
            // this, right- or center-aligned paragraphs leaked clickable area
            // across the empty alignment gap, so neighbouring contact rows
            // (LinkedIn / GitHub icon paragraphs) hijacked each other's
            // clicks.
            if (paragraphLink != null && line.width() > 0.0) {
                emitLinkTarget(
                        environment,
                        fragment.pageIndex(),
                        new PdfLinkAnnotationWriter.PlacedPdfRect(
                                lineX,
                                lineTop - resolvedLineHeight,
                                line.width(),
                                resolvedLineHeight),
                        paragraphLink);
            }

            double spanX = lineX;
            // The same order the glyphs are drawn in: a clickable rectangle placed by
            // walking the logical order would sit where the span would have been in a
            // left-to-right line, which is somewhere else entirely on a reordered one.
            for (ParagraphSpan span : line.spansInVisualOrder()) {
                if (span.linkTarget() != null && span.width() > 0.0) {
                    PdfLinkAnnotationWriter.PlacedPdfRect rect = spanLinkRectangle(
                            span,
                            spanX,
                            lineTop,
                            resolvedLineHeight,
                            line.textAscent(),
                            line.baselineOffsetFromBottom());
                    emitLinkTarget(environment, fragment.pageIndex(), rect, span.linkTarget());
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
        private DocumentDebugOptions debug = DocumentDebugOptions.none();
        private PdfMetadataOptions metadataOptions;
        private PdfWatermarkOptions watermarkOptions;
        private PdfProtectionOptions protectionOptions;
        private PdfViewerPreferencesOptions viewerPreferencesOptions;
        private Instant deterministicTimestamp;

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
         * {@link DocumentDebugOptions#withGuides(boolean)} on the current debug
         * configuration; node-label settings made via {@link #debug(DocumentDebugOptions)}
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
         * {@link DocumentDebugOptions#none()}.</p>
         *
         * @param options debug overlay options, or {@code null} to disable all
         * @return this builder
         * @since 1.8.0
         */
        public Builder debug(DocumentDebugOptions options) {
            this.debug = options == null ? DocumentDebugOptions.none() : options;
            return this;
        }

        /**
         * Configures PDF viewer preferences (page mode / layout / window flags).
         *
         * @param options viewer-preference options, or {@code null} to clear
         * @return this builder
         */
        public Builder viewerPreferences(PdfViewerPreferencesOptions options) {
            this.viewerPreferencesOptions = options;
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
         * Enables (or disables) deterministic output. When enabled, the document
         * CreationDate / ModDate are pinned to a fixed default timestamp and the
         * PDF {@code /ID} is derived from the document metadata instead of PDFBox's
         * time-seeded default, so the same document renders to byte-identical output
         * across runs — for reproducible builds and byte-level output tests. Disabled
         * by default.
         *
         * <p>Two documents whose metadata is identical share an {@code /ID} (it is
         * derived from the info dictionary, not the page content). PDF encryption via
         * {@link #protect(PdfProtectionOptions)} can reintroduce randomness — AES-256
         * uses random salts — so an encrypted document is not byte-reproducible even
         * with this enabled.</p>
         *
         * @param enabled {@code true} to pin output at the default timestamp,
         *                {@code false} to keep PDFBox's live timestamp and {@code /ID}
         * @return this builder
         * @since 2.0.0
         */
        @Beta
        public Builder deterministic(boolean enabled) {
            this.deterministicTimestamp = enabled ? DEFAULT_DETERMINISTIC_INSTANT : null;
            return this;
        }

        /**
         * Enables deterministic output with an explicit timestamp for the document
         * CreationDate / ModDate. See {@link #deterministic(boolean)}.
         *
         * <p>The instant is truncated to whole seconds: PDF dates carry second
         * precision, so truncating up front keeps the serialized dates and the
         * derived {@code /ID} in agreement for sub-second inputs.</p>
         *
         * @param timestamp the instant to pin CreationDate / ModDate to
         * @return this builder
         * @throws NullPointerException if {@code timestamp} is null
         * @since 2.0.0
         */
        @Beta
        public Builder deterministic(Instant timestamp) {
            this.deterministicTimestamp =
                    Objects.requireNonNull(timestamp, "timestamp").truncatedTo(ChronoUnit.SECONDS);
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
                    viewerPreferencesOptions,
                    headerFooterOptions,
                    deterministicTimestamp);
        }
    }
}
