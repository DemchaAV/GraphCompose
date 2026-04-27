package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.document.backend.fixed.FixedLayoutBackend;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext;
import com.demcha.compose.document.backend.fixed.pdf.PdfMeasurementResources;
import com.demcha.compose.document.backend.fixed.pdf.PdfFixedLayoutBackend;
import com.demcha.compose.document.backend.fixed.pdf.PdfOutputOptionsTranslator;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterZone;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfMetadataOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfProtectionOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkOptions;
import com.demcha.compose.document.backend.semantic.SemanticBackend;
import com.demcha.compose.document.backend.semantic.SemanticExportContext;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.output.DocumentOutputOptions;
import com.demcha.compose.document.output.DocumentProtection;
import com.demcha.compose.document.output.DocumentWatermark;
import com.demcha.compose.document.debug.snapshot.LayoutGraphSnapshotExtractor;
import com.demcha.compose.document.dsl.DocumentDsl;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.layout.*;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.snapshot.LayoutSnapshot;
import com.demcha.compose.document.style.DocumentInsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Mutable semantic document session used by the canonical GraphCompose
 * session-first API.
 *
 * <p>A session owns one document graph, the measurement services required to
 * prepare that graph, cached layout/render artifacts, and the fluent DSL facade
 * exposed through {@link #dsl()}.</p>
 *
 * <p>The typical lifecycle is:</p>
 * <ol>
 *   <li>configure the session through {@link GraphCompose#document()} and this type's mutators</li>
 *   <li>author content with {@link #pageFlow()}, {@link #compose(Consumer)}, {@link #dsl()},
 *       or by adding low-level {@link DocumentNode}s directly</li>
 *   <li>inspect {@link #layoutGraph()} / {@link #layoutSnapshot()} as needed</li>
 *   <li>render with {@link #writePdf(OutputStream)}, {@link #toPdfBytes()}, {@link #buildPdf()},
 *       or a custom backend</li>
 * </ol>
 *
 * <p><b>Thread-safety:</b> this type is mutable and not thread-safe.</p>
 *
 * @author Artem Demchyshyn
 */
public final class DocumentSession implements AutoCloseable {
    private static final Logger LIFECYCLE_LOG = LoggerFactory.getLogger("com.demcha.compose.document.lifecycle");

    private final String sessionId = Integer.toHexString(System.identityHashCode(this));
    private final Path defaultOutputFile;
    private final NodeRegistry registry;
    private final LayoutCompiler compiler;
    private final List<DocumentNode> roots = new ArrayList<>();
    private final List<FontFamilyDefinition> customFontFamilies = new ArrayList<>();

    private DocumentPageSize pageSize;
    private DocumentInsets margin;
    private LayoutCanvas canvas;
    private boolean markdown;
    private boolean guideLines;

    private DocumentMetadata documentMetadata;
    private DocumentWatermark documentWatermark;
    private DocumentProtection documentProtection;
    private final List<DocumentHeaderFooter> documentHeadersAndFooters = new ArrayList<>();

    private PdfMeasurementResources measurementResources;

    private long revision;
    private LayoutGraph cachedLayout;
    private long cachedLayoutRevision = -1;
    private LayoutSnapshot cachedSnapshot;
    private long cachedSnapshotRevision = -1;
    private boolean closed;


    /**
     * Creates a canonical document session.
     *
     * @param defaultOutputFile optional default PDF output path
     * @param pageSize physical page size
     * @param margin page margin
     * @param customFontFamilies document-local font families
     * @param markdown whether markdown parsing is enabled
     * @param guideLines whether PDF guide-line overlays are enabled
     */
    public DocumentSession(Path defaultOutputFile,
                           DocumentPageSize pageSize,
                           DocumentInsets margin,
                           Collection<FontFamilyDefinition> customFontFamilies,
                           boolean markdown,
                           boolean guideLines) {
        this.defaultOutputFile = defaultOutputFile;
        this.pageSize = Objects.requireNonNull(pageSize, "pageSize");
        this.margin = margin == null ? DocumentInsets.zero() : margin;
        this.canvas = LayoutCanvas.from(pageSize.width(), pageSize.height(), toEngineMargin(this.margin));
        this.markdown = markdown;
        this.guideLines = guideLines;
        this.registry = BuiltInNodeDefinitions.registerDefaults(new NodeRegistry());
        this.compiler = new LayoutCompiler(registry);
        this.customFontFamilies.addAll(List.copyOf(customFontFamilies));
        refreshMeasurementServices();
        LIFECYCLE_LOG.debug(
                "document.session.created sessionId={} outputConfigured={} pageSize={}x{} customFontFamilies={} markdown={} guideLines={}",
                sessionId,
                defaultOutputFile != null,
                Math.round(pageSize.width()),
                Math.round(pageSize.height()),
                this.customFontFamilies.size(),
                markdown,
                guideLines);
    }

    /**
     * Returns the fluent semantic DSL facade bound to this session.
     *
     * @return a new DSL facade for authoring roots and semantic nodes
     * @throws IllegalStateException if this session has already been closed
     */
    public DocumentDsl dsl() {
        ensureOpen();
        return new DocumentDsl(this);
    }

    /**
     * Alias for {@link #dsl()} for callers that prefer a builder-oriented name.
     *
     * @return a new DSL facade for authoring roots and semantic nodes
     * @throws IllegalStateException if this session has already been closed
     */
    public DocumentDsl builder() {
        return dsl();
    }

    /**
     * Applies a batch of canonical DSL authoring calls to this session.
     *
     * <p>This is useful when the calling code wants one high-level authoring
     * block without manually keeping a {@link DocumentDsl} reference.</p>
     *
     * @param spec callback that receives a live DSL facade
     * @return this session
     * @throws IllegalStateException if this session has already been closed
     */
    public DocumentSession compose(Consumer<DocumentDsl> spec) {
        ensureOpen();
        long startNanos = System.nanoTime();
        LIFECYCLE_LOG.debug("document.compose.start sessionId={} revision={} roots={}", sessionId, revision, roots.size());
        try {
            Objects.requireNonNull(spec, "spec").accept(dsl());
            LIFECYCLE_LOG.debug(
                    "document.compose.end sessionId={} revision={} roots={} durationMs={}",
                    sessionId,
                    revision,
                    roots.size(),
                    elapsedMillis(startNanos));
            return this;
        } catch (RuntimeException | Error ex) {
            LIFECYCLE_LOG.debug(
                    "document.compose.failed sessionId={} revision={} roots={} errorType={}",
                    sessionId,
                    revision,
                    roots.size(),
                    ex.getClass().getSimpleName());
            throw ex;
        }
    }

    /**
     * Starts a root page-flow builder bound to this session.
     *
     * <p>This is the recommended high-level entrypoint for the common
     * compose-first authoring path.</p>
     *
     * @return a root flow builder that attaches to this session when built
     * @throws IllegalStateException if this session has already been closed
     */
    public PageFlowBuilder pageFlow() {
        return dsl().pageFlow();
    }

    /**
     * Configures, builds, and attaches one root page flow in a single call.
     *
     * @param spec callback that configures the root flow
     * @return the built root container node
     * @throws IllegalStateException if this session has already been closed
     */
    public ContainerNode pageFlow(Consumer<PageFlowBuilder> spec) {
        return dsl().pageFlow(spec);
    }

    /**
     * Adds one semantic root node to the session.
     *
     * @param node root or detached semantic node to append
     * @return this session
     * @throws IllegalStateException if this session has already been closed
     */
    public DocumentSession add(DocumentNode node) {
        ensureOpen();
        roots.add(Objects.requireNonNull(node, "node"));
        invalidate();
        return this;
    }

    /**
     * Adds multiple semantic root nodes in iteration order.
     *
     * @param nodes semantic nodes to append
     * @return this session
     * @throws IllegalStateException if this session has already been closed
     */
    public DocumentSession addAll(Collection<? extends DocumentNode> nodes) {
        ensureOpen();
        for (DocumentNode node : nodes) {
            add(node);
        }
        return this;
    }

    /**
     * Removes all registered roots and invalidates cached layout/render state.
     *
     * @return this session
     * @throws IllegalStateException if this session has already been closed
     */
    public DocumentSession clear() {
        ensureOpen();
        roots.clear();
        invalidate();
        return this;
    }

    /**
     * Updates the physical page size for subsequent layout passes.
     *
     * @param pageSize target page size
     * @return this session
     */
    public DocumentSession pageSize(DocumentPageSize pageSize) {
        ensureOpen();
        this.pageSize = Objects.requireNonNull(pageSize, "pageSize");
        this.canvas = LayoutCanvas.from(this.pageSize.width(), this.pageSize.height(), toEngineMargin(margin));
        invalidate();
        return this;
    }

    /**
     * Updates the physical page size from point dimensions.
     *
     * @param width page width in points
     * @param height page height in points
     * @return this session
     */
    public DocumentSession pageSize(double width, double height) {
        return pageSize(DocumentPageSize.of(width, height));
    }

    /**
     * Updates the outer document margin using the public canonical spacing value.
     *
     * @param margin new canvas margin, or {@code null} to reset to zero
     * @return this session
     * @throws IllegalStateException if this session has already been closed
     */
    public DocumentSession margin(DocumentInsets margin) {
        ensureOpen();
        this.margin = margin == null ? DocumentInsets.zero() : margin;
        this.canvas = LayoutCanvas.from(pageSize.width(), pageSize.height(), toEngineMargin(this.margin));
        invalidate();
        return this;
    }

    /**
     * Enables or disables markdown parsing for semantic paragraph blocks.
     *
     * @param enabled {@code true} to render paragraph content with markdown-aware spans
     * @return this session
     */
    public DocumentSession markdown(boolean enabled) {
        ensureOpen();
        this.markdown = enabled;
        invalidate();
        return this;
    }

    /**
     * Enables or disables PDF guide-line overlays for convenience PDF output.
     *
     * <p>This option affects {@link #buildPdf()}, {@link #writePdf(OutputStream)},
     * and {@link #toPdfBytes()}. It does not change the semantic layout graph,
     * so existing layout cache entries remain valid.</p>
     *
     * @param enabled {@code true} to draw debug guide-line overlays
     * @return this session
     */
    public DocumentSession guideLines(boolean enabled) {
        ensureOpen();
        this.guideLines = enabled;
        return this;
    }

    /**
     * Configures backend-neutral document metadata applied by every output
     * backend that supports it (PDF and DOCX in v1.3). Pass {@code null} to
     * clear.
     *
     * @param metadata canonical metadata, or {@code null} to clear
     * @return this session
     * @throws IllegalStateException if this session has already been closed
     */
    public DocumentSession metadata(DocumentMetadata metadata) {
        ensureOpen();
        this.documentMetadata = metadata;
        return this;
    }

    /**
     * Compatibility overload that accepts the PDF-specific metadata value and
     * stores it as canonical metadata.
     *
     * @param options PDF metadata options, or {@code null} to clear
     * @return this session
     */
    public DocumentSession metadata(PdfMetadataOptions options) {
        return metadata(options == null ? null : DocumentMetadata.builder()
                .title(options.getTitle())
                .author(options.getAuthor())
                .subject(options.getSubject())
                .keywords(options.getKeywords())
                .creator(options.getCreator())
                .producer(options.getProducer())
                .build());
    }

    /**
     * Configures a backend-neutral document-wide watermark. Pass {@code null}
     * to clear.
     *
     * @param watermark canonical watermark, or {@code null} to clear
     * @return this session
     * @throws IllegalStateException if this session has already been closed
     */
    public DocumentSession watermark(DocumentWatermark watermark) {
        ensureOpen();
        this.documentWatermark = watermark;
        return this;
    }

    /**
     * Compatibility overload that accepts the PDF-specific watermark value.
     *
     * @param options PDF watermark options, or {@code null} to clear
     * @return this session
     */
    public DocumentSession watermark(PdfWatermarkOptions options) {
        return watermark(options == null ? null : PdfOutputOptionsToCanonical.toCanonical(options));
    }

    /**
     * Configures backend-neutral document protection (passwords and
     * permissions). Pass {@code null} to clear.
     *
     * @param protection canonical protection, or {@code null} to clear
     * @return this session
     * @throws IllegalStateException if this session has already been closed
     */
    public DocumentSession protect(DocumentProtection protection) {
        ensureOpen();
        this.documentProtection = protection;
        return this;
    }

    /**
     * Compatibility overload that accepts the PDF-specific protection value.
     *
     * @param options PDF protection options, or {@code null} to clear
     * @return this session
     */
    public DocumentSession protect(PdfProtectionOptions options) {
        return protect(options == null ? null : PdfOutputOptionsToCanonical.toCanonical(options));
    }

    /**
     * Registers a backend-neutral repeating page header.
     *
     * @param header header options
     * @return this session
     * @throws IllegalStateException if this session has already been closed
     */
    public DocumentSession header(DocumentHeaderFooter header) {
        ensureOpen();
        Objects.requireNonNull(header, "header");
        this.documentHeadersAndFooters.add(header.withZone(DocumentHeaderFooterZone.HEADER));
        return this;
    }

    /**
     * Compatibility overload that accepts the PDF-specific header value.
     *
     * @param options PDF header options
     * @return this session
     */
    public DocumentSession header(PdfHeaderFooterOptions options) {
        Objects.requireNonNull(options, "options");
        return header(PdfOutputOptionsToCanonical.toCanonical(options.withZone(PdfHeaderFooterZone.HEADER)));
    }

    /**
     * Registers a backend-neutral repeating page footer.
     *
     * @param footer footer options
     * @return this session
     * @throws IllegalStateException if this session has already been closed
     */
    public DocumentSession footer(DocumentHeaderFooter footer) {
        ensureOpen();
        Objects.requireNonNull(footer, "footer");
        this.documentHeadersAndFooters.add(footer.withZone(DocumentHeaderFooterZone.FOOTER));
        return this;
    }

    /**
     * Compatibility overload that accepts the PDF-specific footer value.
     *
     * @param options PDF footer options
     * @return this session
     */
    public DocumentSession footer(PdfHeaderFooterOptions options) {
        Objects.requireNonNull(options, "options");
        return footer(PdfOutputOptionsToCanonical.toCanonical(options.withZone(PdfHeaderFooterZone.FOOTER)));
    }

    /**
     * Removes all previously registered headers and footers from this session.
     *
     * @return this session
     * @throws IllegalStateException if this session has already been closed
     */
    public DocumentSession clearHeadersAndFooters() {
        ensureOpen();
        this.documentHeadersAndFooters.clear();
        return this;
    }

    /**
     * Registers a document-local font family for text measurement and PDF rendering.
     *
     * @param definition custom font family definition
     * @return this session
     */
    public DocumentSession registerFontFamily(FontFamilyDefinition definition) {
        ensureOpen();
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
        ensureOpen();
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
        ensureOpen();
        if (cachedLayout != null && cachedLayoutRevision == revision) {
            LIFECYCLE_LOG.debug(
                    "document.layout.cache.hit sessionId={} revision={} roots={} pages={} nodes={} fragments={}",
                    sessionId,
                    revision,
                    roots.size(),
                    cachedLayout.totalPages(),
                    cachedLayout.nodes().size(),
                    cachedLayout.fragments().size());
            return cachedLayout;
        }
        long startNanos = System.nanoTime();
        LIFECYCLE_LOG.debug("document.layout.start sessionId={} revision={} roots={}", sessionId, revision, roots.size());
        try {
            DocumentLayoutPassContext context = new DocumentLayoutPassContext(
                    registry,
                    canvas,
                    measurementResources.fontLibrary(),
                    measurementResources.textMeasurementSystem(),
                    markdown);
            cachedLayout = compiler.compile(documentGraph(), context, context);
            cachedLayoutRevision = revision;
            LIFECYCLE_LOG.debug(
                    "document.layout.end sessionId={} revision={} roots={} pages={} nodes={} fragments={} durationMs={}",
                    sessionId,
                    revision,
                    roots.size(),
                    cachedLayout.totalPages(),
                    cachedLayout.nodes().size(),
                    cachedLayout.fragments().size(),
                    elapsedMillis(startNanos));
            return cachedLayout;
        } catch (RuntimeException ex) {
            LIFECYCLE_LOG.warn(
                    "document.layout.failed sessionId={} revision={} roots={} errorType={}",
                    sessionId,
                    revision,
                    roots.size(),
                    ex.getClass().getSimpleName());
            throw ex;
        }
    }

    /**
     * Extracts the current deterministic layout snapshot used by regression tests.
     *
     * @return layout snapshot derived from the current layout graph
     */
    public LayoutSnapshot layoutSnapshot() {
        ensureOpen();
        if (cachedSnapshot != null && cachedSnapshotRevision == revision) {
            LIFECYCLE_LOG.debug(
                    "document.layoutSnapshot.cache.hit sessionId={} revision={} roots={}",
                    sessionId,
                    revision,
                    roots.size());
            return cachedSnapshot;
        }
        long startNanos = System.nanoTime();
        LIFECYCLE_LOG.debug("document.layoutSnapshot.start sessionId={} revision={} roots={}", sessionId, revision, roots.size());
        cachedSnapshot = LayoutGraphSnapshotExtractor.extract(layoutGraph());
        cachedSnapshotRevision = revision;
        LIFECYCLE_LOG.debug(
                "document.layoutSnapshot.end sessionId={} revision={} roots={} pages={} durationMs={}",
                sessionId,
                revision,
                roots.size(),
                cachedSnapshot.totalPages(),
                elapsedMillis(startNanos));
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
        ensureOpen();
        Objects.requireNonNull(backend, "backend");
        long startNanos = System.nanoTime();
        LIFECYCLE_LOG.debug(
                "document.render.start sessionId={} backend={} revision={} roots={} outputConfigured={}",
                sessionId,
                backend.name(),
                revision,
                roots.size(),
                outputFile != null);
        try {
            R result = backend.render(layoutGraph(), new FixedLayoutRenderContext(
                    canvas,
                    customFontFamilies,
                    outputFile,
                    null));
            LIFECYCLE_LOG.debug(
                    "document.render.end sessionId={} backend={} revision={} durationMs={}",
                    sessionId,
                    backend.name(),
                    revision,
                    elapsedMillis(startNanos));
            return result;
        } catch (Exception ex) {
            LIFECYCLE_LOG.error(
                    "document.render.failed sessionId={} backend={} revision={} errorType={}",
                    sessionId,
                    backend.name(),
                    revision,
                    ex.getClass().getSimpleName(),
                    ex);
            throw ex;
        }
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
        ensureOpen();
        Objects.requireNonNull(backend, "backend");
        return backend.export(documentGraph(),
                new SemanticExportContext(canvas, customFontFamilies, outputFile, buildOutputOptions()));
    }

    /**
     * Renders the current session through the canonical PDF backend and returns bytes.
     *
     * <p>This is a convenience wrapper around {@link #writePdf(OutputStream)}.
     * The returned byte array is not cached by the session, so server code that
     * can write directly to a response or object-storage stream should prefer
     * {@link #writePdf(OutputStream)}.</p>
     *
     * @return rendered PDF bytes
     * @throws Exception if PDF rendering fails
     */
    public byte[] toPdfBytes() throws Exception {
        ensureOpen();
        long startNanos = System.nanoTime();
        LIFECYCLE_LOG.debug("document.pdf.bytes.start sessionId={} revision={} roots={}", sessionId, revision, roots.size());
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            writePdf(output);
            byte[] bytes = output.toByteArray();
            LIFECYCLE_LOG.debug(
                    "document.pdf.bytes.end sessionId={} revision={} byteCount={} durationMs={}",
                    sessionId,
                    revision,
                    bytes.length,
                    elapsedMillis(startNanos));
            return bytes;
        } catch (Exception ex) {
            LIFECYCLE_LOG.error(
                    "document.pdf.bytes.failed sessionId={} revision={} errorType={}",
                    sessionId,
                    revision,
                    ex.getClass().getSimpleName(),
                    ex);
            throw ex;
        }
    }

    /**
     * Streams the current session through the canonical PDF backend.
     *
     * <p>The caller owns the supplied stream. GraphCompose writes the PDF bytes
     * but does not close the stream, which makes this method suitable for HTTP
     * responses, cloud storage uploads, and other server-side streaming paths.</p>
     *
     * @param output destination stream that receives the rendered PDF bytes
     * @throws Exception if PDF rendering fails
     */
    public void writePdf(OutputStream output) throws Exception {
        ensureOpen();
        ensureRenderable();
        OutputStream target = Objects.requireNonNull(output, "output");
        long startNanos = System.nanoTime();
        LIFECYCLE_LOG.debug("document.pdf.stream.start sessionId={} revision={} roots={}", sessionId, revision, roots.size());
        try {
            conveniencePdfBackend().write(layoutGraph(), new FixedLayoutRenderContext(
                    canvas,
                    customFontFamilies,
                    null,
                    target));
            LIFECYCLE_LOG.debug(
                    "document.pdf.stream.end sessionId={} revision={} durationMs={}",
                    sessionId,
                    revision,
                    elapsedMillis(startNanos));
        } catch (Exception ex) {
            LIFECYCLE_LOG.error(
                    "document.pdf.stream.failed sessionId={} revision={} errorType={}",
                    sessionId,
                    revision,
                    ex.getClass().getSimpleName(),
                    ex);
            throw ex;
        }
    }

    /**
     * Builds the current document into the default output file configured on the builder.
     *
     * @throws Exception if no default output file exists or PDF rendering fails
     */
    public void buildPdf() throws Exception {
        ensureOpen();
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
        ensureOpen();
        ensureRenderable();
        Path target = Objects.requireNonNull(outputFile, "outputFile");
        long startNanos = System.nanoTime();
        LIFECYCLE_LOG.debug("document.pdf.build.start sessionId={} revision={} roots={}", sessionId, revision, roots.size());
        try (OutputStream output = Files.newOutputStream(target)) {
            writePdf(output);
            LIFECYCLE_LOG.debug(
                    "document.pdf.build.end sessionId={} revision={} durationMs={}",
                    sessionId,
                    revision,
                    elapsedMillis(startNanos));
        } catch (Exception ex) {
            LIFECYCLE_LOG.error(
                    "document.pdf.build.failed sessionId={} revision={} errorType={}",
                    sessionId,
                    revision,
                    ex.getClass().getSimpleName(),
                    ex);
            throw ex;
        }
    }

    /**
     * Closes measurement resources owned by the session.
     *
     * <p>This method is idempotent: a second call is a no-op. After the session
     * is closed, every public authoring or rendering method throws
     * {@link IllegalStateException}.</p>
     *
     * @throws Exception if the measurement document close fails
     */
    @Override
    public void close() throws Exception {
        if (closed) {
            LIFECYCLE_LOG.debug("document.session.close.skip sessionId={} revision={}", sessionId, revision);
            return;
        }
        closed = true;
        LIFECYCLE_LOG.debug("document.session.close.start sessionId={} revision={} roots={}", sessionId, revision, roots.size());
        try {
            if (measurementResources != null) {
                measurementResources.close();
            }
            LIFECYCLE_LOG.debug("document.session.close.end sessionId={} revision={} roots={}", sessionId, revision, roots.size());
        } catch (Exception ex) {
            LIFECYCLE_LOG.warn(
                    "document.session.close.failed sessionId={} revision={} errorType={}",
                    sessionId,
                    revision,
                    ex.getClass().getSimpleName());
            throw ex;
        }
    }

    /**
     * Returns {@code true} once {@link #close()} has been called on this session.
     *
     * @return whether the session is closed
     */
    public boolean isClosed() {
        return closed;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("DocumentSession is already closed.");
        }
    }

    private void ensureRenderable() {
        if (roots.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot render an empty document. Add at least one root before calling writePdf/toPdfBytes/buildPdf.");
        }
    }

    private PdfFixedLayoutBackend conveniencePdfBackend() {
        if (!guideLines
                && documentMetadata == null
                && documentWatermark == null
                && documentProtection == null
                && documentHeadersAndFooters.isEmpty()) {
            return new PdfFixedLayoutBackend();
        }
        PdfFixedLayoutBackend.Builder builder = PdfFixedLayoutBackend.builder()
                .guideLines(guideLines)
                .metadata(PdfOutputOptionsTranslator.toPdf(documentMetadata))
                .watermark(PdfOutputOptionsTranslator.toPdf(documentWatermark))
                .protect(PdfOutputOptionsTranslator.toPdf(documentProtection));
        for (DocumentHeaderFooter entry : documentHeadersAndFooters) {
            PdfHeaderFooterOptions translated = PdfOutputOptionsTranslator.toPdf(entry);
            if (entry.getZone() == DocumentHeaderFooterZone.FOOTER) {
                builder.footer(translated);
            } else {
                builder.header(translated);
            }
        }
        return builder.build();
    }

    private DocumentOutputOptions buildOutputOptions() {
        if (documentMetadata == null
                && documentWatermark == null
                && documentProtection == null
                && documentHeadersAndFooters.isEmpty()) {
            return DocumentOutputOptions.EMPTY;
        }
        return new DocumentOutputOptions(
                documentMetadata,
                documentWatermark,
                documentProtection,
                List.copyOf(documentHeadersAndFooters));
    }

    private com.demcha.compose.engine.components.style.Margin toEngineMargin(DocumentInsets insets) {
        if (insets == null) {
            return com.demcha.compose.engine.components.style.Margin.zero();
        }
        return new com.demcha.compose.engine.components.style.Margin(
                insets.top(),
                insets.right(),
                insets.bottom(),
                insets.left());
    }

    private void refreshMeasurementServices() {
        try {
            if (measurementResources != null) {
                measurementResources.close();
            }
        } catch (Exception ignored) {
            // Best-effort close while reloading measurement resources.
        }

        measurementResources = PdfMeasurementResources.open(customFontFamilies);
    }

    private void invalidate() {
        revision++;
        cachedLayout = null;
        cachedSnapshot = null;
    }

    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

}
