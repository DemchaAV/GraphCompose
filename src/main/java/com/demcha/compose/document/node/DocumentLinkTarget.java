package com.demcha.compose.document.node;

/**
 * Backend-neutral target of a clickable link attached to semantic document
 * content.
 *
 * <p>A link is either {@linkplain ExternalLinkTarget external} — it opens an
 * absolute URI in the viewer — or {@linkplain InternalLinkTarget internal} — it
 * jumps to a named {@code anchor(...)} elsewhere in the same document. Renderers
 * that support in-document navigation (the PDF backend) translate an internal
 * target into a go-to action; backends that cannot navigate render the run as
 * ordinary styled text.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public sealed interface DocumentLinkTarget permits ExternalLinkTarget, InternalLinkTarget {

    /**
     * Wraps external URI link options as a link target.
     *
     * @param options external link metadata
     * @return an external link target
     */
    static DocumentLinkTarget external(DocumentLinkOptions options) {
        return new ExternalLinkTarget(options);
    }

    /**
     * Wraps a raw absolute URI string as an external link target.
     *
     * @param uri absolute link target URI (must include a scheme)
     * @return an external link target
     */
    static DocumentLinkTarget external(String uri) {
        return new ExternalLinkTarget(new DocumentLinkOptions(uri));
    }

    /**
     * Targets a named in-document anchor declared with {@code anchor(name)} on a
     * builder.
     *
     * @param anchor non-blank anchor name to jump to
     * @return an internal link target
     */
    static DocumentLinkTarget anchor(String anchor) {
        return new InternalLinkTarget(anchor);
    }
}
