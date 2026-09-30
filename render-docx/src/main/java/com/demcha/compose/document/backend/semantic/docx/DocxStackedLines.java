package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * The lines of text a container lays over one another, and the line each is written at.
 *
 * <p>A container's layers are written one after the other. A title set as layers a pitch
 * apart, tighter than its face's own line — the page overlaps the line boxes — was written a
 * line at a time at each line's own height: {@code NorthlineProposal}'s three 46pt title
 * lines, 48pt apart on the page, took 70pt each in Word, and everything under them on the
 * cover stood 44pt low while the shapes drawn where the page puts them stayed. So each line
 * laid over by the next is written as tall as the distance down to the next one's top, and in a
 * shape container on one page the last as tall as the rest of the container where its own line
 * runs past it: every line starts where the page starts it, and such a stack is as tall as its
 * container. Where each line's text stands in its shorter line is
 * the text's seat, not the line's height (see {@code DocxSemanticBackend#shiftToThePagesBaseline}).</p>
 */
final class DocxStackedLines {

    /**
     * The least share of its face a line is squeezed to. Measured, Word sets a 16pt word whole
     * in a 10.7pt exact line — {@code EditorialProposal}'s "STUDIO" under its "NORTHLINE", 0.67
     * of its face; a line squeezed further than that is left at its own height rather than
     * risked.
     */
    static final double LEAST_SHARE_OF_FACE = 0.65;

    private DocxStackedLines() {
    }

    /**
     * The line each paragraph of a stack is written at.
     *
     * <p>A paragraph is laid over by the next layer when both are paragraphs of one laid-out
     * line on one page, the next one's box starts above this one's foot and overlaps it across
     * — a value set beside its label a few points lower is a line of its own — and the
     * distance between their tops is shorter than this one's own line and no shorter than
     * {@link #LEAST_SHARE_OF_FACE} of its largest face. It is written as tall as that distance.
     * The last layer, laid over the one before it, is written as tall as the room from its top
     * to the container's foot when its own line runs past it, under the same least share. A line
     * holding anything but text, whose picture Word could cut off, keeps its own height.</p>
     *
     * @param layers a container's children, in the order they are written
     * @param foot   the foot of the container's content, measured up from the foot of the page,
     *               or {@code NaN} when the last layer is to keep its own line
     * @param layout where the layout placed them
     * @return each such paragraph's line, in points
     */
    static Map<ParagraphNode, Double> of(List<DocumentNode> layers, double foot, DocxLayoutMetrics layout) {
        Map<ParagraphNode, Double> lines = new IdentityHashMap<>();
        // After the loop: whether the last layer is laid over the one before it.
        boolean laidOver = false;
        for (int i = 1; i < layers.size(); i++) {
            laidOver = false;
            if (!(layers.get(i - 1) instanceof ParagraphNode line)
                || !(layers.get(i) instanceof ParagraphNode below)
                || layout.lineCount(line) != 1 || layout.lineCount(below) != 1) {
                continue;
            }
            PlacedNode upper = layout.placement(line);
            PlacedNode lower = layout.placement(below);
            OptionalDouble own = layout.lineHeight(line);
            double face = largestFace(line);
            if (upper == null || lower == null || own.isEmpty() || Double.isNaN(face)
                || upper.startPage() != upper.endPage() || lower.startPage() != upper.startPage()
                || lower.endPage() != upper.startPage()) {
                continue;
            }
            // The page's y runs up from a box's foot.
            boolean overlaps = lower.placementY() + lower.placementHeight() > upper.placementY() + 0.01;
            boolean under = lower.placementX() < upper.placementX() + upper.placementWidth()
                            && upper.placementX() < lower.placementX() + lower.placementWidth();
            double topToTop = top(upper) - top(lower);
            if (overlaps && under && fits(topToTop, own.getAsDouble(), face)) {
                lines.put(line, topToTop);
                laidOver = true;
            }
        }
        if (laidOver && !Double.isNaN(foot) && layers.get(layers.size() - 1) instanceof ParagraphNode last) {
            PlacedNode box = layout.placement(last);
            OptionalDouble own = layout.lineHeight(last);
            double face = largestFace(last);
            if (box != null && own.isPresent() && !Double.isNaN(face) && fits(top(box) - foot, own.getAsDouble(), face)) {
                lines.put(last, top(box) - foot);
            }
        }
        return lines;
    }

    /** Whether a line may be written shorter than its own, at {@code height}. */
    private static boolean fits(double height, double own, double face) {
        return height >= face * LEAST_SHARE_OF_FACE && height < own;
    }

    private static double top(PlacedNode box) {
        return box.placementY() + box.placementHeight();
    }

    /**
     * The largest size a paragraph's text is set in: its runs' where it has runs, else its own;
     * {@code NaN} when a run is anything but text — a picture, an icon — whose height is not
     * its face.
     */
    static double largestFace(ParagraphNode paragraph) {
        if (paragraph.inlineRuns() == null || paragraph.inlineRuns().isEmpty()) {
            return paragraph.textStyle().size();
        }
        double largest = 0;
        for (InlineRun run : paragraph.inlineRuns()) {
            if (!(run instanceof InlineTextRun text)) {
                return Double.NaN;
            }
            DocumentTextStyle style = text.textStyle() != null ? text.textStyle() : paragraph.textStyle();
            largest = Math.max(largest, style.size());
        }
        return largest;
    }
}
