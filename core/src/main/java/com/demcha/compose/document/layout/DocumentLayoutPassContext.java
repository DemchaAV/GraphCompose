package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.font.FontLibrary;

import java.util.ArrayList;
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
     * Compiler used to lay out a composite child inside a box its parent owns
     * (see {@link #emitChildFragments}). Created on first use so a pass with no
     * composed cells never builds one; a pass is single-threaded, and the
     * compiler carries no state beyond the registry, so one instance serves
     * every box in the pass.
     */
    private LayoutCompiler subtreeCompiler;

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
        if (child.isComposite()) {
            return emitCompositeSubtree((PreparedNode<DocumentNode>) child, placement);
        }
        NodeDefinition<E> definition = (NodeDefinition<E>) registry.definitionFor(child.node());
        return definition.emitFragments(child, this, placement);
    }

    /**
     * Lays out a composite child's whole sub-tree inside {@code placement} and
     * returns its fragments in the placement's local coordinate space.
     *
     * <p>A composite's own {@code emitFragments} yields nothing but its
     * decoration — the section background, the container border — because the
     * compiler, not the definition, walks {@link NodeDefinition#children}.
     * Dispatching to it alone would leave the caller with a correctly measured
     * but empty box. The fixed-box walk applies the same column / row / stack
     * layout the sub-tree would get at document level, then the absolute
     * coordinates it produces are rebased onto the placement so the caller can
     * translate them into its own fragment space exactly as it does for a leaf
     * child.</p>
     */
    private List<LayoutFragment> emitCompositeSubtree(PreparedNode<DocumentNode> child,
                                                      FragmentPlacement placement) {
        if (subtreeCompiler == null) {
            subtreeCompiler = new LayoutCompiler(registry);
        }
        List<PlacedFragment> placed = subtreeCompiler.compileFixedBoxSubtree(
                child,
                placement.parentPath(),
                placement.childIndex(),
                placement.depth(),
                placement.x(),
                placement.y() + placement.height(),
                placement.width(),
                placement.pageIndex(),
                canvas,
                this,
                this);
        if (placed.isEmpty()) {
            return List.of();
        }
        List<LayoutFragment> local = new ArrayList<>(placed.size());
        for (PlacedFragment fragment : placed) {
            local.add(new LayoutFragment(
                    fragment.path(),
                    fragment.fragmentIndex(),
                    fragment.x() - placement.x(),
                    fragment.y() - placement.y(),
                    fragment.width(),
                    fragment.height(),
                    fragment.payload()));
        }
        return List.copyOf(local);
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
