package com.demcha.compose.document.node;

/**
 * Marker rendered before each item in a canonical list node.
 *
 * <p>The marker keeps the public list API small while still allowing custom
 * list prefixes for CVs, reports, and template-specific layouts.</p>
 *
 * @param value visible marker prefix, or an empty string for markerless lists
 * @author Artem Demchyshyn
 */
public record ListMarker(String value) {
    /**
     * Creates a normalized marker.
     *
     * @param value visible marker value, or {@code null} for no marker
     */
    public ListMarker {
        value = normalize(value);
    }

    /**
     * Returns the standard bullet marker.
     *
     * @return bullet marker
     */
    public static ListMarker bullet() {
        return new ListMarker("\u2022");
    }

    /**
     * Returns a dash marker.
     *
     * @return dash marker
     */
    public static ListMarker dash() {
        return new ListMarker("-");
    }

    /**
     * Returns a markerless list marker.
     *
     * @return markerless marker
     */
    public static ListMarker none() {
        return new ListMarker("");
    }

    /**
     * Returns a caller-defined marker.
     *
     * @param marker marker text rendered before each item
     * @return custom marker
     */
    public static ListMarker custom(String marker) {
        return new ListMarker(marker);
    }

    /**
     * Returns the built-in marker for a nested-list depth, used when neither
     * the item itself nor {@code ListBuilder.markerFor(depth, ...)} supplied
     * one: {@code •} at depth 0, {@code ◦} at depth 1, {@code ▪} at depth 2,
     * and {@code ·} below that. Fixed-layout rendering and the semantic DOCX
     * export both resolve their fallback through this single cascade so the
     * two outputs of one session always agree.
     *
     * @param depth zero-based nesting depth
     * @return default marker for the depth
     * @since 1.8.0
     */
    public static ListMarker defaultForDepth(int depth) {
        return switch (depth) {
            case 0 -> bullet();                       // •
            case 1 -> new ListMarker("◦");  // ◦
            case 2 -> new ListMarker("▪");  // ▪
            default -> new ListMarker("·"); // ·
        };
    }

    /**
     * Normalizes an author-supplied flat list item before a marker prefix is
     * applied: trims the text and strips one leading author-typed marker
     * ({@code •}, {@code "- "}, {@code "+ "}, or {@code "* "} — but not a
     * {@code **bold} run) so the typed marker does not double up with the
     * rendered one. When {@code normalizeMarkers} is {@code false} the value
     * is returned unchanged apart from null-safety. A blank result means the
     * item carries no renderable content and should be skipped, matching
     * fixed-layout rendering.
     *
     * @param value            raw author-supplied item text; {@code null} is
     *                         treated as empty
     * @param normalizeMarkers whether author-typed markers are stripped
     * @return normalized item text, possibly blank
     * @since 1.8.0
     */
    public static String normalizeItemText(String value, boolean normalizeMarkers) {
        String safe = value == null ? "" : value;
        if (!normalizeMarkers) {
            // Preserve raw whitespace and any author-supplied marker
            // characters. Used by the nested-list flatten path so the
            // depth-based indent prefix survives layout.
            return safe;
        }
        String normalized = safe.trim();
        if (normalized.isEmpty()) {
            return normalized;
        }
        if (normalized.startsWith("•")) {
            return normalized.substring(1).trim();
        }
        if (normalized.startsWith("- ")) {
            return normalized.substring(2).trim();
        }
        if (normalized.startsWith("+ ")) {
            return normalized.substring(2).trim();
        }
        if (normalized.startsWith("* ") && !normalized.startsWith("**")) {
            return normalized.substring(2).trim();
        }
        return normalized;
    }

    /**
     * Returns {@code true} when this marker has non-whitespace content.
     *
     * @return whether the marker is visible
     */
    public boolean isVisible() {
        return value.chars().anyMatch(ch -> !Character.isWhitespace(ch));
    }

    /**
     * Returns the ready-to-render paragraph prefix.
     *
     * @return marker prefix including a trailing space when visible
     */
    public String prefix() {
        return value;
    }

    private static String normalize(String raw) {
        String safe = raw == null ? "" : raw.strip();
        if (safe.isEmpty()) {
            return "";
        }
        char last = safe.charAt(safe.length() - 1);
        return Character.isWhitespace(last) ? safe : safe + " ";
    }
}
