package com.demcha.compose.document.templates.cv.v2.data;

import java.util.Objects;

/**
 * Optional labelled hyperlink in the CV header — LinkedIn, GitHub,
 * portfolio, personal site, etc.
 *
 * @param label visible link text (required, non-blank)
 * @param url   click target (required, non-blank)
 */
public record CvLink(String label, String url) {

    /**
     * Validates that both fields are non-null and non-blank.
     */
    public CvLink {
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
     * Convenience factory mirroring {@code CvLink.of("LinkedIn",
     * "https://...")} call sites.
     *
     * @param label visible link text (required, non-blank)
     * @param url   click target (required, non-blank)
     * @return a {@code CvLink} with the given label and target
     */
    public static CvLink of(String label, String url) {
        return new CvLink(label, url);
    }
}
