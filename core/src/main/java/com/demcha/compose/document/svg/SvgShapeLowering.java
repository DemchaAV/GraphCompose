package com.demcha.compose.document.svg;

/**
 * Lowers SVG basic shapes ({@code rect}, {@code circle}, {@code ellipse},
 * {@code polyline}, {@code polygon}) to synthesized path-data strings, fed
 * back through the one tested path parser ({@link SvgPath}) so every shape
 * shares the same curve machinery. Pure string synthesis — no DOM, no state.
 */
final class SvgShapeLowering {

    private SvgShapeLowering() {
    }

    /**
     * Lowers a {@code <rect>} (optionally rounded) to path data.
     *
     * @param x  left
     * @param y  top
     * @param w  width
     * @param h  height
     * @param rx corner x-radius ({@code <= 0} for square / mirror of ry)
     * @param ry corner y-radius ({@code <= 0} for square / mirror of rx)
     * @return path data, or a plain rectangle when both radii are non-positive
     */
    static String rect(double x, double y, double w, double h, double rx, double ry) {
        if (rx <= 0 && ry <= 0) {
            return "M" + x + " " + y + " h" + w + " v" + h + " h" + (-w) + " Z";
        }
        if (rx <= 0) {
            rx = ry;
        }
        if (ry <= 0) {
            ry = rx;
        }
        rx = Math.min(rx, w / 2);
        ry = Math.min(ry, h / 2);
        return "M" + (x + rx) + " " + y
               + " h" + (w - 2 * rx)
               + " a" + rx + " " + ry + " 0 0 1 " + rx + " " + ry
               + " v" + (h - 2 * ry)
               + " a" + rx + " " + ry + " 0 0 1 " + (-rx) + " " + ry
               + " h" + (2 * rx - w)
               + " a" + rx + " " + ry + " 0 0 1 " + (-rx) + " " + (-ry)
               + " v" + (2 * ry - h)
               + " a" + rx + " " + ry + " 0 0 1 " + rx + " " + (-ry)
               + " Z";
    }

    /**
     * Lowers a {@code <circle>} / {@code <ellipse>} to two-arc path data.
     *
     * @param cx centre x
     * @param cy centre y
     * @param rx x-radius
     * @param ry y-radius
     * @return path data, or {@code null} for a non-positive radius (nothing drawn)
     */
    static String ellipse(double cx, double cy, double rx, double ry) {
        if (rx <= 0 || ry <= 0) {
            return null;
        }
        return "M" + (cx - rx) + " " + cy
               + " a" + rx + " " + ry + " 0 1 0 " + (2 * rx) + " 0"
               + " a" + rx + " " + ry + " 0 1 0 " + (-2 * rx) + " 0"
               + " Z";
    }

    /**
     * Lowers {@code <polyline>} / {@code <polygon>} points to path data.
     *
     * @param points the raw {@code points} attribute
     * @param close  {@code true} to close the ring (polygon)
     * @return path data, or {@code null} for empty points
     */
    static String points(String points, boolean close) {
        String trimmed = points == null ? "" : points.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return "M" + trimmed + (close ? " Z" : "");
    }
}
