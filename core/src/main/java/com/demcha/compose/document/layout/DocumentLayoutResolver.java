package com.demcha.compose.document.layout;

import com.demcha.compose.document.backend.fixed.MeasurementResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Runs the canonical layout's coupled fixed point, pulled out of
 * {@code DocumentSession} so the convergence algorithm is a focused, testable
 * unit rather than a method buried in the session facade.
 *
 * <p>A document that contains a page reference or per-page margins is compiled in
 * repeated passes: page references resolve their numbers, and per-page margins
 * resolve which page each top-level block begins on. A pass feeds both back; the
 * layout is settled once neither changes, capped at {@link #MAX_PASSES} recompiles
 * so a document that never converges still returns its last (best) layout. A
 * document with neither compiles exactly once and is byte-identical to before.</p>
 *
 * <p>Everything the loop needs is read through the small {@link Context} interface
 * that {@code DocumentSession} implements internally — including the inputs that
 * derive from canonical/debug types (page geometry, page-reference resolution),
 * which the session pre-computes so this layout-package class depends only on
 * layout types.</p>
 *
 * @author Artem Demchyshyn
 */
public final class DocumentLayoutResolver {

    /**
     * Cap on recompiles after the first resolve. A table of contents converges in
     * one recompile; the cap bounds a pathological document whose numbers keep
     * shifting pages, falling back to the last layout.
     */
    static final int MAX_PASSES = 5;

    /**
     * Opt-in build / test diagnostic. When the system property
     * {@code graphcompose.failOnUnconvergedLayout} is set, a document whose layout
     * does not settle within {@link #MAX_PASSES} passes throws instead of silently
     * returning its last iteration — surfacing the rare non-converging document in a
     * test run rather than shipping a subtly-wrong render. Off by default, so the
     * production path and its output are unchanged.
     */
    static final String FAIL_ON_UNCONVERGED_PROPERTY = "graphcompose.failOnUnconvergedLayout";

    private static final Logger LIFECYCLE_LOG = LoggerFactory.getLogger("com.demcha.compose.document.lifecycle");

    private final Context context;

    public DocumentLayoutResolver(Context context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    /**
     * Compiles the semantic graph for one layout revision to its fixed point.
     * Returns the resolved layout graph <em>before</em> any page-background fills
     * are spliced in — the session applies those around this result. A document
     * that does not settle within {@link #MAX_PASSES} passes returns its last
     * iteration, unless {@link #FAIL_ON_UNCONVERGED_PROPERTY} is set.
     *
     * @return the settled (or last, if uncapped) layout graph
     * @throws IllegalStateException if the layout never converges and
     *                               {@link #FAIL_ON_UNCONVERGED_PROPERTY} is set
     */
    public LayoutGraph resolve() {
        PageGeometry geometry = context.pageGeometry();
        // Only a horizontal difference makes the loop load-bearing: a block's
        // assigned start page feeds nothing but the page's content width and left
        // edge. Overrides that move the top or bottom margin — a continuation-page
        // safe area, say — leave one width for the whole document, which the first
        // pass already used, so re-running it would return the same graph.
        boolean hasMargins = geometry != null && geometry.hasHorizontalOverrides();
        boolean hasPageReference = context.hasPageReference();

        LayoutGraph graph = compilePass(Map.of(), geometry, Map.of());
        if (!hasMargins && !hasPageReference) {
            return graph;
        }
        // Two coupled fixed points share one loop: page references resolve their
        // numbers, and per-page margins resolve which page each top-level block
        // begins on (its content width). A pass feeds back both; the layout is
        // settled once neither changes.
        Map<String, Integer> resolved = hasPageReference ? context.resolvePageNumbers(graph) : Map.of();
        Map<String, Integer> startPages = hasMargins ? nodeStartPages(graph) : Map.of();
        for (int pass = 0; pass < MAX_PASSES; pass++) {
            graph = compilePass(resolved, geometry, startPages);
            Map<String, Integer> renderedPages = hasPageReference ? context.resolvePageNumbers(graph) : Map.of();
            Map<String, Integer> renderedStarts = hasMargins ? nodeStartPages(graph) : Map.of();
            if (renderedPages.equals(resolved) && renderedStarts.equals(startPages)) {
                return graph;
            }
            resolved = renderedPages;
            startPages = renderedStarts;
        }
        if (Boolean.getBoolean(FAIL_ON_UNCONVERGED_PROPERTY)) {
            throw new IllegalStateException(
                    "Document layout did not converge within " + MAX_PASSES + " passes (sessionId="
                            + context.sessionId() + "); the coupled page-reference / per-page-margin"
                            + " fixed point did not settle.");
        }
        LIFECYCLE_LOG.debug("document.layout.unconverged sessionId={} passes={}", context.sessionId(), MAX_PASSES);
        return graph;
    }

    private LayoutGraph compilePass(Map<String, Integer> resolvedPages,
                                    PageGeometry geometry,
                                    Map<String, Integer> nodeStartPages) {
        DocumentLayoutPassContext passContext = new DocumentLayoutPassContext(
                context.registry(),
                context.canvas(),
                context.measurementResources().fontLibrary(),
                context.measurementResources().textMeasurementSystem(),
                context.markdown(),
                resolvedPages, geometry, nodeStartPages);
        return context.compiler().compile(context.documentGraph(), passContext, passContext);
    }

    private static Map<String, Integer> nodeStartPages(LayoutGraph graph) {
        Map<String, Integer> starts = new HashMap<>();
        for (PlacedNode node : graph.nodes()) {
            starts.put(node.path(), node.startPage());
        }
        return starts;
    }

    /**
     * The layout inputs the resolver reads, implemented by {@code DocumentSession}.
     * The session pre-computes the values that derive from canonical / debug types
     * ({@link #pageGeometry()}, {@link #hasPageReference()},
     * {@link #resolvePageNumbers(LayoutGraph)}) so this class stays within the
     * layout package.
     */
    public interface Context {
        NodeRegistry registry();

        LayoutCompiler compiler();

        LayoutCanvas canvas();

        MeasurementResources measurementResources();

        boolean markdown();

        DocumentGraph documentGraph();

        /** Per-page margin overrides for this revision, or {@code null} if none. */
        PageGeometry pageGeometry();

        /** Whether the document contains a page reference (drives the fixed point). */
        boolean hasPageReference();

        /** Resolves each anchor's currently-rendered page number from a compiled graph. */
        Map<String, Integer> resolvePageNumbers(LayoutGraph graph);

        String sessionId();
    }
}
