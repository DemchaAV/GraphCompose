package com.demcha.compose.document.templates.cv.v2.data;

import java.util.Objects;

/**
 * Generic two-field row used inside a {@link RowsSection}.
 *
 * <p>The {@code label} is the bold "key" rendered at the start of
 * the row (e.g. {@code "Languages"}, {@code "GraphCompose (Java 21,
 * PDFBox)"}). The {@code body} is the regular-weight value, may
 * contain inline markdown.</p>
 *
 * <p>How the pair is presented visually (inline {@code "Label:
 * body"}, two-line "bold label / body below", bullet glyph or not)
 * is decided by the parent {@link RowsSection}'s {@link RowStyle}
 * setting — not by the row itself.</p>
 *
 * @param label bold key (required, non-blank)
 * @param body  free-form text; may contain inline markdown
 *              ({@code **bold**}, {@code *italic*})
 */
public record CvRow(String label, String body) {

    public CvRow {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(body, "body");
        if (label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
    }
}
