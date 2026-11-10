package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.PageBreakNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Where a panel the page breaks over pages is cut into tables: at each page break among its
 * children, and among the children of each section or panel its cell writes them in, at any depth.
 *
 * <p>Word breaks no page inside a table cell: measured in Word and LibreOffice 24.2, a page break
 * in a cell — a run's break or a paragraph's break before it, in a table of the body or one
 * nested in a cell — left the text after it on the same page. The page breaks a card a break runs
 * through, the card going on at the top of the next page, as a section or a card inside it does.
 * So the panel's table ends at the break and a new one opens after it, on the next page, each
 * holding its part of every container the break runs through.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxPanelPieces {

    private DocxPanelPieces() {
    }

    /**
     * What one piece of a panel holds of one of a container's children: the child whole, or the
     * run of its own children from one break to the next ({@code parts}), the child opening in
     * that piece where the run starts its children and closing where it ends them.
     *
     * @param node   the child
     * @param parts  what the piece holds of the child's children, or null where it holds the child
     *               whole
     * @param opens  whether the piece holds the child's start: its top edge
     * @param closes whether the piece holds the child's end: its bottom edge
     */
    record Part(DocumentNode node, List<Part> parts, boolean opens, boolean closes) {

        static Part whole(DocumentNode node) {
            return new Part(node, null, true, true);
        }

        boolean isWhole() {
            return parts == null;
        }
    }

    /**
     * A container's children cut at the page breaks among them and among the children of those
     * of them its cell writes them in, at any depth.
     *
     * @param container        the container
     * @param writtenInItsCell whether a child is a container its parent's cell writes in it, its own
     *                         children after one another: a section or a panel there
     * @return the pieces, one more than the breaks found, each the parts it holds in order; a
     *         piece is empty where a break ends or opens the container
     */
    static List<List<Part>> of(DocumentNode container, Predicate<DocumentNode> writtenInItsCell) {
        List<List<Part>> pieces = new ArrayList<>();
        pieces.add(new ArrayList<>());
        for (DocumentNode child : container.children()) {
            if (child instanceof PageBreakNode) {
                pieces.add(new ArrayList<>());
                continue;
            }
            List<List<Part>> inner = writtenInItsCell.test(child) ? of(child, writtenInItsCell) : List.of();
            if (inner.size() < 2) {
                pieces.get(pieces.size() - 1).add(Part.whole(child));
                continue;
            }
            for (int index = 0; index < inner.size(); index++) {
                if (index > 0) {
                    pieces.add(new ArrayList<>());
                }
                pieces.get(pieces.size() - 1).add(new Part(child, inner.get(index), index == 0,
                        index == inner.size() - 1));
            }
        }
        List<List<Part>> written = new ArrayList<>(pieces.size());
        for (List<Part> piece : pieces) {
            written.add(List.copyOf(piece));
        }
        return List.copyOf(written);
    }

    /** The children a run of parts holds, whole or in part, in order. */
    static List<DocumentNode> nodesOf(List<Part> parts) {
        List<DocumentNode> nodes = new ArrayList<>(parts.size());
        for (Part part : parts) {
            nodes.add(part.node());
        }
        return nodes;
    }

    /**
     * Whether the first part of a piece after the first follows a page break among the container's
     * own children, so that the container's spacing stands above it: a part holding a child's
     * start, not one going on from a break inside that child.
     *
     * @param piece      the piece's parts
     * @param firstPiece whether the piece is the container's first
     */
    static boolean followsABreak(List<Part> piece, boolean firstPiece) {
        return !firstPiece && !piece.isEmpty() && piece.get(0).opens();
    }
}
