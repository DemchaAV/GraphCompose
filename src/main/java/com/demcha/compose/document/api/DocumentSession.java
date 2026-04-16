package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterZone;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfMetadataOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfProtectionOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkOptions;
import com.demcha.compose.font_library.DefaultFonts;
import com.demcha.compose.font_library.FontFamilyDefinition;
import com.demcha.compose.font_library.FontLibrary;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.debug.LayoutSnapshot;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.compose.layout_core.system.measurement.FontLibraryTextMeasurementSystem;
import com.demcha.compose.layout_core.system.interfaces.TextMeasurementSystem;
import com.demcha.compose.document.backend.fixed.FixedLayoutBackend;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext;
import com.demcha.compose.document.backend.fixed.pdf.PdfFixedLayoutBackend;
import com.demcha.compose.document.backend.semantic.SemanticBackend;
import com.demcha.compose.document.backend.semantic.SemanticExportContext;
import com.demcha.compose.document.debug.snapshot.LayoutGraphSnapshotExtractor;
import com.demcha.compose.document.dsl.DocumentDsl;
import com.demcha.compose.document.layout.*;
import com.demcha.compose.document.model.node.DocumentNode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable semantic document session used by the canonical GraphCompose V2 API.
 *
 * <p>A session owns one document graph, the measurement services required to
 * prepare that graph, cached layout/render artifacts, and the fluent DSL facade
 * exposed through {@link #dsl()}.</p>
 *
 * <p>The typical lifecycle is:</p>
 * <ol>
 *   <li>configure the session through {@link GraphCompose#document()} and this type's mutators</li>
 *   <li>author content with {@link #dsl()} or by adding low-level {@link DocumentNode}s directly</li>
 *   <li>inspect {@link #layoutGraph()} / {@link #layoutSnapshot()} as needed</li>
 *   <li>render with {@link #toPdfBytes()}, {@link #buildPdf()}, or a custom backend</li>
 * </ol>
 *
 * <p><b>Thread-safety:</b> this type is mutable and not thread-safe.</p>
 */
public final class DocumentSession implements AutoCloseable {
    private final Path defaultOutputFile;
    private final NodeRegistry registry;
    private final LayoutCompiler compiler;
    private final List<DocumentNode> roots = new ArrayList<>();
    private final List<FontFamilyDefinition> customFontFamilies = new ArrayList<>();
    private final List<PdfHeaderFooterOptions> headerFooterOptions = new ArrayList<>();

    private PDRectangle pageSize;
    private Margin margin;
    private LayoutCanvas canvas;
    private boolean guideLines;
    private PdfMetadataOptions metadataOptions;
    private PdfWatermarkOptions watermarkOptions;
    private PdfProtectionOptions protectionOptions;

    private PDDocument measurementDocument;
    private FontLibrary fontLibrary;
    private TextMeasurementSystem textMeasurementSystem;

    private long revision;
    private long pdfConfigVersion;
    private LayoutGraph cachedLayout;
    private long cachedLayoutRevision = -1;
    private LayoutSnapshot cachedSnapshot;
    private long cachedSnapshotRevision = -1;
    private byte[] cachedPdfBytes;
    private long cachedPdfRevision = -1;

    public DocumentSession(Path defaultOutputFile,
                           PDRectangle pageSize,
                           Margin margin,
                           Collection<FontFamilyDefinition> customFontFamilies,
                           boolean guideLines,
                           PdfMetadataOptions metadataOptions,
                           PdfWatermarkOptions watermarkOptions,
                           PdfProtectionOptions protectionOptions,
                           Collection<PdfHeaderFooterOptions> headerFooterOptions) {
        this.defaultOutputFile = defaultOutputFile;
        this.pageSize = Objects.requireNonNull(pageSize, "pageSize");
        this.margin = margin == null ? Margin.zero() : margin;
        this.canvas = LayoutCanvas.from(pageSize, this.margin);
        this.guideLines = guideLines;
        this.metadataOptions = metadataOptions;
        this.watermarkOptions = watermarkOptions;
        this.protectionOptions = protectionOptions;
        this.registry = BuiltInNodeDefinitions.registerDefaults(new NodeRegistry());
        this.compiler = new LayoutCompiler(registry);
        this.customFontFamilies.addAll(List.copyOf(customFontFamilies));
        this.headerFooterOptions.addAll(List.copyOf(headerFooterOptions));
        refreshMeasurementServices();
    }

    /**
     * Returns the fluent semantic DSL facade bound to this session.
     *
     * @return a new DSL facade for authoring roots and semantic nodes
     */
    public DocumentDsl dsl() {
        return new DocumentDsl(this);
    }

    /**
     * Alias for {@link #dsl()} for callers that prefer a builder-oriented name.
     *
     * @return a new DSL facade for authoring roots and semantic nodes
     */
    public DocumentDsl builder() {
        return dsl();
    }

    /**
     * Adds one semantic root node to the session.
     *
     * @param node root or detached semantic node to append
     * @return this session
     */
    public DocumentSession add(DocumentNode node) {
        roots.add(Objects.requireNonNull(node, "node"));
        invalidate();
        return this;
    }

    /**
     * Adds multiple semantic root nodes in iteration order.
     *
     * @param nodes semantic nodes to append
     * @return this session
     */
    public DocumentSession addAll(Collection<? extends DocumentNode> nodes) {
        for (DocumentNode node : nodes) {
            add(node);
        }
        return this;
    }

    /**
     * Removes all registered roots and invalidates cached layout/render state.
     *
     * @return this session
     */
    public DocumentSession clear() {
        roots.clear();
        invalidate();
        return this;
    }

    /**
     * Updates the physical page size for subsequent layout passes.
     *
     * @param pageSize target page rectangle
     * @return this session
     */
    public DocumentSession pageSize(PDRectangle pageSize) {
        this.pageSize = Objects.requireNonNull(pageSize, "pageSize");
        this.canvas = LayoutCanvas.from(this.pageSize, margin);
        invalidate();
        return this;
    }

    /**
     * Updates the outer document margin and recomputes the semantic canvas.
     *
     * @param margin new canvas margin, or {@code null} to reset to zero
     * @return this session
     */
    public DocumentSession margin(Margin margin) {
        this.margin = margin == null ? Margin.zero() : margin;
        this.canvas = LayoutCanvas.from(pageSize, this.margin);
        invalidate();
        return this;
    }

    /**
     * Enables or disables PDF guide-line overlays for debugging rendered
     * semantic fragment geometry.
     *
     * @param enabled {@code true} to draw guide lines in rendered PDFs
     * @return this session
     */
    public DocumentSession guideLines(boolean enabled) {
        this.guideLines = enabled;
        invalidatePdfArtifacts();
        return this;
    }

    /**
     * Configures document metadata for PDFs rendered from this session.
     *
     * @param options canonical metadata options, or {@code null} to clear
     * @return this session
     */
    public DocumentSession metadata(PdfMetadataOptions options) {
        this.metadataOptions = options;
        invalidatePdfArtifacts();
        return this;
    }

    /**
     * Configures a document-wide watermark for PDFs rendered from this session.
     *
     * @param options canonical watermark options, or {@code null} to clear
     * @return this session
     */
    public DocumentSession watermark(PdfWatermarkOptions options) {
        this.watermarkOptions = options;
        invalidatePdfArtifacts();
        return this;
    }

    /**
     * Configures PDF protection and permissions for this session.
     *
     * @param options canonical protection options, or {@code null} to clear
     * @return this session
     */
    public DocumentSession protect(PdfProtectionOptions options) {
        this.protectionOptions = options;
        invalidatePdfArtifacts();
        return this;
    }

    /**
     * Registers a repeating page header.
     *
     * @param options canonical header options
     * @return this session
     */
    public DocumentSession header(PdfHeaderFooterOptions options) {
        this.headerFooterOptions.add(Objects.requireNonNull(options, "options")
                .withZone(PdfHeaderFooterZone.HEADER));
        invalidatePdfArtifacts();
        return this;
    }

    /**
     * Registers a repeating page footer.
     *
     * @param options canonical footer options
     * @return this session
     */
    public DocumentSession footer(PdfHeaderFooterOptions options) {
        this.headerFooterOptions.add(Objects.requireNonNull(options, "options")
                .withZone(PdfHeaderFooterZone.FOOTER));
        invalidatePdfArtifacts();
        return this;
    }

    /**
     * Registers a document-local font family for text measurement and PDF rendering.
     *
     * @param definition custom font family definition
     * @return this session
     */
    public DocumentSession registerFontFamily(FontFamilyDefinition definition) {
        customFontFamilies.add(Objects.requireNonNull(definition, "definition"));
        refreshMeasurementServices();
        invalidate();
        return this;
    }

    /**
     * Registers a custom semantic node definition.
     *
     * @param definition node definition implementation
     * @param <E> semantic node type handled by the definition
     * @return this session
     */
    public <E extends DocumentNode> DocumentSession registerNodeDefinition(NodeDefinition<E> definition) {
        registry.register(Objects.requireNonNull(definition, "definition"));
        invalidate();
        return this;
    }

    /**
     * Returns the mutable node registry used by this session.
     *
     * @return active node registry
     */
    public NodeRegistry registry() {
        return registry;
    }

    /**
     * Returns an immutable snapshot of the current semantic root graph.
     *
     * @return document graph snapshot
     */
    public DocumentGraph documentGraph() {
        return new DocumentGraph(List.copyOf(roots));
    }

    /**
     * Returns the current semantic layout canvas derived from page size and margin.
     *
     * @return current layout canvas
     */
    public LayoutCanvas canvas() {
        return canvas;
    }

    /**
     * Returns an immutable copy of the current semantic roots.
     *
     * @return semantic root nodes in insertion order
     */
    public List<DocumentNode> roots() {
        return List.copyOf(roots);
    }

    /**
     * Compiles the semantic graph into a resolved, paginated layout graph.
     *
     * @return cached or freshly compiled layout graph
     */
    public LayoutGraph layoutGraph() {
        if (cachedLayout != null && cachedLayoutRevision == revision) {
            return cachedLayout;
        }
        V2Context context = new V2Context();
        cachedLayout = compiler.compile(documentGraph(), context, context);
        cachedLayoutRevision = revision;
        return cachedLayout;
    }

    /**
     * Extracts the current deterministic layout snapshot used by regression tests.
     *
     * @return layout snapshot derived from the current layout graph
     */
    public LayoutSnapshot layoutSnapshot() {
        if (cachedSnapshot != null && cachedSnapshotRevision == revision) {
            return cachedSnapshot;
        }
        cachedSnapshot = LayoutGraphSnapshotExtractor.extract(layoutGraph());
        cachedSnapshotRevision = revision;
        return cachedSnapshot;
    }

    /**
     * Renders the current layout graph with the supplied fixed-layout backend.
     *
     * @param backend backend implementation that consumes the resolved layout graph
     * @param <R> backend-specific result type
     * @return backend render result
     * @throws Exception if rendering fails
     */
    public <R> R render(FixedLayoutBackend<R> backend) throws Exception {
        return render(backend, defaultOutputFile);
    }

    /**
     * Renders the current layout graph with an explicit output target override.
     *
     * @param backend backend implementation that consumes the resolved layout graph
     * @param outputFile optional output target for backends that persist to disk
     * @param <R> backend-specific result type
     * @return backend render result
     * @throws Exception if rendering fails
     */
    public <R> R render(FixedLayoutBackend<R> backend, Path outputFile) throws Exception {
        Objects.requireNonNull(backend, "backend");
        return backend.render(layoutGraph(), new FixedLayoutRenderContext(
                canvas,
                customFontFamilies,
                outputFile,
                guideLines,
                metadataOptions,
                watermarkOptions,
                protectionOptions,
                headerFooterOptions));
    }

    /**
     * Exports the semantic graph through a semantic backend using the default output file.
     *
     * @param backend semantic backend implementation
     * @param <R> backend-specific result type
     * @return export result
     * @throws Exception if export fails
     */
    public <R> R export(SemanticBackend<R> backend) throws Exception {
        return export(backend, defaultOutputFile);
    }

    /**
     * Exports the semantic graph through a semantic backend using an explicit output target.
     *
     * @param backend semantic backend implementation
     * @param outputFile optional output file override
     * @param <R> backend-specific result type
     * @return export result
     * @throws Exception if export fails
     */
    public <R> R export(SemanticBackend<R> backend, Path outputFile) throws Exception {
        Objects.requireNonNull(backend, "backend");
        return backend.export(documentGraph(), new SemanticExportContext(canvas, customFontFamilies, outputFile));
    }

    /**
     * Renders the current session through the canonical PDF backend and returns bytes.
     *
     * @return rendered PDF bytes
     * @throws Exception if PDF rendering fails
     */
    public byte[] toPdfBytes() throws Exception {
        long currentPdfVersion = currentPdfInputsVersion();
        if (cachedPdfBytes != null && cachedPdfRevision == currentPdfVersion) {
            return cachedPdfBytes.clone();
        }
        cachedPdfBytes = render(new PdfFixedLayoutBackend(), null);
        cachedPdfRevision = currentPdfVersion;
        return cachedPdfBytes.clone();
    }

    /**
     * Builds the current document into the default output file configured on the builder.
     *
     * @throws Exception if no default output file exists or PDF rendering fails
     */
    public void buildPdf() throws Exception {
        if (defaultOutputFile == null) {
            throw new IllegalStateException("No default output file was configured for this document session.");
        }
        buildPdf(defaultOutputFile);
    }

    /**
     * Builds the current document into the supplied output file.
     *
     * @param outputFile destination PDF path
     * @throws Exception if PDF rendering fails
     */
    public void buildPdf(Path outputFile) throws Exception {
        Files.write(Objects.requireNonNull(outputFile, "outputFile"), toPdfBytes());
    }

    /**
     * Closes measurement resources owned by the session.
     *
     * @throws Exception if the measurement document close fails
     */
    @Override
    public void close() throws Exception {
        if (measurementDocument != null) {
            measurementDocument.close();
        }
    }

    private void refreshMeasurementServices() {
        try {
            if (measurementDocument != null) {
                measurementDocument.close();
            }
        } catch (Exception ignored) {
            // Best-effort close while reloading measurement resources.
        }

        measurementDocument = new PDDocument();
        fontLibrary = DefaultFonts.library(measurementDocument, customFontFamilies);
        textMeasurementSystem = new FontLibraryTextMeasurementSystem(fontLibrary, PdfFont.class);
    }

    private void invalidate() {
        revision++;
        pdfConfigVersion++;
        cachedLayout = null;
        cachedSnapshot = null;
        cachedPdfBytes = null;
        cachedPdfRevision = -1;
    }

    private void invalidatePdfArtifacts() {
        pdfConfigVersion++;
        cachedPdfBytes = null;
        cachedPdfRevision = -1;
    }

    private long currentPdfInputsVersion() {
        return revision * 31L + pdfConfigVersion;
    }

    private final class V2Context implements PrepareContext, FragmentContext {
        private final Map<PreparedNodeCacheKey, PreparedNode<?>> preparedNodes = new HashMap<>();

        @Override
        public <E extends DocumentNode> PreparedNode<E> prepare(E node, BoxConstraints constraints) {
            PreparedNodeCacheKey cacheKey = new PreparedNodeCacheKey(node, normalizeWidth(constraints.availableWidth()));
            PreparedNode<?> cached = preparedNodes.get(cacheKey);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                PreparedNode<E> typed = (PreparedNode<E>) cached;
                return typed;
            }

            @SuppressWarnings("unchecked")
            NodeDefinition<E> definition = (NodeDefinition<E>) registry.definitionFor(node);
            PreparedNode<E> prepared = Objects.requireNonNull(
                    definition.prepare(node, this, constraints),
                    "Node definition prepare(...) must not return null for " + node.nodeKind());
            preparedNodes.put(cacheKey, prepared);
            return prepared;
        }

        @Override
        public FontLibrary fonts() {
            return fontLibrary;
        }

        @Override
        public TextMeasurementSystem textMeasurement() {
            return textMeasurementSystem;
        }

        @Override
        public LayoutCanvas canvas() {
            return canvas;
        }
    }

    private long normalizeWidth(double value) {
        return Math.round(value * 1_000.0);
    }

    private record PreparedNodeCacheKey(DocumentNode node, long widthKey) {
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof PreparedNodeCacheKey that)) {
                return false;
            }
            return widthKey == that.widthKey && node == that.node;
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(node) + Long.hashCode(widthKey);
        }
    }
}




