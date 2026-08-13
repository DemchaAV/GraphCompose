package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderer;
import com.demcha.compose.document.backend.fixed.SectionUnit;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxAnchorMarkerRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxBarcodeFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxBookmarkMarkerRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxEllipseFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxImageFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxPathFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxPolygonFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxShapeClipBeginRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxShapeClipEndRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxTransformBeginRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxTransformEndRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxLineFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxParagraphFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxShapeFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxChromeRenderer;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxTableRowFragmentRenderHandler;
import com.demcha.compose.document.api.Beta;
import com.demcha.compose.document.backend.fixed.pdf.PdfFixedLayoutBackend;
import com.demcha.compose.document.backend.fixed.pdf.PdfMeasurementResources;
import com.demcha.compose.document.backend.fixed.pdf.PdfOutputOptionsTranslator;
import com.demcha.compose.document.exceptions.UnsupportedNodeCapabilityException;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.AnchorMarkerPayload;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.PdfSemanticFragmentPayload;
import com.demcha.compose.document.layout.payloads.ShapeClipBeginPayload;
import com.demcha.compose.document.layout.payloads.ShapeClipEndPayload;
import com.demcha.compose.document.layout.payloads.TableRowFragmentPayload;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.output.DocumentWatermark;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * <p>The backend renders every fragment payload the layout compiler emits —
 * paragraphs (rich runs, chips, inline graphics), table rows, vector shapes,
 * lines, ellipses, polygons, free paths (with gradient fills and strokes),
 * images, barcodes, transform groups, and navigation markers (hyperlinks,
 * slide-jump internal links, bookmark slide names) — plus document chrome
 * (metadata, watermark, repeating headers/footers), multi-section
 * concatenation, and deterministic byte-identical output; clip regions render
 * through a pixel-exact raster fallback — see
 * {@code docs/architecture/backend-capability-matrix.md} for the live
 * per-capability status and documented deviations. Render-to-images is not
 * implemented and throws {@link UnsupportedOperationException}; rasterize
 * through the PDF backend, which shares the same resolved geometry.</p>
 *
 * <p><b>Thread-safety:</b> immutable and reusable across renders; each render
 * pass owns its own slide show and environment.</p>
 *
 * <p><b>Experimental</b> ({@code @Beta}): first release of the PPTX backend
 * (2.1.0) — this type's contract may still change in a minor release; see
 * {@code docs/api-stability.md}.</p>
 *
 * @since 2.1.0
 */
@Beta
public final class PptxFixedLayoutBackend implements FixedLayoutRenderer {

    private static final Logger RENDER_LOG = LoggerFactory.getLogger("com.demcha.compose.engine.render");

    private static final Instant DEFAULT_DETERMINISTIC_INSTANT = Instant.parse("2000-01-01T00:00:00Z");

    /**
     * Long edge, in pixels, a rasterized clip region aims for. Small regions are
     * upscaled towards it so the picture stays crisp on a projector.
     */
    private static final double CLIP_RASTER_TARGET_PIXELS = 2048.0;

    /** Upscale ceiling, so a tiny badge does not mint a needlessly heavy picture. */
    private static final double CLIP_RASTER_MAX_SCALE = 4.0;

    /**
     * Downscale floor. Without it, a clip region wider than
     * {@link #CLIP_RASTER_TARGET_PIXELS} points rasterizes <em>below</em> native
     * size — an A0-scale composite would land at ~44 DPI and read as visibly
     * blurry, since the picture is anchored at the full clip size regardless.
     */
    private static final double CLIP_RASTER_MIN_SCALE = 1.0;

    private final Map<Class<?>, PptxFragmentRenderHandler<?>> handlers;

    // Package-private: the deck assembly reads each section's chrome directly.
    final DocumentMetadata metadataOptions;
    final DocumentWatermark watermarkOptions;
    final List<DocumentHeaderFooter> headerFooterOptions;

    /**
     * Timestamp all output clocks pin to when deterministic output is enabled;
     * {@code null} keeps POI's wall-clock stamps.
     */
    final Instant deterministicTimestamp;

    /**
     * Raster-slide resolution in DPI; {@code 0} keeps the default editable
     * vector mode. Package-private for the deck assembly.
     */
    final int rasterSlidesDpi;

