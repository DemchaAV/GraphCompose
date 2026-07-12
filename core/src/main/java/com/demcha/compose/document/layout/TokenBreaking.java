package com.demcha.compose.document.layout;

import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.layout.NodeDefinitionSupport.EPS;

/**
 * Breaks a single over-wide, otherwise-unbreakable token into line-pieces that
 * each fit a width budget. Prefers wrapping at readable soft seams
 * ({@code . : / -}) so package coordinates, fully-qualified class names and URLs
 * split naturally, and falls back to character-level splitting for a lone
 * segment that is still too wide.
 *
 * <p>Extracted from {@link TextFlowSupport} (which enters only through
 * {@link #breakLongToken}) so the pure breaking logic is unit-testable in
 * isolation and does not grow the wrap-loop file. Package-private intentionally
 * — engine surface, not public API.</p>
 *
 * @author Artem Demchyshyn
 */
final class TokenBreaking {

    /**
     * Characters after which a long, otherwise-unbreakable token may wrap. These
     * are the joiners inside package coordinates, fully-qualified class names and
     * URLs ({@code org.junit.jupiter:junit-jupiter:5.10.2}, {@code a/b/c}), so an
     * over-wide code token breaks at a readable seam instead of mid-identifier.
     */
    private static final String SOFT_BREAK_AFTER_CHARS = ".:/-";

    private TokenBreaking() {
    }

    /**
     * Breaks a single over-wide token into line-pieces that each fit
     * {@code maxWidth}. Prefers breaking at {@link #SOFT_BREAK_AFTER_CHARS soft
     * seams} (after {@code . : / -}) so coordinates and URLs split naturally, and
     * falls back to character-level {@link #splitLongToken splitting} for any lone
     * segment still too wide (e.g. a long dot-less identifier). The returned pieces
     * have the same shape the wrap loops consume — for a token with no soft seams
     * the result is identical to a bare {@link #splitLongToken}.
     */
    static List<String> breakLongToken(String token,
                                       TextStyle style,
                                       double maxWidth,
                                       TextMeasurementSystem measurement) {
        if (token == null || token.isEmpty()) {
            return List.of();
        }
        List<String> segments = softBreakSegments(token);
        List<String> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        double currentWidth = 0.0;
        for (String segment : segments) {
            double segmentWidth = measurement.textWidth(style, segment);
            if (current.length() > 0 && currentWidth + segmentWidth > maxWidth + EPS) {
                pieces.add(current.toString());
                current.setLength(0);
                currentWidth = 0.0;
            }
            if (current.length() == 0 && segmentWidth > maxWidth + EPS) {
                // A single seam-delimited segment is itself too wide: char-split it.
                // Every char-chunk but the last is a finished piece; the tail seeds
                // the current piece so a following short segment can still join it.
                List<String> chars = splitLongToken(segment, style, maxWidth, measurement);
                for (int index = 0; index < chars.size() - 1; index++) {
                    pieces.add(chars.get(index));
                }
                String tail = chars.get(chars.size() - 1);
                current.append(tail);
                currentWidth = measurement.textWidth(style, tail);
            } else {
                current.append(segment);
                currentWidth += segmentWidth;
            }
        }
        if (current.length() > 0) {
            pieces.add(current.toString());
        }
        return List.copyOf(pieces);
    }

    /**
     * Splits a token after each {@link #SOFT_BREAK_AFTER_CHARS soft-break
     * character}, keeping the delimiter with the preceding segment
     * ({@code org.junit.jupiter:} then {@code junit-jupiter:} then {@code 5.10.2}).
     * A token with no such characters yields a single segment (itself).
     */
    static List<String> softBreakSegments(String token) {
        if (token == null || token.isEmpty()) {
            return List.of();
        }
        List<String> segments = new ArrayList<>();
        int start = 0;
        for (int index = 0; index < token.length(); index++) {
            if (SOFT_BREAK_AFTER_CHARS.indexOf(token.charAt(index)) >= 0) {
                segments.add(token.substring(start, index + 1));
                start = index + 1;
            }
        }
        if (start < token.length()) {
            segments.add(token.substring(start));
        }
        return segments;
    }

    /**
     * Character-level fallback: splits a token into the longest prefixes that each
     * fit {@code maxWidth}, guaranteeing at least one character per piece.
     */
    static List<String> splitLongToken(String token,
                                       TextStyle style,
                                       double maxWidth,
                                       TextMeasurementSystem measurement) {
        if (token == null || token.isEmpty()) {
            return List.of();
        }

        List<String> pieces = new ArrayList<>();
        String remaining = token;
        while (!remaining.isEmpty()) {
            int splitIndex = fitCharacters(remaining, style, maxWidth, measurement);
            if (splitIndex <= 0 || splitIndex >= remaining.length()) {
                pieces.add(remaining);
                break;
            }
            pieces.add(remaining.substring(0, splitIndex));
            remaining = remaining.substring(splitIndex).stripLeading();
        }
        return List.copyOf(pieces);
    }

    /**
     * Largest prefix length of {@code text} whose width fits {@code maxWidth},
     * or 1 (the guaranteed minimum) when not even the first character fits.
     */
    static int fitCharacters(String text,
                             TextStyle style,
                             double maxWidth,
                             TextMeasurementSystem measurement) {
        // Largest prefix length whose width fits. The fit predicate
        // width(substring(0,n)) <= maxWidth is monotonic in n (each added char
        // contributes a non-negative glyph advance), so the fitting lengths form
        // a prefix [1..lastFitting] and a binary search finds the SAME boundary
        // as the old linear scan — but in O(log n) width calls instead of
        // measuring every growing prefix (which was O(n) calls and O(n^2)
        // measured characters for a long unbreakable token).
        int lastFitting = 0;
        int low = 1;
        int high = text.length();
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (measurement.textWidth(style, text.substring(0, mid)) <= maxWidth + EPS) {
                lastFitting = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return lastFitting == 0 ? Math.min(1, text.length()) : lastFitting;
    }
}
