package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.*;
import com.demcha.compose.document.node.TableNode;

import java.util.List;

import static com.demcha.compose.document.layout.NodeDefinitionSupport.*;

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
