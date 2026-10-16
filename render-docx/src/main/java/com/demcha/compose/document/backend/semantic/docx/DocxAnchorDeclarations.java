package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.node.BarcodeNode;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.LineNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.table.DocumentTableCell;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The anchors a document's nodes declare, counted node by node.
 *
 * <p>The layout registers an anchor where it draws the node, and that is not once a node: every
 * cell of a table built from a node registers its anchor under the table's path, and a header row
 * the page repeats registers its anchor again on each page it is drawn on. Counted off the layout,
 * two cells of one table declaring an anchor were one node, and the file keeping one of their
 * bookmarks went unsaid. Counted here, off the document, each node declaring an anchor is one,
 * a table's composed cells and a header row included.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxAnchorDeclarations {

    private DocxAnchorDeclarations() {
    }

    /**
     * Adds what a section's nodes declare to what the sections before it declared.
     *
     * @param graph             the section's document
     * @param nodes             how many nodes declare each anchor, added to
     * @param inARepeatedHeader the anchors a node in a table's repeated header row declares,
     *                          added to
     */
    static void count(DocumentGraph graph, Map<String, Integer> nodes, Set<String> inARepeatedHeader) {
        for (DocumentNode root : graph.roots()) {
            count(root, false, nodes, inARepeatedHeader);
        }
    }

    private static void count(DocumentNode node, boolean inAHeader, Map<String, Integer> nodes,
                              Set<String> inARepeatedHeader) {
        String anchor = declaredBy(node);
        if (anchor != null) {
            nodes.merge(anchor, 1, Integer::sum);
            if (inAHeader) {
                inARepeatedHeader.add(anchor);
            }
        }
        Set<DocumentNode> repeated = repeatedHeaderContent(node);
        for (DocumentNode child : DocxWrittenTree.under(node)) {
            count(child, inAHeader || repeated.contains(child), nodes, inARepeatedHeader);
        }
    }

    /** What the cells of a table's repeated header rows are built from; empty for any other node. */
    private static Set<DocumentNode> repeatedHeaderContent(DocumentNode node) {
        if (!(node instanceof TableNode table) || table.repeatedHeaderRowCount() <= 0) {
            return Set.of();
        }
        Set<DocumentNode> content = Collections.newSetFromMap(new IdentityHashMap<>());
        List<List<DocumentTableCell>> rows = table.rows();
        for (int row = 0; row < Math.min(table.repeatedHeaderRowCount(), rows.size()); row++) {
            for (DocumentTableCell cell : rows.get(row)) {
                if (cell != null && cell.content() != null) {
                    content.add(cell.content());
                }
            }
        }
        return content;
    }

    /**
     * The anchor a node declares — the place a link and a page reference go to — or null. A page
     * reference names an anchor and declares none.
     *
     * @param node a node of the document
     * @return its anchor, or null when it declares none
     */
    static String declaredBy(DocumentNode node) {
        String anchor = null;
        if (node instanceof ParagraphNode paragraph) {
            anchor = paragraph.anchor();
        } else if (node instanceof SectionNode section) {
            anchor = section.anchor();
        } else if (node instanceof ContainerNode container) {
            anchor = container.anchor();
        } else if (node instanceof TableNode table) {
            anchor = table.anchor();
        } else if (node instanceof ImageNode image) {
            anchor = image.anchor();
        } else if (node instanceof BarcodeNode barcode) {
            anchor = barcode.anchor();
        } else if (node instanceof LineNode line) {
            anchor = line.anchor();
        } else if (node instanceof ShapeNode shape) {
            anchor = shape.anchor();
        } else if (node instanceof EllipseNode ellipse) {
            anchor = ellipse.anchor();
        }
        return anchor == null || anchor.isBlank() ? null : anchor;
    }
}
