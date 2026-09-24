package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.ParagraphDirection;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.node.LineNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.PathNode;
import com.demcha.compose.document.node.PolygonNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextDirection;

import java.util.ArrayList;
import java.util.List;

/**
 * Two single-line paragraphs an overlay sets side by side on one line, as one Word line.
 *
 * <p>A CV entry's head band puts its title at the left and its dates at the right of the same
 * line, as two layers of one container. Word has no layers, so the export wrote the layers
 * one after the other, and the dates came out on a line of their own under the title: an
 * extra line in every entry, enough to push a one-page CV onto a second page. A line with
 * text at the left and more at the right is what Word builds with a right-aligned tab stop,
 * so that is what the pair is written as: the left paragraph's runs, a tab, the right one's.</p>
 *
 * <p>An overlay is taken as such a pair when it holds exactly two paragraphs, each laid out
 * on one line, level with one another on the page and apart across it, and nothing else but
 * drawing the export does not write.</p>
 */
final class DocxLinePair {

    /** How far apart two edges may be and still be the same edge, in points. */
    private static final double EDGE = 0.5;

    private DocxLinePair() {
    }

    /**
     * An overlay's pair.
     *
     * @param left       the paragraph at the left
     * @param right      the paragraph at the right
     * @param leftOffset where the left paragraph starts, from the overlay's left edge
     * @param tabStop    where the right paragraph ends, from the overlay's left edge
     * @param line       the height the two lines take together, from the top of the higher
     *                   to the bottom of the lower
     * @param above      from the overlay's top down to that top — negative where the text
     *                   stands above the overlay
     * @param below      from that bottom down to the overlay's bottom — negative where the
     *                   text hangs below it
     */
    record Pair(ParagraphNode left, ParagraphNode right, double leftOffset, double tabStop,
                double line, double above, double below) {
    }

    /**
     * The pair an overlay sets on one line, or {@code null} when it does not set one.
     *
     * @param overlay a layer stack or a shape container
     * @param layout  where the layout placed the overlay and its layers
     * @return the pair, or {@code null}
     */
    static Pair of(DocumentNode overlay, DocxLayoutMetrics layout) {
        PlacedNode box = layout.placement(overlay);
        if (box == null) {
            return null;
        }
        List<ParagraphNode> text = new ArrayList<>(2);
        for (DocumentNode child : overlay.children()) {
            if (child instanceof ParagraphNode paragraph) {
                text.add(paragraph);
            } else if (!isDrawing(child)) {
                return null;
            }
        }
        // A right-to-left line runs the other way from its tab stop; such a pair stays as it was.
        if (text.size() != 2
            || ParagraphDirection.resolve(text.get(0)) == TextDirection.RTL
            || ParagraphDirection.resolve(text.get(1)) == TextDirection.RTL) {
            return null;
        }
        PlacedNode first = layout.placement(text.get(0));
        PlacedNode second = layout.placement(text.get(1));
        if (first == null || second == null
            || layout.lineCount(text.get(0)) != 1 || layout.lineCount(text.get(1)) != 1
            || first.startPage() != second.startPage()) {
            return null;
        }
        double overlap = Math.min(first.placementY() + first.placementHeight(),
                second.placementY() + second.placementHeight())
                - Math.max(first.placementY(), second.placementY());
        if (overlap <= EDGE) {
            return null;
        }
        double[] a = inked(text.get(0), first, layout);
        double[] b = inked(text.get(1), second, layout);
        if (a == null || b == null) {
            return null;
        }
        boolean firstIsLeft = a[0] <= b[0];
        double[] left = firstIsLeft ? a : b;
        double[] right = firstIsLeft ? b : a;
        if (left[1] > right[0] + EDGE) {
            return null;
        }
        // Placements measure up from the page's foot.
        double boxTop = box.placementY() + box.placementHeight();
        double textTop = Math.max(first.placementY() + first.placementHeight(),
                second.placementY() + second.placementHeight());
        double textBottom = Math.min(first.placementY(), second.placementY());
        return new Pair(firstIsLeft ? text.get(0) : text.get(1), firstIsLeft ? text.get(1) : text.get(0),
                Math.max(0, left[0] - box.placementX()),
                right[1] - box.placementX(),
                textTop - textBottom,
                boxTop - textTop,
                textBottom - box.placementY());
    }

    /**
     * Where across the page a one-line paragraph's text runs: its line, placed in its box by
     * its alignment. A paragraph's box is often as wide as the band while its text is not — a
     * right-aligned date spans the band and sets its text at the right end — so the boxes of a
     * pair overlap where their text does not.
     *
     * @return {@code {left, right}} on the page, or {@code null} when the line is unknown
     */
    private static double[] inked(ParagraphNode paragraph, PlacedNode box, DocxLayoutMetrics layout) {
        var line = layout.firstLine(paragraph);
        if (line.isEmpty()) {
            return null;
        }
        double inside = box.placementWidth() - box.padding().left() - box.padding().right();
        double width = Math.min(line.get().width(), inside);
        double start = box.placementX() + box.padding().left();
        double offset = switch (paragraph.align() == null ? TextAlign.LEFT : paragraph.align()) {
            case RIGHT -> inside - width;
            case CENTER -> (inside - width) / 2;
            default -> 0;
        };
        return new double[]{start + offset, start + offset + width};
    }

    /** Whether a node is drawing and nothing else, which the export does not write. */
    private static boolean isDrawing(DocumentNode node) {
        return node instanceof ShapeNode || node instanceof LineNode || node instanceof EllipseNode
               || node instanceof PathNode || node instanceof PolygonNode;
    }
}
