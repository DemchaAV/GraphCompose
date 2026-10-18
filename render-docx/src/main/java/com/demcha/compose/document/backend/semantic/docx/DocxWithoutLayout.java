package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.node.AlignNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.RowArrangement;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.ShapeContainerNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.table.DocumentTableCell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * What a section exported with no layout behind it leaves to the editor, as the report's note on
 * it says.
 *
 * <p>Without the layout nothing is measured or placed, and the export writes what the document
 * states. Line heights, the space between lines and auto columns are the editor's then, and so is
 * more, which the note named none of: a row its arrangement or a growing spacer divides is written
 * as columns Word sizes; a table's fixed column beside an auto one is written with no width, the
 * table's columns all left to Word; what an alignment or a stack's layer is set at across the
 * width stands at the left; and the keeps the page makes — a block kept together or with the
 * next, a table's row in one piece, a row of columns — are written only where the layout shows the
 * page held them on one page, which is nowhere. (A table's repeated header row is still kept with the row under it:
 * that needs no layout.) Each of those is named where the section holds one.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxWithoutLayout {

    private DocxWithoutLayout() {
    }

    /**
     * The note on a section exported with no layout behind it.
     *
     * @param graph the section's document
     * @param what  how the note names the section: "this document", or "this section"
     * @return the note's text
     */
    static String note(DocumentGraph graph, String what) {
        Found found = new Found();
        for (DocumentNode root : graph.roots()) {
            found.look(root, false);
        }
        List<String> editors = new ArrayList<>(List.of("line heights", "the space between lines",
                "auto column widths"));
        if (found.dividedRows) {
            editors.add("the columns of a row its arrangement or a growing spacer divides");
        }
        StringBuilder note = new StringBuilder(what).append(" could not be laid out, so ")
                .append(listed(editors)).append(" are the editor's rather than the engine's");
        if (found.fixedBesideAuto) {
            note.append("; a table's fixed column beside an auto one is written with no width either");
        }
        if (found.placedAcross) {
            note.append("; what an alignment or a stack's layer is set at across the width is written from the left");
        }
        List<String> kept = new ArrayList<>(3);
        if (found.keptBlocks) {
            kept.add("a block kept together or with the next");
        }
        if (found.tableRows) {
            kept.add("a table's row in one piece");
        }
        if (found.rowsOfColumns) {
            kept.add("a row of columns");
        }
        if (!kept.isEmpty()) {
            note.append("; Word is not told to keep what the page keeps from a page break: ")
                    .append(String.join(", ", kept));
        }
        return note.toString();
    }

    private static String listed(List<String> items) {
        return String.join(", ", items.subList(0, items.size() - 1)) + " and " + items.get(items.size() - 1);
    }

    /** What the section holds that the note names. */
    private static final class Found {

        private boolean dividedRows;
        private boolean fixedBesideAuto;
        private boolean placedAcross;
        private boolean keptBlocks;
        private boolean tableRows;
        private boolean rowsOfColumns;

        /**
         * Looks through a node and what the export reaches under it.
         *
         * @param inACell whether the node is in what a table's cell is built from, where neither a
         *                block nor a row of columns is kept on its own, with a layout or without:
         *                the table row it stands in is
         */
        private void look(DocumentNode node, boolean inACell) {
            // A row with nothing in it writes nothing.
            if (node instanceof RowNode row && !row.children().isEmpty()) {
                dividedRows |= row.arrangement() != RowArrangement.START || DocxSemanticBackend.hasGrowChild(row);
                rowsOfColumns |= !inACell;
            }
            if (node instanceof AlignNode align && align.align() != HorizontalAlign.LEFT) {
                placedAcross = true;
            }
            if (node instanceof LayerStackNode stack) {
                placedAcross |= setAcross(stack.layers());
            }
            if (node instanceof ShapeContainerNode container) {
                placedAcross |= setAcross(container.layers());
            }
            if (node instanceof TableNode table && !table.rows().isEmpty()) {
                tableRows = true;
                fixedBesideAuto |= DocxSemanticBackend.fixedBesideAuto(table);
            }
            keptBlocks |= !inACell && (node.keepTogether() || node.keepWithNext());
            Set<DocumentNode> cells = cellContentOf(node);
            for (DocumentNode child : DocxWrittenTree.under(node)) {
                look(child, inACell || cells.contains(child));
            }
        }

        /** Whether a stack sets any of its layers in from its left edge: by alignment, or by offset. */
        private static boolean setAcross(List<LayerStackNode.Layer> layers) {
            for (LayerStackNode.Layer layer : layers) {
                if (layer.offsetX() != 0 || layer.align() != LayerAlign.TOP_LEFT
                                            && layer.align() != LayerAlign.CENTER_LEFT
                                            && layer.align() != LayerAlign.BOTTOM_LEFT) {
                    return true;
                }
            }
            return false;
        }

        /** What a table's cells are built from; empty for any other node. */
        private static Set<DocumentNode> cellContentOf(DocumentNode node) {
            if (!(node instanceof TableNode table)) {
                return Set.of();
            }
            Set<DocumentNode> content = Collections.newSetFromMap(new IdentityHashMap<>());
            for (List<DocumentTableCell> row : table.rows()) {
                for (DocumentTableCell cell : row) {
                    if (cell != null && cell.content() != null) {
                        content.add(cell.content());
                    }
                }
            }
            return content;
        }
    }
}
