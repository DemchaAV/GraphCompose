package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.EllipseFragmentPayload;
import com.demcha.compose.document.layout.payloads.LineFragmentPayload;
import com.demcha.compose.document.layout.payloads.PathFragmentPayload;
import com.demcha.compose.document.layout.payloads.PolygonFragmentPayload;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.LineNode;
import com.demcha.compose.document.node.PathNode;
import com.demcha.compose.document.node.PolygonNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentInsets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Finds the fragments of a table that draw one drawing composed in one of its cells.
 *
 * <p>Content a table cell composes has no place of its own in the layout: its fragments are the
 * table's, under the table's path, so nothing says which drawing a fragment was painted for.
 * Within one of the table's cells they come in the order the cell's content is laid out, which
 * is the order the export writes it in; so when the first of the fragments still waiting in the
 * cell are a drawing's shapes — each of its kind and size, in the order the page paints them,
 * on one page — they are its drawing.</p>
 */
final class DocxCellDrawings {

    /** How far a fragment's size may part from its node's and still be its, in points. */
    private static final double SIZE_TOLERANCE = 0.01;

    private DocxCellDrawings() {
    }

    /** The kind of drawing node a fragment is painted for, or {@code null} for any other. */
    static Class<? extends DocumentNode> drawnKind(PlacedFragment fragment) {
        Object payload = fragment.payload();
        if (payload instanceof ShapeFragmentPayload) {
            return ShapeNode.class;
        }
        if (payload instanceof EllipseFragmentPayload) {
            return EllipseNode.class;
        }
        if (payload instanceof LineFragmentPayload) {
            return LineNode.class;
        }
        if (payload instanceof PathFragmentPayload) {
            return PathNode.class;
        }
        if (payload instanceof PolygonFragmentPayload) {
            return PolygonNode.class;
        }
        return null;
    }

    /**
     * Whether a drawing holds a line anywhere in it. A line alone in a cell, or in a stack of
     * one layer, is a rule and written as one; a drawing holding a line is left where it was
     * drawn before, rather than told apart from a rule here.
     */
    static boolean holdsALine(DocumentNode node) {
        if (node instanceof LineNode) {
            return true;
        }
        for (DocumentNode child : node.children()) {
            if (holdsALine(child)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A layer stack's shapes in the order the page paints them, or {@code null} when it holds
     * anything else — a container outline, a picture, text — or a stack in it has a margin.
     */
    static List<DocumentNode> shapesOf(LayerStackNode stack) {
        List<DocumentNode> shapes = new ArrayList<>();
        return collect(stack, shapes) && !shapes.isEmpty() ? shapes : null;
    }

    private static boolean collect(DocumentNode node, List<DocumentNode> shapes) {
        if (sizeOf(node) != null) {
            shapes.add(node);
            return true;
        }
        if (!(node instanceof LayerStackNode stack) || stack.layers().isEmpty()) {
            return false;
        }
        DocumentInsets margin = stack.margin();
        if (margin != null && (margin.top() != 0 || margin.right() != 0 || margin.bottom() != 0 || margin.left() != 0)) {
            return false;
        }
        List<LayerStackNode.Layer> layers = stack.layers().stream()
                .sorted(Comparator.comparingInt(LayerStackNode.Layer::zIndex))
                .toList();
        for (LayerStackNode.Layer layer : layers) {
            if (!collect(layer.node(), shapes)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the first of the waiting fragments are these shapes, each of its kind and size,
     * one after another, on one page.
     *
     * @param waiting the fragments not yet anchored that one cell holds, in the order the layout
     *                emitted them
     * @param shapes  a drawing's shapes, in the order the page paints them
     */
    static boolean opensWith(List<PlacedFragment> waiting, List<DocumentNode> shapes) {
        if (waiting.size() < shapes.size()) {
            return false;
        }
        int page = waiting.get(0).pageIndex();
        for (int i = 0; i < shapes.size(); i++) {
            PlacedFragment fragment = waiting.get(i);
            DocumentNode shape = shapes.get(i);
            double[] size = sizeOf(shape);
            if (fragment.pageIndex() != page || drawnKind(fragment) != shape.getClass()
                || Math.abs(fragment.width() - size[0]) >= SIZE_TOLERANCE
                || Math.abs(fragment.height() - size[1]) >= SIZE_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    /** A drawing node's width and height, in points, or {@code null} for any other node. */
    private static double[] sizeOf(DocumentNode node) {
        if (node instanceof ShapeNode shape) {
            return new double[]{shape.width(), shape.height()};
        }
        if (node instanceof EllipseNode ellipse) {
            return new double[]{ellipse.width(), ellipse.height()};
        }
        if (node instanceof LineNode line) {
            return new double[]{line.width(), line.height()};
        }
        if (node instanceof PathNode path) {
            return new double[]{path.width(), path.height()};
        }
        if (node instanceof PolygonNode polygon) {
            return new double[]{polygon.width(), polygon.height()};
        }
        return null;
    }
}
