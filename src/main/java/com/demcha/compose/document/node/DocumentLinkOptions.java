package com.demcha.compose.document.node;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * Backend-neutral hyperlink metadata attached to semantic document content.
 *
 * @param uri absolute URI opened by renderers that support links
 * @author Artem Demchyshyn
 */
public record DocumentLinkOptions(String uri) {
    /**
     * Validates and normalizes the link target.
     *
     * @param uri absolute link target
     */
    public DocumentLinkOptions {
        uri = Objects.requireNonNullElse(uri, "").trim();
        if (uri.isEmpty()) {
            throw new IllegalArgumentException("Link URI must not be blank.");
        }
        validate(uri);
    }

    private static void validate(String uri) {
        try {
            URI parsed = new URI(uri);
            if (parsed.getScheme() == null) {
                throw new IllegalArgumentException("Link URI must include a scheme: " + uri);
            }
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid link URI: " + uri, ex);
        }
    }
}
