package com.demcha.compose.document.api;

import com.demcha.compose.document.layout.NodeDefinition;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.font.FontFamilyDefinition;

import java.util.Objects;

/**
 * Fluent facade for the font + node-extension registration calls on a
 * {@link DocumentSession}. Returned by {@link DocumentSession#fonts()}.
 *
 * <p>This facade exists to slim {@code DocumentSession}'s top-level
 * surface (audit finding H1) by grouping the document-local font and
 * extension registration calls behind a single accessor. The equivalent
 * top-level methods on {@code DocumentSession} continue to work
 * unchanged for backward compatibility — both styles mutate the same
 * underlying state.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * try (DocumentSession session = GraphCompose.document(out).create()) {
 *     session.fonts()
 *             .registerFamily(FontFamilyDefinition.serifFamily("MyTitle", regular, bold))
 *             .registerNodeDefinition(new MyCustomDefinition());
 *     template.compose(session, spec);
 *     session.buildPdf();
 * }
 * }</pre>
 *
 * <p>Use {@link #session()} to chain back to the owning session if you
 * prefer a single fluent expression.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.6.0
 */
public final class SessionFontApi {

    private final DocumentSession session;

    /**
     * Package-private constructor — instances are created exclusively via
     * {@link DocumentSession#fonts()}.
     */
    SessionFontApi(DocumentSession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    /**
     * Registers a document-local font family for text measurement and
     * PDF rendering. Equivalent to
     * {@link DocumentSession#registerFontFamily(FontFamilyDefinition)}.
     *
     * @param definition custom font family definition
     * @return this facade for chaining
     * @throws IllegalStateException if the owning session has already been closed
     */
    public SessionFontApi registerFamily(FontFamilyDefinition definition) {
        session.registerFontFamily(definition);
        return this;
    }

    /**
     * Registers a custom semantic node definition. Equivalent to
     * {@link DocumentSession#registerNodeDefinition(NodeDefinition)}.
     *
     * @param definition node definition implementation
     * @param <E>        semantic node type handled by the definition
     * @return this facade for chaining
     * @throws IllegalStateException if the owning session has already been closed
     */
    public <E extends DocumentNode> SessionFontApi registerNodeDefinition(NodeDefinition<E> definition) {
        session.registerNodeDefinition(definition);
        return this;
    }

    /**
     * Returns the owning {@link DocumentSession}, allowing the call
     * chain to continue with authoring or rendering calls in a single
     * fluent expression.
     *
     * @return owning session
     */
    public DocumentSession session() {
        return session;
    }
}
