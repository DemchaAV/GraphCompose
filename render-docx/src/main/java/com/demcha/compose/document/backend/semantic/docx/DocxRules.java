package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LineNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentDashPattern;
import com.demcha.compose.document.style.DocumentTransform;

import java.util.List;

/**
 * Which drawn nodes are a horizontal rule, and where on their box the rule runs.
 *
 * <p>Word has a horizontal rule of its own — the bottom border of a paragraph, which is what
 * it makes itself when a reader types three hyphens and Enter — and it is a paragraph, so it
 * flows with the text around it and a reader moves or deletes it as a line of the document.
 * Two drawn nodes are that rule on the page:</p>
 *
 * <ul>
 *   <li>a {@link LineNode} whose two ends sit at one height, with no transform; and</li>
 *   <li>a {@link ShapeNode} that is only a fill — no stroke, no rounded corners, no gradient,
 *       no transform — and no taller than Word's thickest border, which is how
 *       {@code addDivider} draws one.</li>
 * </ul>
 *
 * <p>Anything else drawn — a vertical or slanted line, a box, a circle — is not a rule, and
 * this returns {@code null} for it.</p>
 */
final class DocxRules {

    /** Word's thickest border: {@code w:sz} counts eighths of a point and stops at 96. */
    static final double MAX_RULE_POINTS = 12.0;

    private static final double EPS = 1e-6;

    private DocxRules() {
    }

    /**
     * A rule on its node's box, measured in points.
     *
     * @param boxWidth   the node's content box width, when the node states it
     * @param fillsWidth whether the rule runs to the right edge of whatever width it is given
     * @param boxHeight  the node's content box height
     * @param startX     where the rule starts, from the box's left edge
     * @param endX       where it ends, from the box's left edge, unless it fills the width
     * @param centreFromTop where the middle of the stroke sits, from the box's top edge
     * @param thickness  the stroke's thickness
     * @param colour     its colour
     * @param dashed     its dash pattern, or {@code null} for a solid rule
     */
    record Rule(double boxWidth, boolean fillsWidth, double boxHeight, double startX, double endX,
                double centreFromTop, double thickness, DocumentColor colour, List<Double> dashed) {
    }

    /** The rule a node draws, or {@code null} when it draws something else. */
    static Rule of(DocumentNode node) {
        if (node instanceof LineNode line) {
            return ofLine(line);
        }
        if (node instanceof ShapeNode shape) {
            return ofShape(shape);
        }
        return null;
    }

    private static Rule ofLine(LineNode line) {
        if (Math.abs(line.startY() - line.endY()) > EPS || transformed(line.transform())
            || line.stroke() == null || !(line.stroke().width() > 0) || line.stroke().color() == null) {
            return null;
        }
        double from = Math.min(line.startX(), line.endX());
        double to = Math.max(line.startX(), line.endX());
        DocumentDashPattern pattern = line.dashPattern();
        return new Rule(line.width(), line.fillWidth(), line.height(), from, to,
                // The page measures y from the bottom of the box.
                line.height() - line.startY(),
                line.stroke().width(), line.stroke().color(),
                pattern == null || pattern.segments().isEmpty() ? null : pattern.segments());
    }

    private static Rule ofShape(ShapeNode shape) {
        boolean stroked = shape.stroke() != null && shape.stroke().width() > 0;
        boolean rounded = shape.cornerRadius() != null && !shape.cornerRadius().isZero();
        if (shape.fillColor() == null || shape.fillPaint() != null || stroked || rounded
            || transformed(shape.transform())
            || !(shape.height() > 0) || shape.height() > MAX_RULE_POINTS || !(shape.width() > 0)) {
            return null;
        }
        return new Rule(shape.width(), false, shape.height(), 0, shape.width(),
                shape.height() / 2.0, shape.height(), shape.fillColor(), null);
    }

    private static boolean transformed(DocumentTransform transform) {
        return transform != null && !transform.isIdentity();
    }
}
