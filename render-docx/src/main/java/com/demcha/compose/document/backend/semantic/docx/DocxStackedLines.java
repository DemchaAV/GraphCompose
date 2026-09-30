package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ParagraphNode;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Function;

/**
 * The lines of text a container lays over one another, and the line each is written at.
 *
 * <p>A container's layers are written one after the other. A title set as layers a pitch
 * apart, tighter than its face's own line — the page overlaps the line boxes — was written a
 * line at a time at each line's own height: {@code NorthlineProposal}'s three 46pt title
 * lines, 48pt apart on the page, took 70pt each in Word, and everything under them on the
 * cover stood 44pt low while the shapes drawn where the page puts them stayed.</p>
 *
 * <p>Written a pitch tall instead, the lines were cut: Word draws an exact line's text on screen
 * only inside the line, and a 46pt line's letters, seated where the page sets them, reach past a
 * 48pt step — "Proposal" lost its descenders and "Brand Refresh" the tops of its capitals. So
 * each line of a stack ends halfway between its own letters and the next line's, where the page
 * leaves room between them, and the stack ends at the container's foot or, where its last
 * letters hang past it, below them. Every line holds its letters whole; the lines together are
 * as tall as the page's, and the text is seated in each where the page sets it (see
 * {@code DocxSemanticBackend#shiftToThePagesBaseline}).</p>
 */
final class DocxStackedLines {

    /**
     * The room kept below the stack's last letters, in points. Between two lines the edge is
     * halfway between their letters, however close: {@code EditorialProposal}'s 41.8pt title,
     * set 41.9pt apart, leaves a point between the "p" of one line and the capitals of the next,
     * and the raise that seats each is rounded by at most a quarter point.
     */
    static final double INK_MARGIN = 0.75;

    private DocxStackedLines() {
    }

    /**
     * One line of a stack as written.
     *
     * @param height   its exact height, in points
     * @param topAbove how far above the page's line the Word line starts, in points
     * @param hang     how far the stack's last line runs past the container's foot, in points;
     *                 0 for any other line
     */
    record Line(double height, double topAbove, double hang) {
    }

    /**
     * The line each paragraph of a stack is written at.
     *
     * <p>A stack is a run of layers, each a paragraph of one laid-out line on one page, whose
     * box the next one's starts above the foot of and overlaps across — a value set beside its
     * label a few points lower is a line of its own. A stack is written only where the letters
     * of each line stay clear of the next one's, and each line's letters can be read: a line
     * holding a picture, whose height is not its letters', keeps its own height, as does every
     * line of its stack.</p>
     *
     * @param layers a container's children, in the order they are written
     * @param foot   the foot of the container's content, measured up from the foot of the page,
     *               or {@code NaN} when the last line is to end where its own line does
     * @param layout where the layout placed them
     * @param ink    the reach of a paragraph's letters above and below its baseline, or
     *               {@code null} when it cannot be read (see {@link DocxInk})
     * @return each stacked paragraph's line
     */
    static Map<ParagraphNode, Line> of(List<DocumentNode> layers, double foot, DocxLayoutMetrics layout,
                                       Function<ParagraphNode, double[]> ink) {
        Map<ParagraphNode, Line> lines = new IdentityHashMap<>();
        List<ParagraphNode> stack = new ArrayList<>();
        for (int i = 0; i < layers.size(); i++) {
            DocumentNode layer = layers.get(i);
            boolean continues = !stack.isEmpty() && layer instanceof ParagraphNode next
                                && laidOver(stack.get(stack.size() - 1), next, layout);
            if (!continues) {
                write(stack, false, foot, layout, ink, lines);
                stack.clear();
            }
            if (layer instanceof ParagraphNode paragraph && oneLine(paragraph, layout)) {
                stack.add(paragraph);
            }
        }
        boolean endsTheContainer = !stack.isEmpty() && stack.get(stack.size() - 1) == layers.get(layers.size() - 1);
        write(stack, endsTheContainer, foot, layout, ink, lines);
        return lines;
    }

    /** Whether the page lays {@code below} over {@code above}: its box starts above that one's foot. */
    private static boolean laidOver(ParagraphNode above, ParagraphNode below, DocxLayoutMetrics layout) {
        if (!oneLine(below, layout)) {
            return false;
        }
        PlacedNode upper = layout.placement(above);
        PlacedNode lower = layout.placement(below);
        if (upper == null || lower == null || lower.startPage() != upper.startPage()) {
            return false;
        }
        // The page's y runs up from a box's foot.
        boolean overlaps = lower.placementY() + lower.placementHeight() > upper.placementY() + 0.01;
        boolean under = lower.placementX() < upper.placementX() + upper.placementWidth()
                        && upper.placementX() < lower.placementX() + lower.placementWidth();
        return overlaps && under;
    }

    private static boolean oneLine(ParagraphNode paragraph, DocxLayoutMetrics layout) {
        PlacedNode box = layout.placement(paragraph);
        return layout.lineCount(paragraph) == 1 && box != null && box.startPage() == box.endPage();
    }

    /**
     * Writes a stack's lines into {@code lines}, measured down from its first line's top.
     *
     * @param last whether the stack ends the container, and so may end at its foot
     */
    private static void write(List<ParagraphNode> stack, boolean last, double foot, DocxLayoutMetrics layout,
                              Function<ParagraphNode, double[]> ink, Map<ParagraphNode, Line> lines) {
        int n = stack.size();
        if (n < 2) {
            return;
        }
        double[] top = new double[n];
        double[] inkTop = new double[n];
        double[] inkFoot = new double[n];
        double ownFoot = 0;
        double first = Double.NaN;
        for (int k = 0; k < n; k++) {
            ParagraphNode paragraph = stack.get(k);
            OptionalDouble lineTop = layout.firstLineTop(paragraph);
            java.util.Optional<ParagraphLine> line = layout.firstLine(paragraph);
            double[] reach = ink.apply(paragraph);
            if (lineTop.isEmpty() || line.isEmpty() || reach == null) {
                return;
            }
            if (k == 0) {
                first = lineTop.getAsDouble();
            }
            top[k] = first - lineTop.getAsDouble();
            double baseline = top[k] + line.get().lineHeight() - line.get().baselineOffsetFromBottom();
            inkTop[k] = baseline - reach[0];
            inkFoot[k] = baseline + reach[1];
            ownFoot = top[k] + line.get().lineHeight();
        }
        if (inkTop[0] < 0) {
            return;
        }
        double[] edge = new double[n + 1];
        for (int k = 1; k < n; k++) {
            // Letters the page sets into the next line's cannot both stand whole in two lines.
            if (inkTop[k] < inkFoot[k - 1]) {
                return;
            }
            edge[k] = (inkFoot[k - 1] + inkTop[k]) / 2;
        }
        double end = Math.max(ownFoot, inkFoot[n - 1] + INK_MARGIN);
        double hang = 0;
        if (last && !Double.isNaN(foot)) {
            double containerFoot = first - foot;
            if (ownFoot > containerFoot) {
                end = Math.max(containerFoot, inkFoot[n - 1] + INK_MARGIN);
                hang = Math.max(0, end - containerFoot);
            }
        }
        edge[n] = end;
        for (int k = 0; k < n; k++) {
            lines.put(stack.get(k), new Line(edge[k + 1] - edge[k], top[k] - edge[k], k == n - 1 ? hang : 0));
        }
    }
}