    /**
     * When {@code true} (the default), a clipped composite renders through the
     * PDF backend into a transparent picture, giving pixel-exact clipping.
     */
    private final boolean clipRasterFallback;
    /** Package-visible: the multi-section assembly reads it off the invoked backend. */
    final boolean embedBundledFonts;

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
        this.clipRasterFallback = builder.clipRasterFallback;
        this.embedBundledFonts = builder.embedBundledFonts;
        this.metadataOptions = builder.metadataOptions;
        this.watermarkOptions = builder.watermarkOptions;
        this.headerFooterOptions = List.copyOf(builder.headerFooterOptions);
        this.deterministicTimestamp = builder.deterministicTimestamp;
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
                new PptxParagraphFragmentRenderHandler(),
                new PptxTableRowFragmentRenderHandler(),
                new PptxImageFragmentRenderHandler(),
                new PptxBarcodeFragmentRenderHandler(),
                new PptxPolygonFragmentRenderHandler(),
                new PptxPathFragmentRenderHandler(),
                new PptxTransformBeginRenderHandler(),
                new PptxTransformEndRenderHandler(),
                new PptxShapeClipBeginRenderHandler(),
                new PptxShapeClipEndRenderHandler(),
                new PptxAnchorMarkerRenderHandler(),
                new PptxBookmarkMarkerRenderHandler());
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
     * Concatenates several {@link SectionUnit sections} into one .pptx and
     * returns its bytes. Each section renders through its own
     * {@link SectionUnit#chrome() chrome} backend — handlers, watermark,
     * header/footer with section-local page numbering, and raster-slide mode
     * are all per-section settings; only deck-global concerns come from the
     * backend this method is invoked on (deterministic output, bundled-font
     * embedding — a deck has one font table) or from the first section that
     * declares them (metadata), matching the PDF backend's combined-document
     * rules. Anchors, links, and slide names resolve across
     * section boundaries against the combined deck; a raster-slide section
     * still resolves incoming links and bookmarks, but its own outgoing
     * navigation is baked into the pixels. A PPTX deck carries one slide
     * size, so every section must share the same page canvas.
     *
     * @param sections ordered, non-empty list of sections
     * @return rendered combined-deck bytes
     * @throws Exception                     if rendering or saving fails
     * @throws UnsupportedOperationException if sections declare differing page sizes
     * @since 2.1.0
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
     * Concatenates several {@link SectionUnit sections} into one .pptx written
     * to the caller-owned stream (never closed by the backend). See
     * {@link #renderSections(List)} for the shared-slide-size constraint.
     *
     * @param sections ordered, non-empty list of sections
     * @param output   caller-owned output stream
     * @throws Exception                     if rendering or saving fails
     * @throws UnsupportedOperationException if sections declare differing page sizes
     * @since 2.1.0
     */
    @Beta
    @Override
    public void writeSections(List<SectionUnit> sections, OutputStream output) throws Exception {
        Objects.requireNonNull(sections, "sections");
        Objects.requireNonNull(output, "output");
        if (sections.isEmpty()) {
            throw new IllegalArgumentException("A multi-section document needs at least one section.");
        }
        LayoutCanvas firstCanvas = sections.get(0).canvas();
        for (int index = 0; index < sections.size(); index++) {
            SectionUnit section = sections.get(index);
            if (!(section.chrome() instanceof PptxFixedLayoutBackend)) {
                throw new IllegalArgumentException(
                        "Section " + index + " carries a "
                        + section.chrome().getClass().getSimpleName()
                        + " chrome; every section of a PPTX multi-section render must use a "
                        + "PptxFixedLayoutBackend.");
            }
            if (section.canvas().width() != firstCanvas.width()
                    || section.canvas().height() != firstCanvas.height()) {
                throw new UnsupportedOperationException(
                        "A PPTX deck carries one slide size, so every section must share the same "
                        + "page canvas; got " + firstCanvas.width() + "x" + firstCanvas.height()
                        + " and " + section.canvas().width() + "x" + section.canvas().height()
                        + " pt. Render differing page sizes through the PDF backend.");
            }
        }
        if (deterministicTimestamp == null) {
            PptxDeckAssembly.renderSections(sections, firstCanvas, this, output);
            return;
        }
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            PptxDeckAssembly.renderSections(sections, firstCanvas, this, buffer);
            writeFinishedDeck(buffer.toByteArray(), output);
        }
    }

    private void renderToOutput(LayoutGraph graph,
                                FixedLayoutRenderContext context,
                                OutputStream output) throws Exception {
        // Without determinism there is no normalization pass, so the deck
        // streams straight to the caller instead of paying a full-size buffer.
        if (deterministicTimestamp == null) {
            if (rasterSlidesDpi > 0) {
                renderRasterSlides(graph, context, output);
            } else {
                renderVectorSlides(graph, context, output);
            }
            return;
        }
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            if (rasterSlidesDpi > 0) {
                renderRasterSlides(graph, context, buffer);
            } else {
                renderVectorSlides(graph, context, buffer);
            }
            writeFinishedDeck(buffer.toByteArray(), output);
        }
    }

    /**
     * Applies the deterministic zip normalization (when enabled) and streams
     * the finished deck bytes.
     */
    private void writeFinishedDeck(byte[] deck, OutputStream output) throws IOException {
        if (deterministicTimestamp != null) {
            deck = PptxDeterminismWriter.normalizeZipEntries(deck, deterministicTimestamp);
        }
        output.write(deck);
    }

    private void renderVectorSlides(LayoutGraph graph,
                                    FixedLayoutRenderContext context,
                                    OutputStream output) throws Exception {
        try (XMLSlideShow show = new XMLSlideShow();
             PdfMeasurementResources measurement =
                     PdfMeasurementResources.open(context.customFontFamilies())) {
            PptxRenderSession session = new PptxRenderSession(
                    show, graph.canvas().width(), graph.canvas().height(), graph.totalPages());
            PptxRenderEnvironment environment =
                    new PptxRenderEnvironment(show, session, 0, graph.canvas().height(),
                            measurement.fontLibrary(), context.customFontFamilies());
            environment.embedsBundledFonts(embedBundledFonts);
            int pageCount = Math.max(graph.totalPages(), 1);
            PptxChromeRenderer.applyWatermarkBehindContent(
                    environment, watermarkOptions, graph.canvas(), pageCount);
            renderGraph(graph, environment, context);
            PptxChromeRenderer.applyWatermarkAboveContent(
                    environment, watermarkOptions, graph.canvas(), pageCount);
            PptxChromeRenderer.applyHeadersAndFooters(
                    environment, headerFooterOptions, graph.canvas(), pageCount);
            PptxNavigationWriter.apply(environment);
            environment.logRasterizedClipSummary();
            // After the last run is drawn, so only the families the deck actually used
            // are offered to the embedder.
            if (embedBundledFonts) {
                environment.embedBundledFontsUsed();
            }
            if (metadataOptions != null) {
                PptxDeckAssembly.applyMetadata(show, metadataOptions);
            }
            if (deterministicTimestamp != null) {
                PptxDeterminismWriter.pinCoreProperties(show, deterministicTimestamp);
            }
            show.write(output);
        }
    }

    /**
     * Raster-slide mode: every page is rendered through the PDF backend at the
     * configured DPI and placed as one full-slide picture — a pixel-exact copy
     * of the PDF/PNG output in .pptx form. Slides are not editable as text,
     * and visible chrome (watermark, headers/footers) is baked into the pixels
     * by the PDF backend; metadata still lands in the deck's core properties.
     */
    private void renderRasterSlides(LayoutGraph graph,
                                    FixedLayoutRenderContext context,
                                    OutputStream output) throws Exception {
        List<BufferedImage> pages = pdfBackendWithVisibleChrome()
                .renderToImages(graph, context, rasterSlidesDpi, false, -1);
        try (XMLSlideShow show = new XMLSlideShow()) {
            PptxRenderSession session = new PptxRenderSession(
                    show, graph.canvas().width(), graph.canvas().height(), graph.totalPages());
            PptxRenderEnvironment environment = new PptxRenderEnvironment(
                    show, session, 0, graph.canvas().height());
            PptxDeckAssembly.placeRasterPages(environment, pages, graph.canvas());
            if (metadataOptions != null) {
                PptxDeckAssembly.applyMetadata(show, metadataOptions);
            }
            if (deterministicTimestamp != null) {
                PptxDeterminismWriter.pinCoreProperties(show, deterministicTimestamp);
            }
            show.write(output);
        }
    }

    /**
     * Builds the PDF sub-backend raster renders go through, carrying this
     * backend's visible chrome so watermarks and headers/footers land in the
     * rasterized pixels.
     */
    PdfFixedLayoutBackend pdfBackendWithVisibleChrome() {
        if (watermarkOptions == null && headerFooterOptions.isEmpty()) {
            return new PdfFixedLayoutBackend();
        }
        PdfFixedLayoutBackend.Builder builder = PdfFixedLayoutBackend.builder()
                .watermark(PdfOutputOptionsTranslator.toPdf(watermarkOptions));
        for (DocumentHeaderFooter entry : headerFooterOptions) {
            if (entry.getZone() == DocumentHeaderFooterZone.FOOTER) {
                builder.footer(PdfOutputOptionsTranslator.toPdf(entry));
            } else {
                builder.header(PdfOutputOptionsTranslator.toPdf(entry));
            }
        }
        return builder.build();
    }

    /**
     * Paints every fragment in graph order, grouping contiguous table-row
     * fragments so every cell fill of a table lands beneath its borders and
     * text — the PDF backend's two-pass discipline.
     */
    void renderGraph(LayoutGraph graph,
                     PptxRenderEnvironment environment,
                     FixedLayoutRenderContext context) throws Exception {
        PptxFragmentRenderHandler<?> tableRowHandler =
                handlers.get(TableRowFragmentPayload.class);
        List<PlacedFragment> fragments = graph.fragments();
        for (int index = 0; index < fragments.size(); index++) {
            PlacedFragment fragment = fragments.get(index);
            if (fragment.payload() instanceof TableRowFragmentPayload
                    && tableRowHandler instanceof PptxTableRowFragmentRenderHandler tableHandler) {
                index = renderTableRowGroup(fragments, index, tableHandler, environment);
                continue;
            }
            if (clipRasterFallback
                    && fragment.payload() instanceof ShapeClipBeginPayload
                    && handlers.get(ShapeClipBeginPayload.class)
                            instanceof PptxShapeClipBeginRenderHandler) {
                // A custom clip handler registered via Builder.addHandler keeps
                // normal dispatch; only the built-in degrade path is replaced.
                index = renderClippedComposite(graph, fragments, index, environment, context);
                continue;
            }
            renderFragment(fragment, environment);
        }
    }

    /**
     * Renders one clip region — the fragments between a clip-begin marker and
     * its matching end — through the PDF backend into a transparent picture
     * placed at the clip bounds, so the clip is pixel-exact even though
     * DrawingML cannot express it. Fragment-level links, bookmarks, and
     * anchors inside the region are still recorded; run-level link hotspots
     * inside a rasterized composite are not emitted, and custom fragment
     * handlers do not apply inside it (the region renders through the PDF
     * backend's defaults). Returns the last consumed index.
     *
     * <p>When {@link PptxClipSafety} proves the clip cannot remove any ink —
     * the common defensive-clip case, e.g. a rounded card whose padded
     * content never reaches the corners — the raster fallback is skipped
     * entirely and the children render as native, editable shapes.</p>
     */
    private int renderClippedComposite(LayoutGraph graph,
                                       List<PlacedFragment> fragments,
                                       int beginIndex,
                                       PptxRenderEnvironment environment,
                                       FixedLayoutRenderContext context) throws Exception {
        PlacedFragment beginFragment = fragments.get(beginIndex);
        ShapeClipBeginPayload clipPayload = (ShapeClipBeginPayload) beginFragment.payload();
        String ownerPath = clipPayload.ownerPath();
        int endIndex = beginIndex + 1;
        while (endIndex < fragments.size()
                && !(fragments.get(endIndex).payload() instanceof ShapeClipEndPayload end
                        && Objects.equals(end.ownerPath(), ownerPath))) {
            endIndex++;
        }
        if (endIndex >= fragments.size()) {
            // Defensive: an unmatched begin falls back to the unclipped path.
            renderFragment(beginFragment, environment);
            return beginIndex;
        }
        if (beginFragment.width() <= 0 || beginFragment.height() <= 0) {
            recordRegionNavigation(fragments, beginIndex, endIndex, environment);
            return endIndex;
        }
        if (PptxClipSafety.clipCannotRemoveInk(
                clipPayload, beginFragment, fragments, beginIndex, endIndex)) {
            // The clip provably cuts nothing: consume only the begin marker
            // and let the main loop render the children as native, editable
            // shapes (nested clips and table groups re-dispatch normally; the
            // end marker is a silent no-op).
            return beginIndex;
        }

        // Translate the region into a clip-sized canvas and rasterize only the
        // clip box — the PDF backend applies the real clip, and the raster
        // never exceeds the capped clip size no matter how large the page is.
        List<PlacedFragment> region = new ArrayList<>(endIndex - beginIndex + 1);
        for (int index = beginIndex; index <= endIndex; index++) {
            PlacedFragment fragment = fragments.get(index);
            region.add(new PlacedFragment(fragment.path(), fragment.fragmentIndex(), 0,
                    fragment.x() - beginFragment.x(), fragment.y() - beginFragment.y(),
                    fragment.width(), fragment.height(),
                    fragment.margin(), fragment.padding(), fragment.payload()));
        }
        LayoutCanvas clipCanvas = new LayoutCanvas(
                beginFragment.width(), beginFragment.height(),
                beginFragment.width(), beginFragment.height(),
                com.demcha.compose.engine.components.style.Margin.of(0));
        LayoutGraph regionGraph = new LayoutGraph(clipCanvas, 1, List.of(), region);
        double longestEdge = Math.max(1.0,
                Math.max(beginFragment.width(), beginFragment.height()));
        double scale = Math.max(CLIP_RASTER_MIN_SCALE,
                Math.min(CLIP_RASTER_MAX_SCALE, CLIP_RASTER_TARGET_PIXELS / longestEdge));
        BufferedImage raster = new PdfFixedLayoutBackend()
                .renderToImages(regionGraph, context, (int) Math.round(72.0 * scale), true, 0)
                .get(0);
        environment.recordRasterizedClip(raster.getWidth(), raster.getHeight());
        byte[] png;
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            ImageIO.write(raster, "png", buffer);
            png = buffer.toByteArray();
        }
        XSLFPictureShape picture =
                environment.surface(beginFragment.pageIndex()).createPicture(
                        environment.slideShow().addPicture(png, PictureData.PictureType.PNG));
        picture.setAnchor(PptxCoordinates.anchorOf(environment.canvasHeight(), beginFragment));
        ((org.openxmlformats.schemas.presentationml.x2006.main.CTPicture) picture.getXmlObject())
                .getNvPicPr().getCNvPr().setName("GraphCompose Clipped Composite");

        recordRegionNavigation(fragments, beginIndex, endIndex, environment);
        return endIndex;
    }

    /** Records links, bookmarks, and anchors of a consumed clip region. */
    private void recordRegionNavigation(List<PlacedFragment> fragments,
                                        int beginIndex,
                                        int endIndex,
                                        PptxRenderEnvironment environment) {
        for (int index = beginIndex; index <= endIndex; index++) {
            PlacedFragment fragment = fragments.get(index);
            if (fragment.payload() instanceof AnchorMarkerPayload anchor) {
                environment.registerAnchor(fragment, anchor.anchor());
            } else {
                // Inside a rasterized composite no per-line hotspots were
                // emitted, so paragraph links keep their fragment rectangle.
                finishRenderedFragment(fragment, fragment.payload(), environment, true);
            }
        }
    }

    /**
     * Renders one contiguous run of same-table row fragments: all fills
     * first, then all borders and text. Returns the last consumed index.
     */
    private int renderTableRowGroup(List<PlacedFragment> fragments,
                                    int startIndex,
                                    PptxTableRowFragmentRenderHandler handler,
                                    PptxRenderEnvironment environment) {
        String tablePath = fragments.get(startIndex).path();
        int endExclusive = startIndex;
        while (endExclusive < fragments.size()
                && Objects.equals(fragments.get(endExclusive).path(), tablePath)
                && fragments.get(endExclusive).payload()
                        instanceof TableRowFragmentPayload) {
            endExclusive++;
        }
        for (int index = startIndex; index < endExclusive; index++) {
            PlacedFragment fragment = fragments.get(index);
            handler.renderFills(fragment,
                    (TableRowFragmentPayload) fragment.payload(),
                    environment);
        }
        for (int index = startIndex; index < endExclusive; index++) {
            PlacedFragment fragment = fragments.get(index);
            TableRowFragmentPayload payload =
                    (TableRowFragmentPayload) fragment.payload();
            handler.renderBordersAndText(fragment, payload, environment);
            finishRenderedFragment(fragment, payload, environment, false);
        }
        return endExclusive - 1;
    }

    private void renderFragment(PlacedFragment fragment, PptxRenderEnvironment environment) throws Exception {
        Object payload = fragment.payload();
        PptxFragmentRenderHandler<Object> handler = handlerFor(payload);
        handler.render(fragment, payload, environment);
        finishRenderedFragment(fragment, payload, environment, false);
    }

    /**
     * Generic post-render bookkeeping shared by every semantic payload:
     * fragment-rectangle links and bookmark records are collected here so no
     * individual handler has to remember navigation concerns. Paragraph link
     * rectangles are skipped on the normal path — the paragraph handler emits
     * tighter per-line hotspots itself — unless the caller says otherwise.
     */
    private void finishRenderedFragment(PlacedFragment fragment,
                                        Object payload,
                                        PptxRenderEnvironment environment,
                                        boolean includeParagraphLinks) {
        if (payload instanceof PdfSemanticFragmentPayload semanticPayload) {
            if (semanticPayload.linkTarget() != null
                    && (includeParagraphLinks || !(payload instanceof ParagraphFragmentPayload))) {
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
        private boolean clipRasterFallback = true;
        private boolean embedBundledFonts = true;
        private DocumentMetadata metadataOptions;
        private DocumentWatermark watermarkOptions;
        private final List<DocumentHeaderFooter> headerFooterOptions = new ArrayList<>();
        private Instant deterministicTimestamp;

        private Builder() {
        }

        /**
         * Configures deck metadata, written into the OPC core properties
         * (title, author, subject, keywords) and the extended application
         * property.
         *
         * @param metadata canonical metadata, or {@code null} for none
         * @return this builder
         */
        public Builder metadata(DocumentMetadata metadata) {
            this.metadataOptions = metadata;
            return this;
        }

        /**
         * Configures a document-wide watermark, rendered on every slide with
         * the PDF backend's placement math — behind or above the content per
         * its layer.
         *
         * @param watermark canonical watermark, or {@code null} for none
         * @return this builder
         */
        public Builder watermark(DocumentWatermark watermark) {
            this.watermarkOptions = watermark;
            return this;
        }

        /**
         * Adds a repeating header, rendered on every slide with resolved
         * {@code {page}} / {@code {pages}} / {@code {date}} tokens.
         *
         * @param options header chrome
         * @return this builder
         */
        public Builder header(DocumentHeaderFooter options) {
            headerFooterOptions.add(Objects.requireNonNull(options, "options")
                    .withZone(DocumentHeaderFooterZone.HEADER));
            return this;
        }

        /**
         * Adds a repeating footer, rendered on every slide with resolved
         * {@code {page}} / {@code {pages}} / {@code {date}} tokens.
         *
         * @param options footer chrome
         * @return this builder
         */
        public Builder footer(DocumentHeaderFooter options) {
            headerFooterOptions.add(Objects.requireNonNull(options, "options")
                    .withZone(DocumentHeaderFooterZone.FOOTER));
            return this;
        }

        /**
         * Enables (or disables) deterministic output. When enabled, the deck's
         * OPC created / modified core properties are pinned to a fixed default
         * timestamp and every zip entry's modification time is normalized, so
         * the same document renders to byte-identical output across runs — for
         * reproducible builds and byte-level output tests. Disabled by
         * default.
         *
         * <p>A {@code {date}} token in header/footer chrome resolves against
         * the wall-clock date, so decks using it stay byte-identical only
         * within one day — the same limitation the PDF backend has.</p>
         *
         * @param enabled {@code true} to pin output at the default timestamp,
         *                {@code false} to keep POI's live timestamps
         * @return this builder
         * @since 2.1.0
         */
        @Beta
        public Builder deterministic(boolean enabled) {
            this.deterministicTimestamp = enabled ? DEFAULT_DETERMINISTIC_INSTANT : null;
            return this;
        }

        /**
         * Enables deterministic output with an explicit timestamp. See
         * {@link #deterministic(boolean)}.
         *
         * <p>The instant is truncated to whole seconds up front: zip DOS
         * times carry two-second resolution and OPC dates whole seconds, so
         * truncating keeps every serialized clock in agreement for sub-second
         * inputs.</p>
         *
         * @param timestamp the instant to pin the deck's clocks to
         * @return this builder
         * @throws NullPointerException if {@code timestamp} is null
         * @since 2.1.0
         */
        @Beta
        public Builder deterministic(Instant timestamp) {
            this.deterministicTimestamp =
                    Objects.requireNonNull(timestamp, "timestamp").truncatedTo(ChronoUnit.SECONDS);
            return this;
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
         * payload type as a built-in default replaces that default; a second
         * custom handler for the same payload type is rejected, matching the
         * PDF builder's contract.
         *
         * <p>Custom handlers do not apply inside rasterized clip composites —
         * a clip region that cannot be proven a visual no-op renders through
         * the PDF backend's default handlers into one picture.</p>
         *
         * @param handler handler to register
         * @return this builder
         * @throws IllegalArgumentException if a custom handler for the same
         *                                  payload type is already registered
         */
        public Builder addHandler(PptxFragmentRenderHandler<?> handler) {
            Objects.requireNonNull(handler, "handler");
            for (PptxFragmentRenderHandler<?> existing : customHandlers) {
                if (existing.payloadType().equals(handler.payloadType())) {
                    throw new IllegalArgumentException(
                            "Duplicate custom PPTX handler for payload type "
                            + handler.payloadType().getName()
                            + "; remove the previous addHandler() call before registering another");
                }
            }
            customHandlers.add(handler);
            return this;
        }

        /**
         * Controls the clip fallback. DrawingML has no graphics-state
         * clipping, so by default a clipped composite renders through the PDF
         * backend into one transparent picture placed at the clip bounds:
         * pixel-exact clipping (including any enclosing rotation), at the
         * price of that fragment not being editable as shapes. Disabling the
         * fallback renders the children unclipped with a one-time warning.
         *
         * @param enabled {@code true} keeps the raster fallback
         * @return this builder
         */
        public Builder clipRasterFallback(boolean enabled) {
            this.clipRasterFallback = enabled;
            return this;
        }

        /**
         * Controls whether a bundled font family the deck drew with travels with it.
         *
         * <p>On by default, because a viewer without the family installed substitutes, and
         * for a script the substitute does not cover — Georgian, Armenian, Hangul, Arabic,
         * Hebrew — that means boxes rather than words. A family a caller registers has
         * always travelled; this is the same terms for the ones the library ships.</p>
         *
         * <p>It costs what a font weighs. Embedding carries a font program whole — there is
         * no subsetting — so only the faces a run actually drew are taken: a deck that uses
         * one weight of a family does not carry the others. Measured on the shipped
         * examples, an Arabic article grows from 29&nbsp;KB to 235&nbsp;KB and a
         * five-script catalogue from 27&nbsp;KB to 1.4&nbsp;MB. Turn it off for a deck
         * whose readers are known to have the fonts, or one that only uses Latin families
         * a viewer will substitute acceptably — the deck then names each family and
         * carries none.</p>
         *
         * <p>This governs the families the library <em>ships</em>. A family the caller
         * registers through {@code registerFontFamily} travels regardless of this flag,
         * and at family granularity rather than face.</p>
         *
         * @param enabled {@code true} carries the families the deck used
         * @return this builder
         * @since 2.2.0
         */
        public Builder embedBundledFonts(boolean enabled) {
            this.embedBundledFonts = enabled;
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
