package com.demcha.compose.document.api;

import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.NodeRegistry;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.snapshot.LayoutSnapshot;

import java.util.List;
import java.util.Objects;

/**
 * Fluent facade for the layout-introspection methods of a
 * {@link DocumentSession}. Returned by {@link DocumentSession#layout()}.
 *
 * <p>This facade exists to slim {@code DocumentSession}'s top-level
 * surface (audit finding H1) by grouping the read-only layout
 * inspection calls behind a single accessor. The equivalent top-level
 * methods on {@code DocumentSession} continue to work unchanged for
 * backward compatibility — both styles return the same underlying
 * objects.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * try (DocumentSession session = GraphCompose.document().create()) {
 *     session.compose(dsl -> dsl.pageFlow(flow -> flow.addText("hi")));
 *     LayoutGraph graph = session.layout().graph();
 *     int totalPages = graph.totalPages();
 *     LayoutSnapshot snapshot = session.layout().snapshot();
 * }
 * }</pre>
 *
 * <p>Use {@link #session()} to chain back to the owning session if you
 * prefer a single fluent expression that mixes layout inspection and
 * other session calls.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.6.0
 */
public final class SessionLayoutApi {

    private final DocumentSession session;

    /**
     * Package-private constructor — instances are created exclusively via
     * {@link DocumentSession#layout()}.
     */
    SessionLayoutApi(DocumentSession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    /**
     * Compiles the semantic graph into a resolved, paginated layout
     * graph. Equivalent to {@link DocumentSession#layoutGraph()}.
     *
     * @return cached or freshly compiled layout graph
     * @throws IllegalStateException if the owning session has already been closed
     */
    public LayoutGraph graph() {
        return session.layoutGraph();
    }

    /**
     * Returns an immutable snapshot of the current semantic root graph.
     * Equivalent to {@link DocumentSession#documentGraph()}.
     *
     * @return document graph snapshot
     */
    public DocumentGraph documentGraph() {
        return session.documentGraph();
    }

    /**
     * Returns an immutable copy of the current semantic roots.
     * Equivalent to {@link DocumentSession#roots()}.
     *
     * @return semantic root nodes in insertion order
     */
    public List<DocumentNode> roots() {
        return session.roots();
    }

    /**
     * Returns the current semantic layout canvas derived from page size
     * and margin. Equivalent to {@link DocumentSession#canvas()}.
     *
     * @return current layout canvas
     */
    public LayoutCanvas canvas() {
        return session.canvas();
    }

    /**
     * Returns the mutable node registry used by this session.
     * Equivalent to {@link DocumentSession#registry()}.
     *
     * @return active node registry
     */
    public NodeRegistry registry() {
        return session.registry();
    }

    /**
     * Extracts a deterministic {@link LayoutSnapshot} for snapshot-style
     * regression testing. Equivalent to
     * {@link DocumentSession#layoutSnapshot()}.
     *
     * @return immutable layout snapshot
     * @throws IllegalStateException if the owning session has already been closed
     */
    public LayoutSnapshot snapshot() {
        return session.layoutSnapshot();
    }

    /**
     * Returns the owning {@link DocumentSession}, allowing the layout
     * inspection chain to continue with authoring or rendering calls in
     * a single fluent expression.
     *
     * @return owning session
     */
    public DocumentSession session() {
        return session;
    }
}
