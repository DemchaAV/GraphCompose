package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.backend.fixed.BackendProviders;
import com.demcha.compose.document.backend.fixed.FixedLayoutBackend;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderer;
import com.demcha.compose.document.backend.fixed.MeasurementResources;
import com.demcha.compose.document.backend.fixed.SectionUnit;
import com.demcha.compose.document.backend.semantic.SemanticBackend;
import com.demcha.compose.document.debug.snapshot.LayoutGraphSnapshotExtractor;
import com.demcha.compose.document.debug.snapshot.PageIndexExtractor;
import com.demcha.compose.document.dsl.DocumentDsl;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.exceptions.DocumentRenderingException;
import com.demcha.compose.document.layout.*;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.PageReferenceNode;
import com.demcha.compose.document.output.*;
import com.demcha.compose.document.snapshot.LayoutSnapshot;
import com.demcha.compose.document.snapshot.PageIndex;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.font.FontFamilyDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 *       their PPTX counterparts ({@link #writePptx(OutputStream)}, {@link #toPptxBytes()},
 *       {@link #buildPptx(Path)}), or a custom backend</li>
 * </ol>
 *
 * <p><b>Thread-safety:</b> this type is mutable and not thread-safe.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.0.0
 */
public final class DocumentSession implements AutoCloseable {
    private static final Logger LIFECYCLE_LOG = LoggerFactory.getLogger("com.demcha.compose.document.lifecycle");

    private final String sessionId = Integer.toHexString(System.identityHashCode(this));
    private final Path defaultOutputFile;
    private final NodeRegistry registry;
    private final LayoutCompiler compiler;
    private final List<DocumentNode> roots = new ArrayList<>();
    private final List<FontFamilyDefinition> customFontFamilies = new ArrayList<>();
    private final DocumentChromeOptions chromeOptions = new DocumentChromeOptions();
    private final DocumentLayoutCache layoutCache = new DocumentLayoutCache();
    private final DocumentRenderingFacade renderingFacade = new DocumentRenderingFacade(new RenderingContextImpl());
    private final DocumentLayoutResolver layoutResolver = new DocumentLayoutResolver(new LayoutResolverContext());
    private DocumentPageSize pageSize;
    private DocumentInsets margin;
    private LayoutCanvas canvas;
    private boolean markdown;
    private DocumentDebugOptions debug = DocumentDebugOptions.none();
    private List<PageBackgroundFill> pageBackgrounds = List.of();
    private List<PageMarginRule> pageMargins = List.of();
    private MeasurementResources measurementResources;
    private boolean closed;


    /**
     * Creates a canonical document session.
     *
     * @param defaultOutputFile  optional default output path for the no-arg build methods
     * @param pageSize           physical page size
     * @param margin             page margin
     * @param customFontFamilies document-local font families
     * @param markdown           whether markdown parsing is enabled
     * @param guideLines         whether PDF guide-line overlays are enabled
     */
    public DocumentSession(Path defaultOutputFile,
                           DocumentPageSize pageSize,
                           DocumentInsets margin,
                           Collection<FontFamilyDefinition> customFontFamilies,
                           boolean markdown,
                           boolean guideLines) {
        this.defaultOutputFile = defaultOutputFile;
        this.pageSize = Objects.requireNonNull(pageSize, "pageSize");
        this.margin = margin == null ? DocumentInsets.zero() : requireNonNegativePageMargin(margin);
        this.canvas = LayoutCanvas.from(pageSize.width(), pageSize.height(), this.margin);
        this.markdown = markdown;
        this.debug = DocumentDebugOptions.none().withGuides(guideLines);
        this.registry = BuiltInNodeDefinitions.registerDefaults(new InvalidatingNodeRegistry());
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
     * Runs a fixed-layout convenience body (PDF or PPTX) and unifies the
     * cross-cutting checked-exception wrapping: any underlying
     * {@link Exception} is rewrapped as {@link DocumentRenderingException}
     * with the supplied {@code action} fragment. {@link RuntimeException}s
     * pass through unchanged so existing callers that already catch them keep
     * their semantics.
     */
    private static <R> R wrapRendering(String action, RenderingBody<R> body) throws DocumentRenderingException {
        try {
            return body.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentRenderingException("Failed to " + action + ": " + e.getMessage(), e);
        }
    }

    /**
     * Image-rendering analogue of {@link #wrapRendering}: rewraps any
     * underlying checked {@link Exception} as {@link DocumentRenderingException}
     * while letting {@link RuntimeException}s (e.g. argument/state validation)
     * propagate unchanged.
     */
    private static <R> R wrapImageRendering(String action, ImageRenderingBody<R> body) throws DocumentRenderingException {
        try {
            return body.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentRenderingException("Failed to " + action + ": " + e.getMessage(), e);
        }
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
        LIFECYCLE_LOG.debug("document.compose.start sessionId={} revision={} roots={}", sessionId, layoutCache.revision(), roots.size());
        try {
            Objects.requireNonNull(spec, "spec").accept(dsl());
            LIFECYCLE_LOG.debug("document.compose.end sessionId={} revision={} roots={} durationMs={}", sessionId, layoutCache.revision(), roots.size(), elapsedMillis(startNanos));
            return this;
        } catch (RuntimeException | Error ex) {
            LIFECYCLE_LOG.debug("document.compose.failed sessionId={} revision={} roots={} errorType={}", sessionId, layoutCache.revision(), roots.size(), ex.getClass().getSimpleName());
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
        this.canvas = LayoutCanvas.from(this.pageSize.width(), this.pageSize.height(), margin);
        invalidate();
        return this;
    }

    /**
     * Updates the physical page size from point dimensions.
     *
     * @param width  page width in points
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
     * @throws IllegalStateException    if this session has already been closed
     * @throws IllegalArgumentException if any margin component is negative — a
     *                                  negative page margin overflows the sheet;
     *                                  use a node's {@code bleed(...)} instead
     */
    public DocumentSession margin(DocumentInsets margin) {
        ensureOpen();
        this.margin = margin == null ? DocumentInsets.zero() : requireNonNegativePageMargin(margin);
        this.canvas = LayoutCanvas.from(pageSize.width(), pageSize.height(), this.margin);
        invalidate();
        return this;
    }

    /**
     * Rejects a negative page margin. Unlike a node margin (where a negative
     * value is a valid overlap / inset trick), a negative page margin makes the
     * content area larger than the page, so content silently overflows the sheet.
     * To draw content past the page edge, use a node's {@code bleed(...)} instead.
     *
     * @param margin the requested page margin
     * @return {@code margin} when every component is non-negative
     * @throws IllegalArgumentException if any component is negative
     */
    private static DocumentInsets requireNonNegativePageMargin(DocumentInsets margin) {
        if (margin.top() < 0 || margin.right() < 0 || margin.bottom() < 0 || margin.left() < 0) {
            throw new IllegalArgumentException(
                    "page margin must be non-negative: " + margin
                    + " — use a node's bleed(...) to extend content past the page edge");
        }
        return margin;
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
     * <p>Shorthand for toggling only the guide overlay on the current
     * {@link #debug(DocumentDebugOptions) debug} configuration; node-label
     * settings are preserved.</p>
     *
     * @param enabled {@code true} to draw debug guide-line overlays
     * @return this session
     */
    public DocumentSession guideLines(boolean enabled) {
        ensureOpen();
        this.debug = this.debug.withGuides(enabled);
        return this;
    }

    /**
     * Configures PDF debug overlays (guide lines and semantic node labels)
     * for convenience PDF output.
     *
     * <p>This option affects {@link #buildPdf()}, {@link #writePdf(OutputStream)},
     * and {@link #toPdfBytes()}. Debug overlays draw on top of regular content
     * and never participate in measurement or pagination, so the semantic
     * layout graph and existing layout cache entries remain valid.</p>
     *
     * <p>Node labels print each node's stable semantic path — the same path
     * reported by {@link #layoutSnapshot()} — so a misplaced block on the
     * sheet can be traced straight back to the builder call that authored
     * it.</p>
     *
     * @param options debug overlay options; {@code null} disables all overlays
     * @return this session
     * @since 1.8.0
     */
    public DocumentSession debug(DocumentDebugOptions options) {
        ensureOpen();
        this.debug = options == null ? DocumentDebugOptions.none() : options;
        return this;
    }

    /**
     * Configures a document-wide page background fill applied behind every
     * fragment on every page.
     *
     * <p>Use this for cinematic looks (cream paper, deep navy hero documents,
     * etc.) without having to wrap every section in a stack and shape. Pass
     * {@code null} to clear the background.</p>
     *
     * @param color page background color, or {@code null} to clear
     * @return this session
     * @throws IllegalStateException if this session has already been closed
     */
    public DocumentSession pageBackground(DocumentColor color) {
        ensureOpen();
        this.pageBackgrounds = color == null
                ? List.of()
                : List.of(PageBackgroundFill.fullPage(color));
        invalidate();
        return this;
    }

    /**
     * Convenience overload that accepts a {@link Color} value.
     *
     * @param color page background color, or {@code null} to clear
     * @return this session
     */
    public DocumentSession pageBackground(Color color) {
        return pageBackground(color == null ? null : DocumentColor.of(color));
    }

    /**
     * Configures one or more rectangular background fills applied behind
     * every fragment on every page. Each fill is defined as a fraction of
     * the canvas (see {@link PageBackgroundFill}), so the layout works
     * across page sizes. Use this for multi-column page chrome — a pale
     * sidebar column plus a white main column, accent stripes, etc. —
     * that should repeat automatically when content paginates onto a new
     * page.
     *
     * <p>Pass {@code null} or an empty list to clear. Fills paint in list
     * order; later entries paint on top of earlier ones where they
     * overlap.</p>
     *
     * @param fills ordered list of fills, or {@code null}/empty to clear
     * @return this session
     * @throws IllegalStateException if this session has already been closed
     */
    public DocumentSession pageBackgrounds(List<PageBackgroundFill> fills) {
        ensureOpen();
        this.pageBackgrounds = fills == null ? List.of() : List.copyOf(fills);
        invalidate();
        return this;
    }

    /**
     * Overrides the page margin for ranges of pages, replacing the document-wide
     * {@link #margin(DocumentInsets)} on the pages each rule covers. Use this for a
     * document whose pages are not all the same shape — a full-bleed cover with
     * {@link DocumentInsets#zero()} insets followed by book margins on the body, a
     * wide title page, and so on.
     *
     * <p>Each {@link PageMarginRule} addresses pages by 1-based number; rules apply
     * in list order and the last rule covering a page wins. Pass {@code null} or an
     * empty list to clear (the default — every page uses the document-wide margin).</p>
     *
     * <p>Because content is measured before it is paginated, each top-level block is
     * laid out at the content width <em>and</em> against the vertical space of the page
     * it begins on. A margin that changes the content width or the usable height
     * therefore takes effect where content breaks onto a covered page — the model is
     * different margins for different page ranges, not a margin that changes mid-block,
     * and a keep-together block's fit is judged against its starting page's height. To
     * extend a single node past the page edge instead, use {@code bleed(...)} (see
     * {@link com.demcha.compose.document.style.DocumentBleed}).</p>
     *
     * @param rules ordered list of per-page margin overrides, or {@code null}/empty to clear
     * @return this session
     * @throws IllegalStateException if this session has already been closed
     * @since 1.9.0
     */
    public DocumentSession pageMargins(List<PageMarginRule> rules) {
        ensureOpen();
        this.pageMargins = rules == null ? List.of() : List.copyOf(rules);
        invalidate();
        return this;
    }

    /**
     * Returns a fluent facade for chrome configuration (metadata,
     * watermark, protection, header, footer). The facade is a thin
     * grouping of the canonical chrome methods on this session — both
     * styles set the same underlying chrome state.
     *
     * <p>Example:</p>
     * <pre>{@code
     * session.chrome()
     *         .metadata(DocumentMetadata.builder().title("Q1").build())
     *         .watermark(DocumentWatermark.builder().text("DRAFT").build());
     * }</pre>
     *
     * <p>Use {@link SessionChromeApi#session()} to chain back to the
     * session if you need to mix chrome and authoring calls in a single
     * fluent expression.</p>
     *
     * @return chrome configuration facade for this session
     * @throws IllegalStateException if this session has already been closed
     * @since 1.6.0
     */
    public SessionChromeApi chrome() {
        ensureOpen();
        return new SessionChromeApi(this, chromeOptions);
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
        chromeOptions.setMetadata(metadata);
        return this;
    }

    /**
     * Sets the PDF viewer preferences — how a reader presents the document when it
     * opens (page mode, page layout, window flags). PDF-only; other backends
     * ignore it.
     *
     * @param viewerPreferences viewer preferences, or {@code null} to clear
     * @return this session
     * @throws IllegalStateException if this session has already been closed
     * @since 1.9.0
     */
    public DocumentSession viewerPreferences(DocumentViewerPreferences viewerPreferences) {
        ensureOpen();
        chromeOptions.setViewerPreferences(viewerPreferences);
        return this;
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
        chromeOptions.setWatermark(watermark);
        return this;
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
        chromeOptions.setProtection(protection);
        return this;
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
        chromeOptions.addHeader(header);
        return this;
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
        chromeOptions.addFooter(footer);
        return this;
    }

    /**
     * Removes all previously registered headers and footers from this session.
     *
     * @return this session
     * @throws IllegalStateException if this session has already been closed
     */
    public DocumentSession clearHeadersAndFooters() {
        ensureOpen();
        chromeOptions.clearHeadersAndFooters();
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
     * Registers a custom semantic node definition. Equivalent to
     * {@code session.registry().register(definition)} — the layout
     * cache is invalidated either way (the registry returned by
     * {@link #registry()} is a session-owned wrapper that funnels
     * mutations through {@code invalidate()} since v1.6.7).
     *
     * @param definition node definition implementation
     * @param <E>        semantic node type handled by the definition
     * @return this session
     * @throws IllegalStateException if this session has already been closed
     */
    public <E extends DocumentNode> DocumentSession registerNodeDefinition(NodeDefinition<E> definition) {
        ensureOpen();
        registry.register(Objects.requireNonNull(definition, "definition"));
        return this;
    }

    /**
     * Returns a fluent facade for document-local font and node-extension
     * registration. The facade is a thin grouping of
     * {@link #registerFontFamily(FontFamilyDefinition)} and
     * {@link #registerNodeDefinition(NodeDefinition)} — both styles mutate
     * the same underlying state.
     *
     * @return font + extension registration facade for this session
     * @throws IllegalStateException if this session has already been closed
     * @since 1.6.0
     */
    public SessionFontApi fonts() {
        ensureOpen();
        return new SessionFontApi(this);
    }

    /**
     * Returns a fluent facade for read-only layout inspection. The
     * facade is a thin grouping of {@link #layoutGraph()},
     * {@link #documentGraph()}, {@link #roots()}, {@link #canvas()},
     * {@link #registry()}, and {@link #layoutSnapshot()} — both styles
     * read the same underlying state.
     *
     * <p>Example:</p>
     * <pre>{@code
     * LayoutGraph graph = session.layout().graph();
     * LayoutSnapshot snap = session.layout().snapshot();
     * }</pre>
     *
     * @return layout-inspection facade for this session
     * @since 1.6.0
     */
    public SessionLayoutApi layout() {
        return new SessionLayoutApi(this);
    }

    /**
     * Returns the live node registry backing this session. The returned
     * instance is a session-owned subclass of {@link NodeRegistry} (since
     * v1.6.7); calling {@link NodeRegistry#register(NodeDefinition)} on it
     * mutates the registry <em>and</em> invalidates the layout cache, so it
     * is interchangeable with {@link #registerNodeDefinition(NodeDefinition)}.
     * Read-only access via {@link NodeRegistry#definitionFor(DocumentNode)}
     * is unaffected.
     *
     * @return live node registry (invalidates layout cache on mutation)
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
     * Returns the document-wide page margin — whatever was last passed to
     * {@link #margin(DocumentInsets)}, or zero insets when none was.
     *
     * <p>Composition code that has to reason about the page's own edges reads it
     * here rather than through {@link #canvas()}, whose margin is an engine type.
     * A template that derives a {@link PageMarginRule} from it — reserving a safe
     * area on continuation pages, say — keeps working when the caller chooses a
     * margin other than the one the template recommends.</p>
     *
     * @return the current page margin
     * @since 2.3.0
     */
    public DocumentInsets margin() {
        return margin;
    }

    /**
     * Returns the usable content height of the page in points — the page height
     * minus the top and bottom margins. Convenience alias for
     * {@code canvas().innerHeight()}: the value a composition reads to decide how
     * much vertical space a section, sidebar, or spacer may fill.
     *
     * @return the inner (content) height of the current page canvas, in points
     * @since 1.7.0
     */
    public double availableHeight() {
        return canvas().innerHeight();
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
        long revision = layoutCache.revision();
        if (layoutCache.isLayoutCached()) {
            LayoutGraph cached = layoutCache.layout(() -> {
                throw new IllegalStateException("Cache miss after isLayoutCached() returned true.");
            });
            LIFECYCLE_LOG.debug("document.layout.cache.hit sessionId={} revision={} roots={} pages={} nodes={} fragments={}", sessionId, revision, roots.size(), cached.totalPages(), cached.nodes().size(), cached.fragments().size());
            return cached;
        }
        long startNanos = System.nanoTime();
        LIFECYCLE_LOG.debug("document.layout.start sessionId={} revision={} roots={}", sessionId, revision, roots.size());
        try {
            LayoutGraph computed = layoutCache.layout(this::computeLayout);
            LIFECYCLE_LOG.debug("document.layout.end sessionId={} revision={} roots={} pages={} nodes={} fragments={} durationMs={}", sessionId, revision, roots.size(), computed.totalPages(), computed.nodes().size(), computed.fragments().size(), elapsedMillis(startNanos));
            return computed;
        } catch (RuntimeException ex) {
            LIFECYCLE_LOG.warn("document.layout.failed sessionId={} revision={} roots={} errorType={}", sessionId, revision, roots.size(), ex.getClass().getSimpleName());
            throw ex;
        }
    }

    /**
     * Compiles the semantic graph for this layout revision and splices in any
     * session-wide page-background fills. The convergence fixed point lives in
     * {@link DocumentLayoutResolver}; a document without a page reference or
     * per-page margins compiles once and is byte-identical to before. All passes
     * run inside the cache compute, so the result is cached once per revision.
     */
    private LayoutGraph computeLayout() {
        return DocumentPageBackgrounds.apply(layoutResolver.resolve(), pageBackgrounds);
    }

    private PageGeometry buildPageGeometry() {
        if (pageMargins.isEmpty()) {
            return null;
        }
        List<PageMarginOverride> overrides = new ArrayList<>(pageMargins.size());
        for (PageMarginRule rule : pageMargins) {
            int fromIndex = rule.fromPage() - 1;
            int toIndexExclusive = rule.toPageExclusive() == Integer.MAX_VALUE
                    ? Integer.MAX_VALUE
                    : rule.toPageExclusive() - 1;
            overrides.add(PageMarginOverride.fromInsets(fromIndex, toIndexExclusive, rule.insets()));
        }
        return PageGeometry.of(canvas, overrides);
    }

    private static boolean containsPageReference(List<DocumentNode> nodes) {
        for (DocumentNode node : nodes) {
            if (node instanceof PageReferenceNode || containsPageReference(node.children())) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Integer> resolvedPageNumbers(LayoutGraph graph) {
        Map<String, Integer> pages = new HashMap<>();
        PageIndexExtractor.from(graph).all().forEach((anchor, reference) -> pages.put(anchor, reference.pageNumber()));
        return pages;
    }

    /**
     * Extracts the current deterministic layout snapshot used by regression tests.
     *
     * @return layout snapshot derived from the current layout graph
     */
    public LayoutSnapshot layoutSnapshot() {
        ensureOpen();
        long revision = layoutCache.revision();
        if (layoutCache.isSnapshotCached()) {
            LayoutSnapshot cached = layoutCache.snapshot(() -> {
                throw new IllegalStateException("Snapshot cache miss after isSnapshotCached() returned true.");
            });
            LIFECYCLE_LOG.debug("document.layoutSnapshot.cache.hit sessionId={} revision={} roots={}", sessionId, revision, roots.size());
            return cached;
        }
        long startNanos = System.nanoTime();
        LIFECYCLE_LOG.debug("document.layoutSnapshot.start sessionId={} revision={} roots={}", sessionId, revision, roots.size());
        LayoutSnapshot computed = layoutCache.snapshot(() -> LayoutGraphSnapshotExtractor.extract(layoutGraph()));
        LIFECYCLE_LOG.debug("document.layoutSnapshot.end sessionId={} revision={} roots={} pages={} durationMs={}", sessionId, revision, roots.size(), computed.totalPages(), elapsedMillis(startNanos));
        return computed;
    }

    /**
     * Resolves every declared {@code anchor(...)} to its final page in a single,
     * backend-neutral pass over the laid-out document — the foundation for
     * cross-references ("see page N") and clickable tables of contents.
     *
     * <p>Computed from the resolved layout graph (not from rendered output) and
     * cached per layout revision alongside {@link #layoutSnapshot()}. A duplicate
     * anchor resolves to its last registration, matching where a
     * {@code linkTo(anchor)} jumps.</p>
     *
     * @return the resolved anchor-to-page index
     * @throws IllegalStateException if this session has already been closed
     * @since 1.9.0
     */
    public PageIndex pageIndex() {
        ensureOpen();
        long revision = layoutCache.revision();
        if (layoutCache.isPageIndexCached()) {
            PageIndex cached = layoutCache.pageIndex(() -> {
                throw new IllegalStateException("PageIndex cache miss after isPageIndexCached() returned true.");
            });
            LIFECYCLE_LOG.debug("document.pageIndex.cache.hit sessionId={} revision={} roots={}", sessionId, revision, roots.size());
            return cached;
        }
        long startNanos = System.nanoTime();
        PageIndex computed = layoutCache.pageIndex(() -> PageIndexExtractor.from(layoutGraph()));
        LIFECYCLE_LOG.debug("document.pageIndex.end sessionId={} revision={} roots={} anchors={} durationMs={}", sessionId, revision, roots.size(), computed.all().size(), elapsedMillis(startNanos));
        return computed;
    }

    /**
     * Renders the current layout graph with the supplied fixed-layout backend.
     *
     * @param backend backend implementation that consumes the resolved layout graph
     * @param <R>     backend-specific result type
     * @return backend render result
     * @throws Exception if rendering fails
     */
    public <R> R render(FixedLayoutBackend<R> backend) throws Exception {
        return renderingFacade.render(backend, defaultOutputFile);
    }

    /**
     * Renders the current layout graph with an explicit output target override.
     *
     * @param backend    backend implementation that consumes the resolved layout graph
     * @param outputFile optional output target for backends that persist to disk
     * @param <R>        backend-specific result type
     * @return backend render result
     * @throws Exception if rendering fails
     */
    public <R> R render(FixedLayoutBackend<R> backend, Path outputFile) throws Exception {
        return renderingFacade.render(backend, outputFile);
    }

    /**
     * Exports the semantic graph through a semantic backend using the default output file.
     *
     * @param backend semantic backend implementation
     * @param <R>     backend-specific result type
     * @return export result
     * @throws Exception if export fails
     */
    public <R> R export(SemanticBackend<R> backend) throws Exception {
        return renderingFacade.export(backend, defaultOutputFile);
    }

    /**
     * Exports the semantic graph through a semantic backend using an explicit output target.
     *
     * @param backend    semantic backend implementation
     * @param outputFile optional output file override
     * @param <R>        backend-specific result type
     * @return export result
     * @throws Exception if export fails
     */
    public <R> R export(SemanticBackend<R> backend, Path outputFile) throws Exception {
        return renderingFacade.export(backend, outputFile);
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
     * @throws DocumentRenderingException if PDF rendering fails
     */
    public byte[] toPdfBytes() throws DocumentRenderingException {
        return wrapRendering("render PDF bytes", renderingFacade::toPdfBytes);
    }

    /**
     * Streams the current session through the canonical PDF backend.
     *
     * <p>The caller owns the supplied stream. GraphCompose writes the PDF bytes
     * but does not close the stream, which makes this method suitable for HTTP
     * responses, cloud storage uploads, and other server-side streaming paths.</p>
     *
     * @param output destination stream that receives the rendered PDF bytes
     * @throws DocumentRenderingException if PDF rendering fails
     */
    public void writePdf(OutputStream output) throws DocumentRenderingException {
        wrapRendering("write PDF to stream", () -> {
            renderingFacade.writePdf(output);
            return null;
        });
    }

    /**
     * Builds the current document into the default output file configured on the builder.
     *
     * @throws IllegalStateException      if no default output file was configured
     * @throws DocumentRenderingException if PDF rendering fails
     */
    public void buildPdf() throws DocumentRenderingException {
        ensureOpen();
        if (defaultOutputFile == null) {
            throw new IllegalStateException("No default output file was configured for this document session.");
        }
        wrapRendering("build PDF at '" + defaultOutputFile + "'", () -> {
            renderingFacade.buildPdf(defaultOutputFile);
            return null;
        });
    }

    /**
     * Builds the current document into the supplied output file.
     *
     * @param outputFile destination PDF path
     * @throws DocumentRenderingException if PDF rendering fails
     */
    public void buildPdf(Path outputFile) throws DocumentRenderingException {
        wrapRendering("build PDF at '" + outputFile + "'", () -> {
            renderingFacade.buildPdf(outputFile);
            return null;
        });
    }

    /**
     * Renders the current session through the fixed-layout PPTX backend and
     * returns the .pptx bytes. One resolved page becomes one identically-sized
     * slide with the exact same geometry the PDF backend paints; the session's
     * chrome (metadata, watermark, headers/footers) applies the same way as in
     * {@link #toPdfBytes()}, with options PPTX cannot honour skipped with a
     * one-time warning.
     *
     * <p>Requires {@code io.github.demchaav:graph-compose-render-pptx} on the
     * classpath; without it the render fails with a
     * {@link com.demcha.compose.document.exceptions.MissingBackendException}
     * naming the artifact to add. The returned byte array is not cached by the
     * session, so server code that can stream should prefer
     * {@link #writePptx(OutputStream)}.</p>
     *
     *
     * <p><b>Experimental</b> ({@code @Beta}): the PPTX backend ships its
     * first release in 2.1.0, so the PPTX-facing contract may still change in
     * a minor release — see {@code docs/api-stability.md}.</p>
     * @return rendered .pptx bytes
     * @throws DocumentRenderingException if PPTX rendering fails
     * @since 2.1.0
     */
    @Beta
    public byte[] toPptxBytes() throws DocumentRenderingException {
        return wrapRendering("render PPTX bytes", renderingFacade::toPptxBytes);
    }

    /**
     * Streams the current session through the fixed-layout PPTX backend. See
     * {@link #toPptxBytes()} for the geometry and classpath contract.
     *
     * <p>The caller owns the supplied stream: GraphCompose writes the deck
     * bytes but does not close the stream.</p>
     *
     * @param output destination stream that receives the rendered .pptx bytes
     * @throws DocumentRenderingException if PPTX rendering fails
     * @since 2.1.0
     */
    @Beta
    public void writePptx(OutputStream output) throws DocumentRenderingException {
        wrapRendering("write PPTX to stream", () -> {
            renderingFacade.writePptx(output);
            return null;
        });
    }

    /**
     * Builds the current document as a .pptx deck into the supplied output
     * file. See {@link #toPptxBytes()} for the geometry and classpath contract.
     *
     * @param outputFile destination .pptx path
     * @throws DocumentRenderingException if PPTX rendering fails
     * @since 2.1.0
     */
    @Beta
    public void buildPptx(Path outputFile) throws DocumentRenderingException {
        wrapRendering("build PPTX at '" + outputFile + "'", () -> {
            renderingFacade.buildPptx(outputFile);
            return null;
        });
    }

    /**
     * Renders every page of the current document to a raster image at the given
     * resolution, with an opaque white background.
     *
     * <p>Renders the in-memory document directly to images — without the
     * intermediate PDF byte array of {@link #toPdfBytes()} followed by a
     * {@code Loader.loadPDF(...)} re-parse — so it avoids that serialize-then-reparse
     * round-trip. Useful for page previews, thumbnails, and pixel diffing.</p>
     *
     * @param dpi target resolution in dots per inch (72 = native page size); must be {@code > 0}
     * @return one image per page, in page order
     * @throws IllegalArgumentException   if {@code dpi <= 0}
     * @throws DocumentRenderingException if rendering fails
     * @since 1.9.0
     */
    public List<BufferedImage> toImages(int dpi) throws DocumentRenderingException {
        return toImages(dpi, false);
    }

    /**
     * Renders every page of the current document to a raster image at the given
     * resolution. See {@link #toImages(int)} for the no-round-trip rationale.
     *
     * @param dpi         target resolution in dots per inch; must be {@code > 0}
     * @param transparent {@code true} for a transparent (ARGB) background, {@code false} for opaque white (RGB)
     * @return one image per page, in page order
     * @throws IllegalArgumentException   if {@code dpi <= 0}
     * @throws DocumentRenderingException if rendering fails
     * @since 1.9.0
     */
    public List<BufferedImage> toImages(int dpi, boolean transparent) throws DocumentRenderingException {
        requirePositiveDpi(dpi);
        return wrapImageRendering("render images at " + dpi + " DPI",
                () -> renderingFacade.renderImages(dpi, transparent, -1));
    }

    /**
     * Renders a single page of the current document to a raster image at the given
     * resolution, with an opaque white background.
     *
     * <p>Each call rebuilds the whole document, so to rasterize several pages prefer
     * {@link #toImages(int)} — one build, every page.</p>
     *
     * @param pageIndex zero-based page index
     * @param dpi       target resolution in dots per inch; must be {@code > 0}
     * @return the rendered page image
     * @throws IllegalArgumentException   if {@code dpi <= 0}
     * @throws IndexOutOfBoundsException  if {@code pageIndex} is out of range
     * @throws DocumentRenderingException if rendering fails
     * @since 1.9.0
     */
    public BufferedImage toImage(int pageIndex, int dpi) throws DocumentRenderingException {
        return toImage(pageIndex, dpi, false);
    }

    /**
     * Renders a single page of the current document to a raster image at the given
     * resolution.
     *
     * @param pageIndex   zero-based page index
     * @param dpi         target resolution in dots per inch; must be {@code > 0}
     * @param transparent {@code true} for a transparent (ARGB) background, {@code false} for opaque white (RGB)
     * @return the rendered page image
     * @throws IllegalArgumentException   if {@code dpi <= 0}
     * @throws IndexOutOfBoundsException  if {@code pageIndex} is out of range
     * @throws DocumentRenderingException if rendering fails
     * @since 1.9.0
     */
    public BufferedImage toImage(int pageIndex, int dpi, boolean transparent) throws DocumentRenderingException {
        requirePositiveDpi(dpi);
        if (pageIndex < 0) {
            throw new IndexOutOfBoundsException("pageIndex must be >= 0, got " + pageIndex);
        }
        return wrapImageRendering("render page " + pageIndex + " at " + dpi + " DPI",
                () -> renderingFacade.renderImages(dpi, transparent, pageIndex).get(0));
    }

    private static void requirePositiveDpi(int dpi) {
        if (dpi <= 0) {
            throw new IllegalArgumentException("dpi must be > 0, got " + dpi);
        }
    }

    /**
     * Closes measurement resources owned by the session.
     *
     * <p>This method is idempotent: a second call is a no-op. After the session
     * is closed, every public authoring or rendering method throws
     * {@link IllegalStateException}.</p>
     *
     * <p>The {@link AutoCloseable#close()} contract permits throwing
     * {@link Exception}, but GraphCompose narrows the declaration to
     * {@link DocumentRenderingException} so callers using
     * {@code try (DocumentSession session = ...) { ... }} do not need to
     * declare or catch a checked {@code Exception} on the implicit close.
     * Any underlying {@link java.io.IOException} from PDFBox is preserved
     * as the {@linkplain Throwable#getCause() cause}.</p>
     *
     * @throws DocumentRenderingException if measurement resource close fails
     */
    @Override
    public void close() throws DocumentRenderingException {
        if (closed) {
            LIFECYCLE_LOG.debug("document.session.close.skip sessionId={} revision={}", sessionId, layoutCache.revision());
            return;
        }
        closed = true;
        LIFECYCLE_LOG.debug("document.session.close.start sessionId={} revision={} roots={}", sessionId, layoutCache.revision(), roots.size());
        try {
            if (measurementResources != null) {
                measurementResources.close();
            }
            LIFECYCLE_LOG.debug("document.session.close.end sessionId={} revision={} roots={}", sessionId, layoutCache.revision(), roots.size());
        } catch (RuntimeException ex) {
            LIFECYCLE_LOG.warn("document.session.close.failed sessionId={} revision={} errorType={}", sessionId, layoutCache.revision(), ex.getClass().getSimpleName());
            throw ex;
        } catch (Exception ex) {
            LIFECYCLE_LOG.warn("document.session.close.failed sessionId={} revision={} errorType={}", sessionId, layoutCache.revision(), ex.getClass().getSimpleName());
            throw new DocumentRenderingException("Failed to close measurement resources: " + ex.getMessage(), ex);
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
                    "Cannot render an empty document. Add at least one root before calling a "
                    + "render or output method (writePdf/toPdfBytes/buildPdf, "
                    + "writePptx/toPptxBytes/buildPptx, toImages/toImage).");
        }
    }

    private void refreshMeasurementServices() {
        try {
            if (measurementResources != null) {
                measurementResources.close();
            }
        } catch (Exception ignored) {
            // Best-effort close while reloading measurement resources.
        }

        measurementResources = BackendProviders.fontMetrics().openMeasurement(customFontFamilies);
    }

    private void invalidate() {
        layoutCache.invalidate();
    }

    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    @FunctionalInterface
    private interface RenderingBody<R> {
        R run() throws Exception;
    }

    @FunctionalInterface
    private interface ImageRenderingBody<R> {
        R run() throws Exception;
    }

    /**
     * Session-owned {@link NodeRegistry} subclass that funnels every
     * {@link #register(NodeDefinition)} call through both
     * {@link DocumentSession#ensureOpen()} and
     * {@link DocumentSession#invalidate()}. The two registration entry
     * points — {@code session.registry().register(...)} and
     * {@link DocumentSession#registerNodeDefinition(NodeDefinition)} —
     * are now fully interchangeable: both refuse to mutate a closed
     * session and both invalidate the cached compile.
     *
     * <p>Added in v1.6.7 (Track I3) as cache-invalidation only; the
     * {@code ensureOpen()} symmetry landed in v1.6.8 (Track J2) after
     * the senior review noted that the two entry points still
     * disagreed on closed-session behaviour.</p>
     */
    private final class InvalidatingNodeRegistry extends NodeRegistry {
        @Override
        public <E extends DocumentNode> NodeRegistry register(NodeDefinition<E> definition) {
            ensureOpen();
            super.register(definition);
            invalidate();
            return this;
        }
    }

    /**
     * Captures this session as one section of a {@link MultiSectionDocument}: its
     * resolved layout graph, page geometry, custom fonts, and chrome (header /
     * footer / watermark / metadata). Package-private — used only by
     * {@link MultiSectionDocument} to concatenate sessions into one PDF.
     *
     * @return a render unit for this section
     */
    SectionUnit toSectionRenderUnit() {
        ensureOpen();
        ensureRenderable();
        return new SectionUnit(
                layoutGraph(),
                canvas,
                List.copyOf(customFontFamilies),
                chromeOptions.toConvenienceBackend("pdf", debug));
    }

    /**
     * Inner adapter exposing session-private state to {@link DocumentRenderingFacade}
     * without making the corresponding session methods public.
     */
    private final class RenderingContextImpl implements DocumentRenderingFacade.Context {
        @Override
        public void ensureOpen() {
            DocumentSession.this.ensureOpen();
        }

        @Override
        public void ensureRenderable() {
            DocumentSession.this.ensureRenderable();
        }

        @Override
        public String sessionId() {
            return sessionId;
        }

        @Override
        public long revision() {
            return layoutCache.revision();
        }

        @Override
        public int rootCount() {
            return roots.size();
        }

        @Override
        public LayoutCanvas canvas() {
            return canvas;
        }

        @Override
        public List<FontFamilyDefinition> customFontFamilies() {
            return List.copyOf(customFontFamilies);
        }

        @Override
        public LayoutGraph layoutGraph() {
            return DocumentSession.this.layoutGraph();
        }

        @Override
        public DocumentGraph documentGraph() {
            return DocumentSession.this.documentGraph();
        }

        @Override
        public DocumentOutputOptions outputOptions() {
            return chromeOptions.snapshot();
        }

        @Override
        public FixedLayoutRenderer convenienceBackend(String format) {
            return chromeOptions.toConvenienceBackend(format, debug);
        }
    }

    private final class LayoutResolverContext implements DocumentLayoutResolver.Context {
        @Override
        public NodeRegistry registry() {
            return registry;
        }

        @Override
        public LayoutCompiler compiler() {
            return compiler;
        }

        @Override
        public LayoutCanvas canvas() {
            return canvas;
        }

        @Override
        public MeasurementResources measurementResources() {
            return measurementResources;
        }

        @Override
        public boolean markdown() {
            return markdown;
        }

        @Override
        public DocumentGraph documentGraph() {
            return DocumentSession.this.documentGraph();
        }

        @Override
        public PageGeometry pageGeometry() {
            return buildPageGeometry();
        }

        @Override
        public boolean hasPageReference() {
            return containsPageReference(roots);
        }

        @Override
        public Map<String, Integer> resolvePageNumbers(LayoutGraph graph) {
            return resolvedPageNumbers(graph);
        }

        @Override
        public String sessionId() {
            return sessionId;
        }
    }
}
