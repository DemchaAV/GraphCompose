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
