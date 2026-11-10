package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.ParagraphDirection;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.LineNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.PathNode;
import com.demcha.compose.document.node.PolygonNode;
import com.demcha.compose.document.node.ShapeContainerNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextDirection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Single-line paragraphs an overlay sets side by side on one line, as one Word line.
 *
 * <p>A CV entry's head band puts its title at the left and its dates at the right of the same
 * line, as two layers of one container. Word has no layers, so the export wrote the layers
 * one after the other, and the dates came out on a line of their own under the title: an
 * extra line in every entry, enough to push a one-page CV onto a second page. A line with
 * text at the left and more along it is what Word builds with tab stops, so that is what the
 * line is written as: the leftmost paragraph's runs, then a tab and the next one's runs for
 * each paragraph further along (see {@link Stop}). A band of three captions — a title, a place
 * in the middle, dates at the right — is one line the same way.</p>
 *
 * <p>An overlay is taken as such a line when it holds two or more paragraphs, each laid out on
 * one line, all level with one another on the page and each {@link #MIN_GAP} apart from the
 * next across it, and nothing else but drawing the export does not write. A paragraph the page
 * sets over two lines or more is not one line's part, and its overlay is written as before.</p>
 */
final class DocxLinePair {

    /** How far apart two edges may be and still be the same edge, in points. */
    private static final double EDGE = 0.5;

    /**
     * The room each text must leave before the next one along the line, in points, and a text
     * set from its start after its end. An editor sets text a little wider or narrower than the
     * page does: a tab the text before it runs past, or a last text run past the line's end,
     * sends a word onto a line of its own — under an exact line height, out of sight.
     */
    private static final double MIN_GAP = 4;

    private DocxLinePair() {
    }

    /** How a tab stop holds the text written after it: by its start, its middle or its end. */
    enum Tab {
        /** Its text starts at the stop. */
        LEFT,
        /** Its text is centred on the stop. */
        CENTER,
        /** Its text ends at the stop. */
        RIGHT
    }

    /**
     * A paragraph written after a tab in the shared line, and the stop that sets it there.
     *
     * <p>The last paragraph along the line is held by its end — a right stop where its text
     * ends — unless it is set from its start ({@link #setFromItsStart}) with room past its end,
     * when a left stop holds its start where the page puts it, and an editor setting it narrower
     * or wider moves its end, as on the page, not its start. A paragraph between the first and
     * the last is held where the page sets it: a centred one by its middle and a right-aligned
     * one by its end, since the layout gives such a paragraph a box as wide as the band and its
     * alignment sets its text there; a left-aligned one, whose box the layout shrinks to its
     * text, by what its layer sets it from — its start in a layer placed from the left, its
     * middle in a centred one, its end in one placed from the right. An editor setting it wider
     * or narrower then moves it as the page would.</p>
     *
     * @param text the paragraph
     * @param at   where its stop stands, from the overlay's left edge, in points
     * @param tab  how the stop holds it
     */
    record Stop(ParagraphNode text, double at, Tab tab) {
    }

    /**
     * An overlay's shared line.
     *
     * @param left       the paragraph the line starts with, at the left
     * @param stops      the paragraphs further along the line, left to right, each after a tab
     * @param leftOffset where the left paragraph starts, from the overlay's left edge
     * @param line       the height the lines take together, from the top of the highest to the
     *                   bottom of the lowest
     * @param above      from the overlay's top down to that top — negative where the text
     *                   stands above the overlay
     * @param below      from that bottom down to the overlay's bottom — negative where the
     *                   text hangs below it
     */
    record Pair(ParagraphNode left, List<Stop> stops, double leftOffset, double line, double above,
                double below) {

        Pair {
            stops = List.copyOf(stops);
        }

        /** The paragraph the line ends with, at the right. */
        ParagraphNode right() {
            return stops.get(stops.size() - 1).text();
        }

        /** Every paragraph of the line, left to right. */
        List<ParagraphNode> paragraphs() {
            List<ParagraphNode> all = new ArrayList<>(stops.size() + 1);
            all.add(left);
            for (Stop stop : stops) {
                all.add(stop.text());
            }
            return all;
        }
    }

    /**
     * The line an overlay sets its paragraphs on, or {@code null} when it does not set one.
     *
     * @param overlay a layer stack or a shape container
     * @param layout  where the layout placed the overlay and its layers
     * @return the line, or {@code null}
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
        if (text.size() < 2) {
            return null;
        }
        // A right-to-left line runs the other way from its tab stops; such a line stays as it was.
        List<PlacedNode> placed = new ArrayList<>(text.size());
        List<double[]> inked = new ArrayList<>(text.size());
        for (ParagraphNode paragraph : text) {
            PlacedNode place = layout.placement(paragraph);
            if (ParagraphDirection.resolve(paragraph) == TextDirection.RTL
                || place == null || layout.lineCount(paragraph) != 1
                || !placed.isEmpty() && place.startPage() != placed.get(0).startPage()) {
                return null;
            }
            placed.add(place);
        }
        // Placements measure up from the page's foot. Level with one another: every line shares
        // some of its height with every other.
        double textTop = Double.NEGATIVE_INFINITY;
        double textBottom = Double.POSITIVE_INFINITY;
        double lowestTop = Double.POSITIVE_INFINITY;
        double highestBottom = Double.NEGATIVE_INFINITY;
        for (PlacedNode place : placed) {
            double top = place.placementY() + place.placementHeight();
            textTop = Math.max(textTop, top);
            textBottom = Math.min(textBottom, place.placementY());
            lowestTop = Math.min(lowestTop, top);
            highestBottom = Math.max(highestBottom, place.placementY());
        }
        if (lowestTop - highestBottom <= EDGE) {
            return null;
        }
        for (int index = 0; index < text.size(); index++) {
            double[] ink = inked(text.get(index), placed.get(index), layout);
            if (ink == null) {
                return null;
            }
            inked.add(ink);
        }
        // Left to right. Two texts starting at the same place fail the gap below.
        List<Integer> order = new ArrayList<>(text.size());
        for (int index = 0; index < text.size(); index++) {
            order.add(index);
        }
        order.sort(Comparator.comparingDouble(index -> inked.get(index)[0]));
        for (int at = 1; at < order.size(); at++) {
            if (inked.get(order.get(at - 1))[1] + MIN_GAP > inked.get(order.get(at))[0]) {
                return null;
            }
        }
        List<Stop> stops = new ArrayList<>(order.size() - 1);
        for (int at = 1; at < order.size(); at++) {
            ParagraphNode paragraph = text.get(order.get(at));
            double[] ink = inked.get(order.get(at));
            Tab tab;
            if (at == order.size() - 1) {
                // Held by its start, its end runs free: an editor setting it wider breaks its last
                // word onto a line of its own unless the overlay leaves room past it. Held by its
                // end, it cannot.
                tab = setFromItsStart(overlay, paragraph)
                      && box.placementX() + box.placementWidth() - ink[1] >= MIN_GAP ? Tab.LEFT : Tab.RIGHT;
            } else {
                tab = switch (paragraph.align() == null ? TextAlign.LEFT : paragraph.align()) {
                    case CENTER -> Tab.CENTER;
                    case RIGHT -> Tab.RIGHT;
                    default -> heldByItsLayer(overlay, paragraph);
                };
            }
            double stop = switch (tab) {
                case LEFT -> ink[0];
                case CENTER -> (ink[0] + ink[1]) / 2;
                case RIGHT -> ink[1];
            };
            stops.add(new Stop(paragraph, stop - box.placementX(), tab));
        }
        double boxTop = box.placementY() + box.placementHeight();
        return new Pair(text.get(order.get(0)), stops,
                Math.max(0, inked.get(order.get(0))[0] - box.placementX()),
                textTop - textBottom,
                boxTop - textTop,
                textBottom - box.placementY());
    }

    /**
     * Whether a paragraph's text is set from its start: aligned left in a layer placed from the
     * overlay's left. A bank detail's value set at a column across the card starts there
     * whatever its length; written after a right tab at its end, as a date at the right of a band
     * is, it started wherever the editor's narrower setting of it ended up — "GB36 SRLG …" a few
     * points right of the values over it.
     */
    private static boolean setFromItsStart(DocumentNode overlay, ParagraphNode paragraph) {
        if (paragraph.align() != TextAlign.LEFT) {
            return false;
        }
        LayerAlign align = layerAlignOf(overlay, paragraph);
        return align == LayerAlign.TOP_LEFT || align == LayerAlign.CENTER_LEFT || align == LayerAlign.BOTTOM_LEFT;
    }

    /**
     * How a left-aligned paragraph's layer holds it across: by its start in a layer placed from
     * the left, its middle in a centred one, its end in one placed from the right. The layout
     * shrinks a left-aligned paragraph's box to its text, so the layer, not the alignment, sets
     * where the text stands — a place in the middle of a band, left-aligned by default in a
     * centred layer, is centred on the page.
     */
    private static Tab heldByItsLayer(DocumentNode overlay, ParagraphNode paragraph) {
        LayerAlign align = layerAlignOf(overlay, paragraph);
        if (align == null) {
            return Tab.LEFT;
        }
        return switch (align) {
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> Tab.CENTER;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> Tab.RIGHT;
            default -> Tab.LEFT;
        };
    }

    /** The alignment of the layer an overlay holds a paragraph in, or {@code null}. */
    private static LayerAlign layerAlignOf(DocumentNode overlay, ParagraphNode paragraph) {
        List<LayerStackNode.Layer> layers = overlay instanceof LayerStackNode stack ? stack.layers()
                : overlay instanceof ShapeContainerNode container ? container.layers()
                : List.of();
        for (LayerStackNode.Layer layer : layers) {
            if (layer.node() == paragraph) {
                return layer.align();
            }
        }
        return null;
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
