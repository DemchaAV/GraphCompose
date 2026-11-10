package com.demcha.compose.document.backend.semantic.docx;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * How wide Word draws a border, and which of the widths the page strokes it does not draw.
 *
 * <p>A Word border's width is its {@code w:sz}, in eighths of a point, and a line border's runs
 * from two to ninety-six (ECMA-376): no border Word draws is thinner than a quarter of a point
 * or wider than twelve. The page strokes a border at the width it is given, so a panel's 20pt
 * border is drawn 12pt wide and a 0.1pt hairline a quarter of a point, and the report names
 * each; a width that rounds to an eighth Word draws is drawn at that eighth, unnamed.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxBorderWidths {

    /** Word's thinnest border, in eighths of a point. */
    static final long THINNEST = 2;

    /** Word's widest, in eighths of a point. */
    static final long WIDEST = 96;

    private DocxBorderWidths() {
    }

    /**
     * A border's width as {@code w:sz}: eighths of a point, held from Word's thinnest to its
     * widest.
     *
     * @param points the width the page strokes it at
     * @return the eighths written
     */
    static long eighths(double points) {
        return Math.max(THINNEST, Math.min(WIDEST, Math.round(points * 8)));
    }

    /**
     * What a note says of borders Word draws at another width than the page strokes them, or
     * {@code null} where it draws each in the eighth of a point nearest the page's.
     *
     * @param what   what the borders are, as "its borders" or "the rule"
     * @param plural whether {@code what} names more than one
     * @param widths the widths the page strokes them at, in points, each above zero
     * @return the note's text, or {@code null}
     */
    static String notHeld(String what, boolean plural, Collection<Double> widths) {
        TreeSet<Double> thinner = new TreeSet<>();
        TreeSet<Double> wider = new TreeSet<>();
        for (double width : widths) {
            long eighths = Math.round(width * 8);
            if (eighths < THINNEST) {
                thinner.add(width);
            } else if (eighths > WIDEST) {
                wider.add(width);
            }
        }
        if (thinner.isEmpty() && wider.isEmpty()) {
            return null;
        }
        String them = plural ? "them" : "it";
        List<String> drawn = new ArrayList<>(2);
        if (!thinner.isEmpty()) {
            drawn.add(pointsOf(THINNEST / 8.0) + "pt wide where the page strokes " + them + " " + listed(thinner));
        }
        if (!wider.isEmpty()) {
            drawn.add(pointsOf(WIDEST / 8.0) + "pt wide where the page strokes " + them + " " + listed(wider));
        }
        return what + (plural ? " are" : " is") + " drawn " + String.join(", and ", drawn)
               + ": a Word border is no thinner than " + pointsOf(THINNEST / 8.0) + "pt and no wider than "
               + pointsOf(WIDEST / 8.0) + "pt";
    }

    /**
     * What a note says of a border the page strokes at no width, which it draws as the thinnest
     * line a screen or a printer shows (a line width of 0, in PDF).
     *
     * @param what what the border is, as "a page header's separator"
     * @return the note's text
     */
    static String ofNoWidth(String what) {
        return what + ", which the page strokes 0pt wide, the thinnest line a screen or a printer shows, is "
               + "drawn " + pointsOf(THINNEST / 8.0) + "pt wide: a Word border is no thinner";
    }

    /** Widths as a note lists them: "0.1pt", "0.05pt and 0.1pt". */
    private static String listed(TreeSet<Double> widths) {
        List<String> each = widths.stream().map(width -> pointsOf(width) + "pt").collect(Collectors.toList());
        if (each.size() == 1) {
            return each.get(0);
        }
        return String.join(", ", each.subList(0, each.size() - 1)) + " and " + each.get(each.size() - 1);
    }

    /** A width to the thousandth of a point, with no trailing zeros: a hairline is no tenth of one. */
    private static String pointsOf(double points) {
        return BigDecimal.valueOf(Math.round(points * 1000) / 1000.0).stripTrailingZeros().toPlainString();
    }
}
