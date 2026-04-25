package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.font.FontLibrary;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    private final Map<PreparedNodeCacheKey, PreparedNode<?>> preparedNodes = new HashMap<>();

    /**
     * Creates a layout-pass context.
     *
     * @param registry semantic node registry used for preparation
     * @param canvas active layout canvas
     * @param fontLibrary document font library
     * @param textMeasurementSystem text measurement service for this pass
     * @param markdown whether paragraph markdown parsing is enabled
     */
    public DocumentLayoutPassContext(NodeRegistry registry,
                                     LayoutCanvas canvas,
                                     FontLibrary fontLibrary,
                                     TextMeasurementSystem textMeasurementSystem,
                                     boolean markdown) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.canvas = Objects.requireNonNull(canvas, "canvas");
        this.fontLibrary = Objects.requireNonNull(fontLibrary, "fontLibrary");
        this.textMeasurementSystem = Objects.requireNonNull(textMeasurementSystem, "textMeasurementSystem");
        this.markdown = markdown;
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
