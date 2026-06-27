package com.demcha.compose.document.templates.core.identity;

import java.util.Objects;

/**
 * Optional labelled hyperlink in the CV header — LinkedIn, GitHub,
 * portfolio, personal site, etc.
 *
 * @param label visible link text (required, non-blank)
 * @param url   click target (required, non-blank)
 */
public record Link(String label, String url) {

    /**
     * Validates that both fields are non-null and non-blank.
     */
    public Link {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(url, "url");
        if (label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        if (url.isBlank()) {
            throw new IllegalArgumentException("url must not be blank");
        }
    }

    /**
     * Convenience factory mirroring {@code Link.of("LinkedIn",
     * "https://...")} call sites.
     *
     * @param label visible link text (required, non-blank)
     * @param url   click target (required, non-blank)
     * @return a {@code Link} with the given label and target
     */
    public static Link of(String label, String url) {
        return new Link(label, url);
    }
}
