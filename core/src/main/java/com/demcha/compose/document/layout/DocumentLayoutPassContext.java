package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.font.FontLibrary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Internal context for one canonical document layout pass.
 *
 * <p>The context owns prepared-node caching and the backend measurement services
 * needed by node definitions. Keeping this state in the layout package lets the
 * public {@code DocumentSession} remain a lifecycle/API facade rather than a
 * holder of engine measurement details.</p>
 *
 * @author Artem Demchyshyn
 */
public final class DocumentLayoutPassContext implements PrepareContext, FragmentContext {
    private final NodeRegistry registry;
    private final LayoutCanvas canvas;
    private final FontLibrary fontLibrary;
    private final TextMeasurementSystem textMeasurementSystem;
    private final boolean markdown;
    private final Map<String, Integer> resolvedPages;
    private final PageGeometry pageGeometry;
    private final Map<String, Integer> nodeStartPages;
    private final Map<PreparedNodeCacheKey, PreparedNode<?>> preparedNodes = new HashMap<>();

    /**
     * Creates a layout-pass context with no resolved page numbers — the first
     * pass, where page-reference nodes render their placeholder.
     *
     * @param registry              semantic node registry used for preparation
     * @param canvas                active layout canvas
     * @param fontLibrary           document font library
     * @param textMeasurementSystem text measurement service for this pass
     * @param markdown              whether paragraph markdown parsing is enabled
     */
    public DocumentLayoutPassContext(NodeRegistry registry,
                                     LayoutCanvas canvas,
                                     FontLibrary fontLibrary,
                                     TextMeasurementSystem textMeasurementSystem,
                                     boolean markdown) {
        this(registry, canvas, fontLibrary, textMeasurementSystem, markdown, Map.of());
    }

    /**
     * Creates a layout-pass context carrying resolved anchor page numbers — the
     * second pass of a page-reference / table-of-contents resolve.
     *
     * @param registry              semantic node registry used for preparation
     * @param canvas                active layout canvas
     * @param fontLibrary           document font library
     * @param textMeasurementSystem text measurement service for this pass
     * @param markdown              whether paragraph markdown parsing is enabled
     * @param resolvedPages         anchor name to 1-based page number
     */
    public DocumentLayoutPassContext(NodeRegistry registry,
                                     LayoutCanvas canvas,
                                     FontLibrary fontLibrary,
                                     TextMeasurementSystem textMeasurementSystem,
                                     boolean markdown,
                                     Map<String, Integer> resolvedPages) {
        this(registry, canvas, fontLibrary, textMeasurementSystem, markdown, resolvedPages, null, Map.of());
    }

    /**
     * Creates a layout-pass context carrying per-page margin geometry and the
     * previous pass's top-level block page assignments — the per-page-margin fixed
     * point. A document with no per-page margins passes {@code null} geometry and an
     * empty assignment map and is laid out exactly as before.
     *
     * @param registry              semantic node registry used for preparation
     * @param canvas                active layout canvas (the document-wide fallback)
     * @param fontLibrary           document font library
     * @param textMeasurementSystem text measurement service for this pass
     * @param markdown              whether paragraph markdown parsing is enabled
     * @param resolvedPages         anchor name to 1-based page number
     * @param pageGeometry          per-page geometry resolver, or {@code null}
     * @param nodeStartPages        node path to its 0-based start page
     */
    public DocumentLayoutPassContext(NodeRegistry registry,
                                     LayoutCanvas canvas,
                                     FontLibrary fontLibrary,
                                     TextMeasurementSystem textMeasurementSystem,
                                     boolean markdown,
                                     Map<String, Integer> resolvedPages,
                                     PageGeometry pageGeometry,
                                     Map<String, Integer> nodeStartPages) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.canvas = Objects.requireNonNull(canvas, "canvas");
        this.fontLibrary = Objects.requireNonNull(fontLibrary, "fontLibrary");
        this.textMeasurementSystem = Objects.requireNonNull(textMeasurementSystem, "textMeasurementSystem");
        this.markdown = markdown;
        this.resolvedPages = resolvedPages == null ? Map.of() : Map.copyOf(resolvedPages);
        this.pageGeometry = pageGeometry;
        this.nodeStartPages = nodeStartPages == null ? Map.of() : Map.copyOf(nodeStartPages);
    }

    @Override
    public OptionalInt resolvedPage(String anchor) {
        Integer page = anchor == null ? null : resolvedPages.get(anchor);
        return page == null ? OptionalInt.empty() : OptionalInt.of(page);
    }

    @Override
    public PageGeometry pageGeometry() {
        return pageGeometry;
    }

    @Override
    public int assignedStartPage(String path, int fallback) {
        Integer assigned = path == null ? null : nodeStartPages.get(path);
        return assigned == null ? fallback : assigned;
    }

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

    @Override
    public boolean markdownEnabled() {
        return markdown;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends DocumentNode> List<LayoutFragment> emitChildFragments(
            PreparedNode<E> child,
            FragmentPlacement placement) {
        Objects.requireNonNull(child, "child");
        Objects.requireNonNull(placement, "placement");
        NodeDefinition<E> definition = (NodeDefinition<E>) registry.definitionFor(child.node());
        return definition.emitFragments(child, this, placement);
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
