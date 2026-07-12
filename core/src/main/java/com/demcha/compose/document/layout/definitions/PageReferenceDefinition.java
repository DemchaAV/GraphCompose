package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.*;
import com.demcha.compose.document.node.PageReferenceNode;
import com.demcha.compose.document.node.ParagraphNode;

import java.util.List;
import java.util.OptionalInt;

/**
 * Layout definition for {@link PageReferenceNode}: resolves the anchor's page
 * number from the layout pass and renders it as a one-line text leaf. The text is
 * laid out by reusing {@link TextFlowSupport}, so it shares the engine's text
 * measurement and rendering with paragraphs.
 *
 * <p>The leaf is sized to the number's glyph width (not a full text block), so an
 * unresolved reference reserves only its content width and never over-constrains
 * the row it sits in during the first pass.</p>
 *
 * @author Artem Demchyshyn
 */
public final class PageReferenceDefinition implements NodeDefinition<PageReferenceNode> {

    /** Slack added to the measured glyph width so a single-token number is not char-wrapped by float rounding. */
    private static final double WRAP_SLACK_PT = 1.0;

    /**
     * Creates the page-reference layout definition.
     */
    public PageReferenceDefinition() {
    }

    @Override
    public Class<PageReferenceNode> nodeType() {
        return PageReferenceNode.class;
    }

    @Override
    public PreparedNode<PageReferenceNode> prepare(PageReferenceNode node, PrepareContext ctx, BoxConstraints constraints) {
        String text = resolveText(node, ctx.resolvedPage(node.anchor()));
        // Size the box to the number's glyph width so the leaf measures as content,
        // not as a full-width text block (an empty placeholder otherwise claims the
        // whole row width). + 1pt guards against single-glyph wrap on float rounding.
        double glyph = TextFlowSupport.measureTextWidth(node.textStyle(), text, ctx.textMeasurement());
        double width = Math.min(constraints.availableWidth(), glyph + node.padding().horizontal() + WRAP_SLACK_PT);
        ParagraphNode paragraph = new ParagraphNode(node.name(), text, node.textStyle(), node.align(),
                0.0, node.padding(), node.margin());
        PreparedNode<ParagraphNode> prepared = TextFlowSupport.prepareParagraph(paragraph, ctx, BoxConstraints.natural(width));
        return PreparedNode.leaf(node, prepared.measureResult(), new PreparedPageReference(prepared));
    }

    @Override
    public PaginationPolicy paginationPolicy(PageReferenceNode node) {
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<PageReferenceNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        PreparedPageReference layout = prepared.requirePreparedLayout(PreparedPageReference.class);
        return TextFlowSupport.emitParagraphFragments(layout.paragraph(), placement);
    }

    private static String resolveText(PageReferenceNode node, OptionalInt page) {
        return page.isPresent() ? Integer.toString(page.getAsInt()) : node.placeholderText();
    }

    /** Carries the prepared transient paragraph from prepare to emit. */
    private record PreparedPageReference(PreparedNode<ParagraphNode> paragraph) implements PreparedNodeLayout {
    }
}
