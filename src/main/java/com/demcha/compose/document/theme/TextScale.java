package com.demcha.compose.document.theme;

import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.Objects;

/**
 * Semantic text-style scale for business documents.
 *
 * <p>Each token is a fully-resolved {@link DocumentTextStyle} that callers can
 * pass straight into paragraph builders, inline runs, or table cell styles. The
 * scale is intentionally compact — picking from a fixed set keeps invoice,
 * proposal, and report templates visually consistent.</p>
 *
 * @param h1 document/title heading style
 * @param h2 section heading style
 * @param h3 sub-heading style
 * @param body default paragraph style
 * @param caption small/secondary text style
 * @param label form-label or table-header style
 * @param accent inline accent style (for status keywords, totals)
 *
 * @author Artem Demchyshyn
 */
public record TextScale(
        DocumentTextStyle h1,
        DocumentTextStyle h2,
        DocumentTextStyle h3,
        DocumentTextStyle body,
        DocumentTextStyle caption,
        DocumentTextStyle label,
        DocumentTextStyle accent
) {
    /**
     * Validates required style tokens.
     */
    public TextScale {
        Objects.requireNonNull(h1, "h1");
        Objects.requireNonNull(h2, "h2");
        Objects.requireNonNull(h3, "h3");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(caption, "caption");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(accent, "accent");
    }
}
