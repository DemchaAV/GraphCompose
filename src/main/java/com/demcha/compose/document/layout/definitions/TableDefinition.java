package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.BoxConstraints;
import com.demcha.compose.document.layout.FragmentContext;
import com.demcha.compose.document.layout.FragmentPlacement;
import com.demcha.compose.document.layout.LayoutFragment;
import com.demcha.compose.document.layout.NodeDefinition;
import com.demcha.compose.document.layout.PaginationPolicy;
import com.demcha.compose.document.layout.PrepareContext;
import com.demcha.compose.document.layout.PreparedNode;
import com.demcha.compose.document.layout.PreparedSplitResult;
import com.demcha.compose.document.layout.SplitRequest;
import com.demcha.compose.document.node.TableNode;

import java.util.List;

import static com.demcha.compose.document.layout.NodeDefinitionSupport.emitTableFragments;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.prepareTable;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.splitTable;

/**
 * Layout definition for {@link TableNode}: resolves table geometry through
 * {@link com.demcha.compose.document.layout.NodeDefinitionSupport} and emits
 * page-aware row fragments.
 *
 * @author Artem Demchyshyn
 */
public final class TableDefinition implements NodeDefinition<TableNode> {

    /**
     * Creates the table layout definition.
     */
    public TableDefinition() {
    }

    @Override
    public Class<TableNode> nodeType() {
        return TableNode.class;
    }

    @Override
    public PreparedNode<TableNode> prepare(TableNode node, PrepareContext ctx, BoxConstraints constraints) {
        return prepareTable(node, ctx, constraints);
    }

    @Override
    public PaginationPolicy paginationPolicy(TableNode node) {
        return PaginationPolicy.SPLITTABLE;
    }

    @Override
    public PreparedSplitResult<TableNode> split(PreparedNode<TableNode> prepared, SplitRequest request) {
        return splitTable(prepared, request);
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<TableNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        return emitTableFragments(prepared, ctx, placement);
    }
}
