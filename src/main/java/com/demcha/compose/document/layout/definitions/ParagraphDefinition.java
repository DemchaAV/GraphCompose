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
import com.demcha.compose.document.node.ParagraphNode;

import java.util.List;

import static com.demcha.compose.document.layout.TextFlowSupport.emitParagraphFragments;
import static com.demcha.compose.document.layout.TextFlowSupport.prepareParagraph;
import static com.demcha.compose.document.layout.TextFlowSupport.splitParagraph;

/**
 * Layout definition for {@link ParagraphNode}: wraps text into visual lines,
 * supports inline runs and markdown styling, and slices into fragments for
 * pagination through {@link com.demcha.compose.document.layout.TextFlowSupport}.
 *
 * @author Artem Demchyshyn
 */
public final class ParagraphDefinition implements NodeDefinition<ParagraphNode> {
    @Override
    public Class<ParagraphNode> nodeType() {
        return ParagraphNode.class;
    }

    @Override
    public PreparedNode<ParagraphNode> prepare(ParagraphNode node, PrepareContext ctx, BoxConstraints constraints) {
        return prepareParagraph(node, ctx, constraints);
    }

    @Override
    public PaginationPolicy paginationPolicy(ParagraphNode node) {
        return PaginationPolicy.SPLITTABLE;
    }

    @Override
    public PreparedSplitResult<ParagraphNode> split(PreparedNode<ParagraphNode> prepared, SplitRequest request) {
        return splitParagraph(prepared, request);
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<ParagraphNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        return emitParagraphFragments(prepared, placement);
    }
}
