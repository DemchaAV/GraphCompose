package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.engine.components.content.table.TableResolvedCell;

import java.util.List;

/**
 * PDF payload for one resolved table row fragment.
 *
 * @param cells resolved cells in column order
 * @param startsPageFragment whether this row starts a table page fragment
 * @param linkOptions optional fragment-level link metadata
 * @param bookmarkOptions optional fragment-level bookmark metadata
 */
public record TableRowFragmentPayload(
        List<TableResolvedCell> cells,
        boolean startsPageFragment,
        DocumentLinkOptions linkOptions,
        DocumentBookmarkOptions bookmarkOptions
) implements PdfSemanticFragmentPayload {
    /**
     * Creates an immutable table row fragment payload.
     */
    public TableRowFragmentPayload {
        cells = List.copyOf(cells);
    }
}
