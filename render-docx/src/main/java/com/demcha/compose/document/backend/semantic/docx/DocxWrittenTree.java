package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.table.DocumentTableCell;

import java.util.ArrayList;
import java.util.List;

/**
 * What the export reaches under a node, for the passes that read the document before any of it is
 * written.
 *
 * <p>A table holds the content of a cell built from a node in the cell, not among its children.
 * Walked by its children alone, the document lost what its tables' cells are built from to the
 * passes made before the body: a face set only in a cell was not shipped
 * ({@link DocxFontTable}), and a heading in a cell named a heading style the styles part did not
 * define.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxWrittenTree {

    private DocxWrittenTree() {
    }

    /**
     * A node's children and, for a table, the content of each cell built from a node, row by row.
     *
     * @param node a node of the document
     * @return what the export reaches directly under it, whether or not it ends up writing it
     */
    static List<DocumentNode> under(DocumentNode node) {
        if (!(node instanceof TableNode table)) {
            return node.children();
        }
        List<DocumentNode> under = new ArrayList<>(node.children());
        for (List<DocumentTableCell> row : table.rows()) {
            for (DocumentTableCell cell : row) {
                if (cell != null && cell.content() != null) {
                    under.add(cell.content());
                }
            }
        }
        return under;
    }
}
