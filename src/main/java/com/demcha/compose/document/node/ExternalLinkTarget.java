package com.demcha.compose.document.node;

import java.util.Objects;

/**
 * A {@link DocumentLinkTarget} that opens an absolute external URI.
 *
 * @param options external link metadata; must not be {@code null}
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public record ExternalLinkTarget(DocumentLinkOptions options) implements DocumentLinkTarget {
    /**
     * Validates the wrapped link options.
     *
     * @param options external link metadata
     */
    public ExternalLinkTarget {
        Objects.requireNonNull(options, "options");
    }
}
