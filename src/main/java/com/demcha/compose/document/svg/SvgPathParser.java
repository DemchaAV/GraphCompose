package com.demcha.compose.document.svg;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal SVG path-data scanner and state machine: turns a {@code d} string
 * into absolute drawing ops in SVG user space (y down). Quadratics elevate
 * exactly to cubics, smooth shorthands reflect the previous control point,
 * and elliptical arcs convert via the W3C endpoint-to-center algorithm into
 * cubic slices of at most 90°. {@link SvgPath} owns normalization and the
 * public surface; every branch here is driven through {@code SvgPathTest}.
 *
 * <p>Op encoding: {@code [0]}=kind (0 move, 1 line, 2 cubic, 3 close),
 * followed by coordinate pairs.</p>
 */

final class SvgPathParser {

    private static final double EPS = 1e-9;
    private final String d;
    private int pos;

    private double curX;
    private double curY;
    private double startX;
    private double startY;
    private double lastCubicC2X;
    private double lastCubicC2Y;
    private double lastQuadCX;
    private double lastQuadCY;
    private char lastCmd;

    private final List<double[]> ops = new ArrayList<>();

    SvgPathParser(String d) {
        this.d = d == null ? "" : d;
    }

    List<double[]> parse() {
        skipSeparators();
        if (pos >= d.length()) {
            throw new IllegalArgumentException("SVG path data is empty");
        }
        char first = d.charAt(pos);
        if (first != 'M' && first != 'm') {
            throw new IllegalArgumentException(
                    "SVG path data must start with a moveto, found '" + first + "' at position " + pos);
        }
        char cmd = 0;
        while (true) {
            skipSeparators();
            if (pos >= d.length()) {
                break;
            }
            char c = d.charAt(pos);
            if (isCommand(c)) {
                cmd = c;
                pos++;
            } else if (cmd == 0) {
                throw fail("a command letter");
            } else {
                // Implicit repetition: a moveto chain continues as lineto.
                if (cmd == 'M') {
                    cmd = 'L';
                } else if (cmd == 'm') {
                    cmd = 'l';
                }
            }
            apply(cmd);
        }
        return ops;
    }

    private void apply(char cmd) {
        boolean rel = Character.isLowerCase(cmd);
        switch (Character.toUpperCase(cmd)) {
            case 'M' -> {
                double x = number() + (rel ? curX : 0);
                double y = number() + (rel ? curY : 0);
                moveTo(x, y);
            }
            case 'L' -> {
                double x = number() + (rel ? curX : 0);
                double y = number() + (rel ? curY : 0);
                lineTo(x, y);
            }
            case 'H' -> lineTo(number() + (rel ? curX : 0), curY);
            case 'V' -> lineTo(curX, number() + (rel ? curY : 0));
            case 'C' -> {
                double c1x = number() + (rel ? curX : 0);
                double c1y = number() + (rel ? curY : 0);
                double c2x = number() + (rel ? curX : 0);
                double c2y = number() + (rel ? curY : 0);
                double x = number() + (rel ? curX : 0);
                double y = number() + (rel ? curY : 0);
                cubicTo(c1x, c1y, c2x, c2y, x, y);
            }
            case 'S' -> {
                double c1x = curX;
                double c1y = curY;
                if (lastCmd == 'C' || lastCmd == 'S') {
                    c1x = 2 * curX - lastCubicC2X;
                    c1y = 2 * curY - lastCubicC2Y;
                }
                double c2x = number() + (rel ? curX : 0);
                double c2y = number() + (rel ? curY : 0);
                double x = number() + (rel ? curX : 0);
                double y = number() + (rel ? curY : 0);
                cubicTo(c1x, c1y, c2x, c2y, x, y);
                lastCmd = 'S';
                return;
            }
            case 'Q' -> {
                double qx = number() + (rel ? curX : 0);
                double qy = number() + (rel ? curY : 0);
                double x = number() + (rel ? curX : 0);
                double y = number() + (rel ? curY : 0);
                quadTo(qx, qy, x, y);
                lastCmd = 'Q';
                return;
            }
            case 'T' -> {
                double qx = curX;
                double qy = curY;
                if (lastCmd == 'Q' || lastCmd == 'T') {
                    qx = 2 * curX - lastQuadCX;
                    qy = 2 * curY - lastQuadCY;
                }
                double x = number() + (rel ? curX : 0);
                double y = number() + (rel ? curY : 0);
                quadTo(qx, qy, x, y);
                lastCmd = 'T';
                return;
            }
            case 'A' -> {
                double rx = number();
                double ry = number();
                double rotationDegrees = number();
                boolean largeArc = flag();
                boolean sweep = flag();
                double x = number() + (rel ? curX : 0);
                double y = number() + (rel ? curY : 0);
                arcTo(rx, ry, rotationDegrees, largeArc, sweep, x, y);
            }
            case 'Z' -> {
                ops.add(new double[]{3});
                curX = startX;
                curY = startY;
            }
            default -> throw fail("a supported command");
        }
        lastCmd = Character.toUpperCase(cmd);
    }

