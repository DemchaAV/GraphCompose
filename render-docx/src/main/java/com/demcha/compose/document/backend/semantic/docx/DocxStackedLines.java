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
 * cover stood 44pt low while the shapes drawn where the page puts them stayed. Word sets the
 * foot of an exactly-spaced line at the bottom of its line, so the first line keeps its own
 * height and each line after it takes the distance from the foot of the line above to its
 * own: every line's foot, and the stack's bottom, land where the page puts them.</p>
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
     * The line each paragraph laid over the layer before it is written at.
     *
     * <p>A paragraph counts when it and the layer before it are paragraphs of one laid-out
     * line on one page, its box starts above that line's foot and overlaps it across — a value
     * set beside its label a few points lower is a line of its own — and the distance between
     * the two feet is shorter than its own line and no shorter than {@link #LEAST_SHARE_OF_FACE}
     * of its largest face. A line holding anything but text, whose picture Word could cut off,
     * keeps its own height.</p>
     *
     * @param layers a container's children, in the order they are written
     * @param layout where the layout placed them
     * @return each such paragraph's line, in points
     */
    static Map<ParagraphNode, Double> of(List<DocumentNode> layers, DocxLayoutMetrics layout) {
        Map<ParagraphNode, Double> lines = new IdentityHashMap<>();
        for (int i = 1; i < layers.size(); i++) {
            if (!(layers.get(i - 1) instanceof ParagraphNode above)
                || !(layers.get(i) instanceof ParagraphNode line)
                || layout.lineCount(above) != 1 || layout.lineCount(line) != 1) {
                continue;
            }
            PlacedNode upper = layout.placement(above);
            PlacedNode lower = layout.placement(line);
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
            double footToFoot = upper.placementY() - lower.placementY();
            if (overlaps && under && footToFoot >= face * LEAST_SHARE_OF_FACE && footToFoot < own.getAsDouble()) {
                lines.put(line, footToFoot);
            }
        }
        return lines;
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
