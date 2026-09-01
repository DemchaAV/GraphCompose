package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.*;
import com.demcha.compose.document.node.PageFieldKind;
import com.demcha.compose.document.node.PageFieldNode;
import com.demcha.compose.document.node.ParagraphNode;

import java.util.List;
import java.util.OptionalInt;

/**
 * Layout definition for {@link PageFieldNode}: renders the page number the field
 * asks for as a one-line text leaf.
 *
 * <p>The field carries a question rather than an answer, because the two export
 * lanes learn the number at different moments — a semantic backend maps it onto
 * the format's own field, and this is the other half: on a paginated render the
 * number is known, so the field resolves to it here.</p>
 *
 * <p>The number arrives through the same channel a page reference uses, keyed by
 * {@link #NUMBER_KEY} / {@link #TOTAL_KEY} rather than by an anchor name — the
 * zone that owns the band puts it there when it compiles the band's content.
 * Outside a zone nothing supplies it, and the field renders empty rather than
 * failing: a stray field is a blank, not a broken document.</p>
 *
 * @author Artem Demchyshyn
 */
public final class PageFieldDefinition implements NodeDefinition<PageFieldNode> {

    /** Reserved key under which a page zone publishes the page it is drawing. */
    public static final String NUMBER_KEY = "@page-field.number";

    /** Reserved key under which a page zone publishes the page count. */
    public static final String TOTAL_KEY = "@page-field.total";

    /** Slack so a single-token number is not char-wrapped by float rounding. */
    private static final double WRAP_SLACK_PT = 1.0;

    /**
     * Creates the page-field layout definition.
     */
    public PageFieldDefinition() {
    }

    @Override
    public Class<PageFieldNode> nodeType() {
        return PageFieldNode.class;
    }

    @Override
    public PreparedNode<PageFieldNode> prepare(PageFieldNode node, PrepareContext ctx, BoxConstraints constraints) {
        String text = resolveText(node, ctx);
        double glyph = TextFlowSupport.measureTextWidth(node.textStyle(), text, ctx.textMeasurement());
        double width = Math.min(constraints.availableWidth(), glyph + node.padding().horizontal() + WRAP_SLACK_PT);
        ParagraphNode paragraph = new ParagraphNode(node.name(), text, node.textStyle(), node.align(),
                0.0, node.padding(), node.margin());
        PreparedNode<ParagraphNode> prepared =
                TextFlowSupport.prepareParagraph(paragraph, ctx, BoxConstraints.natural(width));
        return PreparedNode.leaf(node, prepared.measureResult(), new PreparedPageField(prepared));
    }

    @Override
    public PaginationPolicy paginationPolicy(PageFieldNode node) {
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<PageFieldNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        PreparedPageField layout = prepared.requirePreparedLayout(PreparedPageField.class);
        return TextFlowSupport.emitParagraphFragments(layout.paragraph(), placement);
    }

    private static String resolveText(PageFieldNode node, PrepareContext ctx) {
        OptionalInt value = ctx.resolvedPage(
                node.kind() == PageFieldKind.TOTAL ? TOTAL_KEY : NUMBER_KEY);
        return value.isPresent() ? Integer.toString(value.getAsInt()) : "";
    }

    /** Carries the prepared transient paragraph from prepare to emit. */
    private record PreparedPageField(PreparedNode<ParagraphNode> paragraph) implements PreparedNodeLayout {
    }
}
