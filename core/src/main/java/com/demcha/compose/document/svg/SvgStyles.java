package com.demcha.compose.document.svg;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentLineJoin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Presentation-attribute parsing for the icon reader: the SVG colour
 * grammar ({@code #rgb[a]} / {@code #rrggbb[aa]} hex, {@code rgb()} /
 * {@code rgba()} with numbers or percentages, the CSS named-colour table,
 * {@code none}, {@code currentColor}), absolute CSS lengths
 * ({@code px} = user units, {@code pt}, {@code in}, {@code mm}, {@code cm},
 * {@code pc}), and the stroke style trio {@code stroke-linecap} /
 * {@code stroke-linejoin} / {@code stroke-dasharray}.
 *
 * <p>Everything unsupported fails loudly with the supported alternatives
 * listed — relative units ({@code em}, {@code %}, viewport units) have no
 * deterministic meaning inside an icon frame.</p>
 */
final class SvgStyles {

    private SvgStyles() {
    }

    // ------------------------------------------------------------------
    // Colours
    // ------------------------------------------------------------------

    /**
     * Parses an SVG paint colour.
     *
     * @param value   raw attribute value
     * @param current colour {@code currentColor} / {@code inherit} resolve to
     * @return the colour, or {@code null} for {@code none}
     * @throws IllegalArgumentException for paints outside the icon subset
     */
    static DocumentColor color(String value, DocumentColor current) {
        String v = value.trim().toLowerCase(Locale.ROOT);
        if (v.equals("none")) {
            return null;
        }
        if (v.equals("currentcolor") || v.equals("inherit")) {
            return current;
        }
        if (v.startsWith("#")) {
            DocumentColor hex = hexColor(v.substring(1));
            if (hex != null) {
                return hex;
            }
        }
        if ((v.startsWith("rgb(") || v.startsWith("rgba(")) && v.endsWith(")")) {
            return rgbColor(v);
        }
        DocumentColor named = SvgColors.named(v);
        if (named != null) {
            return named;
        }
        throw new IllegalArgumentException(
                "unsupported SVG colour '" + value
                + "' — use #hex (3/4/6/8 digits), rgb()/rgba(), a CSS colour name, none, or currentColor");
    }

    private static DocumentColor hexColor(String hex) {
        String expanded = switch (hex.length()) {
            case 3, 4 -> {
                StringBuilder sb = new StringBuilder(hex.length() * 2);
                for (int i = 0; i < hex.length(); i++) {
                    sb.append(hex.charAt(i)).append(hex.charAt(i));
                }
                yield sb.toString();
            }
            case 6, 8 -> hex;
            default -> null;
        };
        if (expanded == null) {
            return null;
        }
        DocumentColor color = DocumentColor.rgb(
                Integer.parseInt(expanded.substring(0, 2), 16),
                Integer.parseInt(expanded.substring(2, 4), 16),
                Integer.parseInt(expanded.substring(4, 6), 16));
        if (expanded.length() == 8) {
            color = color.withOpacity(Integer.parseInt(expanded.substring(6, 8), 16) / 255.0);
        }
        return color;
    }

    private static DocumentColor rgbColor(String v) {
        String inner = v.substring(v.indexOf('(') + 1, v.length() - 1);
        String[] parts = inner.split("[,\\s/]+");
        if (parts.length != 3 && parts.length != 4) {
            throw new IllegalArgumentException(
                    "rgb()/rgba() needs three colour channels (plus optional alpha): '" + v + "'");
        }
        DocumentColor color = DocumentColor.rgb(
                channel(parts[0]), channel(parts[1]), channel(parts[2]));
        if (parts.length == 4) {
            color = color.withOpacity(alpha(parts[3]));
        }
        return color;
    }

    /** One rgb() channel: 0–255 number or percentage. */
    private static int channel(String value) {
        String t = value.trim();
        if (t.endsWith("%")) {
            double pct = Math.max(0, Math.min(100, Double.parseDouble(t.substring(0, t.length() - 1))));
            // pct/100*255 keeps 50% exact (127.5 → 128); pct*2.55 drifts to 127.
            return (int) Math.round(pct / 100.0 * 255.0);
        }
        return Math.max(0, Math.min(255, (int) Math.round(Double.parseDouble(t))));
    }

    /** rgba() alpha: 0–1 number or percentage, clamped. */
    private static double alpha(String value) {
        String t = value.trim();
        double a = t.endsWith("%")
                ? Double.parseDouble(t.substring(0, t.length() - 1)) / 100.0
                : Double.parseDouble(t);
        return Math.max(0.0, Math.min(1.0, a));
    }

    // ------------------------------------------------------------------
    // Lengths
    // ------------------------------------------------------------------

    /**
     * Parses an absolute CSS length into SVG user units (1 user unit = 1px).
     *
     * @param value   raw attribute value, e.g. {@code "7"}, {@code "2.5px"},
     *                {@code "1pt"}
     * @param context attribute name for the error message
     * @return length in user units
     * @throws IllegalArgumentException for relative units
     */
    static double length(String value, String context) {
        String v = value.trim().toLowerCase(Locale.ROOT);
        int unitStart = v.length();
        while (unitStart > 0 && Character.isLetter(v.charAt(unitStart - 1))) {
            unitStart--;
        }
        String unit = v.substring(unitStart);
        double number = Double.parseDouble(v.substring(0, unitStart).trim());
        return switch (unit) {
            case "", "px" -> number;
            case "pt" -> number * (96.0 / 72.0);
            case "pc" -> number * 16.0;
            case "in" -> number * 96.0;
            case "mm" -> number * (96.0 / 25.4);
            case "cm" -> number * (96.0 / 2.54);
            default -> throw new IllegalArgumentException(
                    "unsupported unit '" + unit + "' on " + context + "='" + value
                    + "' — use user units, px, pt, pc, in, mm or cm");
        };
    }

    // ------------------------------------------------------------------
    // Stroke style
    // ------------------------------------------------------------------

    /**
     * Parses {@code stroke-linecap}.
     *
     * @param value raw attribute value
     * @return the cap style
     * @throws IllegalArgumentException for values outside the SVG enum
     */
    static DocumentLineCap lineCap(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "butt" -> DocumentLineCap.BUTT;
            case "round" -> DocumentLineCap.ROUND;
            case "square" -> DocumentLineCap.SQUARE;
            case "inherit" -> null;
            default -> throw new IllegalArgumentException(
                    "unsupported stroke-linecap '" + value + "' — use butt, round, or square");
        };
    }

    /**
     * Parses {@code stroke-linejoin}. The SVG2 {@code miter-clip} and
     * {@code arcs} values fall back to plain mitres — they only differ
     * beyond the mitre limit.
     *
     * @param value raw attribute value
     * @return the join style
     * @throws IllegalArgumentException for values outside the SVG enum
     */
    static DocumentLineJoin lineJoin(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "miter", "miter-clip", "arcs" -> DocumentLineJoin.MITER;
            case "round" -> DocumentLineJoin.ROUND;
            case "bevel" -> DocumentLineJoin.BEVEL;
            case "inherit" -> null;
            default -> throw new IllegalArgumentException(
                    "unsupported stroke-linejoin '" + value + "' — use miter, round, or bevel");
        };
    }

    /**
     * Parses {@code stroke-dasharray} into user-unit lengths. Per SVG, an
     * odd-length list repeats doubled, {@code none} (and an all-zero list)
     * mean solid, negative values are an error.
     *
     * @param value raw attribute value
     * @return dash lengths in user units; empty list for solid
     * @throws IllegalArgumentException for negative entries or bad units
     */
    static List<Double> dashArray(String value) {
        String v = value.trim();
        if (v.isEmpty() || v.equalsIgnoreCase("none")) {
            return List.of();
        }
        String[] parts = v.split("[\\s,]+");
        List<Double> lengths = new ArrayList<>(parts.length * 2);
        boolean anyPositive = false;
        for (String part : parts) {
            double length = length(part, "stroke-dasharray");
            if (length < 0) {
                throw new IllegalArgumentException(
                        "stroke-dasharray entries must be non-negative: '" + value + "'");
            }
            anyPositive |= length > 0;
            lengths.add(length);
        }
        if (!anyPositive) {
            return List.of();
        }
        if (lengths.size() % 2 != 0) {
            lengths.addAll(new ArrayList<>(lengths));
        }
        return List.copyOf(lengths);
    }
}
