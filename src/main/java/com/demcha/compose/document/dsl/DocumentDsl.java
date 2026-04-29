package com.demcha.compose.document.dsl;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.internal.BuilderSupport;
import com.demcha.compose.document.node.ContainerNode;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Fluent semantic authoring facade for {@link DocumentSession}.
 *
 * <p>The DSL keeps the V2 authoring model node-based internally while exposing
 * a compact builder experience for the common document-building path.</p>
 *
 * <pre>{@code
 * try (var document = GraphCompose.document(Path.of("output.pdf")).create()) {
 *     document.pageFlow(page -> page
 *             .module("Summary", module -> module.paragraph("Hello GraphCompose"))
 *             .module("Skills", module -> module.bullets("Java", "SQL")));
 *
 *     document.buildPdf();
 * }
 * }</pre>
 *
 * @author Artem Demchyshyn
 */
public final class DocumentDsl {
    private final DocumentSession session;

    /**
     * Creates a DSL facade bound to one live document session.
     *
     * @param session mutable session that receives built root nodes
     */
    public DocumentDsl(DocumentSession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    /**
     * Starts a root page-flow builder that attaches itself to the session when
     * {@link PageFlowBuilder#build()} is called.
     *
     * @return a root flow builder
     */
    public PageFlowBuilder pageFlow() {
        return new PageFlowBuilder(session);
    }

    /**
     * Configures, builds, and attaches one root page flow in a single call.
     *
     * @param spec callback that configures the root flow
     * @return the built root container node
     */
    public ContainerNode pageFlow(Consumer<PageFlowBuilder> spec) {
        return BuilderSupport.configure(pageFlow(), spec).build();
    }

    /**
     * Starts a semantic section builder.
     *
     * @return a detached section builder
     */
    public SectionBuilder section() {
        return new SectionBuilder();
    }

    /**
     * Starts a semantic module builder.
     *
     * @return a detached module builder
     */
    public ModuleBuilder module() {
        return new ModuleBuilder();
    }

    /**
     * Starts a paragraph builder.
     *
     * @return a detached paragraph builder
     */
    public ParagraphBuilder paragraph() {
        return new ParagraphBuilder();
    }

    /**
     * Alias for {@link #paragraph()} for callers thinking in short text blocks.
     *
     * @return a detached paragraph builder
     */
    public ParagraphBuilder text() {
        return paragraph();
    }

    /**
     * Starts a semantic list builder.
     *
     * @return a detached list builder
     */
    public ListBuilder list() {
        return new ListBuilder();
    }

    /**
     * Starts an image builder.
     *
     * @return a detached image builder
     */
    public ImageBuilder image() {
        return new ImageBuilder();
    }

    /**
     * Starts a generic rectangle-like shape builder.
     *
     * @return a detached shape builder
     */
    public ShapeBuilder shape() {
        return new ShapeBuilder();
    }

    /**
     * Starts a semantic barcode or QR-code builder.
     *
     * @return a detached barcode builder
     */
    public BarcodeBuilder barcode() {
        return new BarcodeBuilder();
    }

    /**
     * Starts a divider builder preconfigured as a one-point horizontal rule.
     *
     * @return a detached divider builder
     */
    public DividerBuilder divider() {
        return new DividerBuilder();
    }

    /**
     * Starts a semantic table builder.
     *
     * @return a detached table builder
     */
    public TableBuilder table() {
        return new TableBuilder();
    }

    /**
     * Starts a page-break control builder.
     *
     * @return a detached page-break builder
     */
    public PageBreakBuilder pageBreak() {
        return new PageBreakBuilder();
    }

    /**
     * Builds a {@link RichText} run sequence in one call. Mirrors
     * {@code RichText.empty()} chained with the supplied callback so the entry
     * point lives next to the rest of the DSL builders.
     *
     * @param spec callback that appends runs to a fresh rich-text builder
     * @return the configured rich-text run sequence
     */
    public RichText richText(Consumer<RichText> spec) {
        Objects.requireNonNull(spec, "spec");
        RichText richText = RichText.empty();
        spec.accept(richText);
        return richText;
    }
}