    private void moveTo(double x, double y) {
        ops.add(new double[]{0, x, y});
        curX = x;
        curY = y;
        startX = x;
        startY = y;
    }

    private void lineTo(double x, double y) {
        ops.add(new double[]{1, x, y});
        curX = x;
        curY = y;
    }

    private void cubicTo(double c1x, double c1y, double c2x, double c2y, double x, double y) {
        ops.add(new double[]{2, c1x, c1y, c2x, c2y, x, y});
        lastCubicC2X = c2x;
        lastCubicC2Y = c2y;
        curX = x;
        curY = y;
    }

    /** Exact quadratic→cubic elevation: c = q0 + 2/3 (q − q0) etc. */
    private void quadTo(double qx, double qy, double x, double y) {
        double c1x = curX + 2.0 / 3.0 * (qx - curX);
        double c1y = curY + 2.0 / 3.0 * (qy - curY);
        double c2x = x + 2.0 / 3.0 * (qx - x);
        double c2y = y + 2.0 / 3.0 * (qy - y);
        cubicTo(c1x, c1y, c2x, c2y, x, y);
        lastQuadCX = qx;
        lastQuadCY = qy;
    }

    /**
     * W3C SVG 1.1 F.6: endpoint parameterization → center, then cubic
     * spans of at most 90° each ({@code t = 4/3 · tan(δ/4)}).
     */
    private void arcTo(double rx, double ry, double rotationDegrees,
                       boolean largeArc, boolean sweep, double x, double y) {
        if (rx == 0 || ry == 0) {
            lineTo(x, y);
            return;
        }
        double x1 = curX;
        double y1 = curY;
        if (Math.abs(x1 - x) < EPS && Math.abs(y1 - y) < EPS) {
            return;
        }
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        double phi = Math.toRadians(rotationDegrees % 360.0);
        double cosPhi = Math.cos(phi);
        double sinPhi = Math.sin(phi);

        double dx2 = (x1 - x) / 2.0;
        double dy2 = (y1 - y) / 2.0;
        double x1p = cosPhi * dx2 + sinPhi * dy2;
        double y1p = -sinPhi * dx2 + cosPhi * dy2;

        double lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry);
        if (lambda > 1) {
            double scale = Math.sqrt(lambda);
            rx *= scale;
            ry *= scale;
        }

        double rx2 = rx * rx;
        double ry2 = ry * ry;
        double num = rx2 * ry2 - rx2 * y1p * y1p - ry2 * x1p * x1p;
        double den = rx2 * y1p * y1p + ry2 * x1p * x1p;
        double co = Math.sqrt(Math.max(0, num / den)) * (largeArc != sweep ? 1 : -1);
        double cxp = co * rx * y1p / ry;
        double cyp = -co * ry * x1p / rx;
        double cx = cosPhi * cxp - sinPhi * cyp + (x1 + x) / 2.0;
        double cy = sinPhi * cxp + cosPhi * cyp + (y1 + y) / 2.0;

