package com.demcha.compose.document.backend.fixed.pdf.options;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * Canonical hyperlink metadata attached to semantic fragments.
 *
 * @param uri absolute URI that should be opened when the PDF annotation is activated
 */
public record PdfLinkOptions(String uri) {
    /**
     * Canonical compact constructor that validates the target URI.
     *
     * @param uri absolute URI that should be opened when the PDF annotation is activated
     */
    public PdfLinkOptions {
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