        double theta1 = angle(1, 0, (x1p - cxp) / rx, (y1p - cyp) / ry);
        double delta = angle((x1p - cxp) / rx, (y1p - cyp) / ry,
                (-x1p - cxp) / rx, (-y1p - cyp) / ry) % (2 * Math.PI);
        if (!sweep && delta > 0) {
            delta -= 2 * Math.PI;
        } else if (sweep && delta < 0) {
            delta += 2 * Math.PI;
        }

        int slices = (int) Math.ceil(Math.abs(delta) / (Math.PI / 2.0));
        double sliceDelta = delta / slices;
        double t = 4.0 / 3.0 * Math.tan(sliceDelta / 4.0);
        double theta = theta1;
        for (int i = 0; i < slices; i++) {
            double cosT = Math.cos(theta);
            double sinT = Math.sin(theta);
            double thetaNext = theta + sliceDelta;
            double cosN = Math.cos(thetaNext);
            double sinN = Math.sin(thetaNext);

            double sx = cx + rx * cosPhi * cosT - ry * sinPhi * sinT;
            double sy = cy + rx * sinPhi * cosT + ry * cosPhi * sinT;
            double ex = cx + rx * cosPhi * cosN - ry * sinPhi * sinN;
            double ey = cy + rx * sinPhi * cosN + ry * cosPhi * sinN;

            double dSx = -rx * cosPhi * sinT - ry * sinPhi * cosT;
            double dSy = -rx * sinPhi * sinT + ry * cosPhi * cosT;
            double dEx = -rx * cosPhi * sinN - ry * sinPhi * cosN;
            double dEy = -rx * sinPhi * sinN + ry * cosPhi * cosN;

            cubicTo(sx + t * dSx, sy + t * dSy, ex - t * dEx, ey - t * dEy, ex, ey);
            theta = thetaNext;
        }
        curX = x;
        curY = y;
    }

    private static double angle(double ux, double uy, double vx, double vy) {
        return Math.atan2(ux * vy - uy * vx, ux * vx + uy * vy);
    }

    // ---- scanning -------------------------------------------------

    private static boolean isCommand(char c) {
        return "MmLlHhVvCcSsQqTtAaZz".indexOf(c) >= 0;
    }

    private void skipSeparators() {
        while (pos < d.length()) {
            char c = d.charAt(pos);
            if (c == ',' || Character.isWhitespace(c)) {
                pos++;
            } else {
                break;
            }
        }
    }

    private double number() {
        skipSeparators();
        int start = pos;
        if (pos < d.length() && (d.charAt(pos) == '+' || d.charAt(pos) == '-')) {
            pos++;
        }
        int digits = 0;
        while (pos < d.length() && Character.isDigit(d.charAt(pos))) {
            pos++;
            digits++;
        }
        if (pos < d.length() && d.charAt(pos) == '.') {
            pos++;
            while (pos < d.length() && Character.isDigit(d.charAt(pos))) {
                pos++;
                digits++;
            }
        }
        if (digits == 0) {
            throw fail("a number");
        }
        if (pos < d.length() && (d.charAt(pos) == 'e' || d.charAt(pos) == 'E')) {
            int expStart = pos;
            pos++;
            if (pos < d.length() && (d.charAt(pos) == '+' || d.charAt(pos) == '-')) {
                pos++;
            }
            int expDigits = 0;
            while (pos < d.length() && Character.isDigit(d.charAt(pos))) {
                pos++;
                expDigits++;
            }
            if (expDigits == 0) {
                pos = expStart;
            }
        }
        double value = Double.parseDouble(d.substring(start, pos));
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw fail("a finite number");
        }
        return value;
    }

    /** Arc flags are single characters, so {@code "011"} is three flags-and-digits, not one number. */
    private boolean flag() {
        skipSeparators();
        if (pos >= d.length() || (d.charAt(pos) != '0' && d.charAt(pos) != '1')) {
            throw fail("an arc flag (0 or 1)");
        }
        return d.charAt(pos++) == '1';
    }

    private IllegalArgumentException fail(String expected) {
        String found = pos < d.length() ? "'" + d.charAt(pos) + "'" : "end of data";
        return new IllegalArgumentException(
                "Expected " + expected + " at position " + pos + " in SVG path data, found " + found);
    }
}
