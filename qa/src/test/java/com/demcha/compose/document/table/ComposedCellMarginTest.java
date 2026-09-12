package com.demcha.compose.document.table;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.TableRowFragmentPayload;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A margin on the node handed to {@link DocumentTableCell#node} is honoured,
 * and honoured in both passes.
 *
 * <p>The cell reserves a box for its composed content, then the fragment pass
 * lays that content out inside the box. Those two have to start from the same
 * geometry. They did not: the cell was measured at its full inner width with
 * the node's margin left in, and the fixed-box walk that places a composite
 * subtracts that margin itself — so the content re-wrapped one margin narrower
 * than the row was sized for and painted below the table, while its own box
 * shifted right and overhung the next column. On the vertical axis the cell
 * height ignored the margin outright while placement applied {@code margin.top},
 * dropping the content through the cell floor. A margin on a leaf composed
 * child was discarded entirely.</p>
 *
 * <p>Every assertion here is containment: the content of a cell stays inside
 * the table that reserved room for it. That is the property a margin must not
 * be able to break, and it holds whatever the font metrics are.</p>
 */
class ComposedCellMarginTest {

    private static final double TOLERANCE = 0.5;

    /** Wide enough that the text wraps, so a narrower placement costs a line. */
    private static final String BODY =
            "A fixed width pins the horizontal axis and leaves the height to the content.";

    private static ParagraphNode paragraph(String name, DocumentInsets margin) {
        return new ParagraphNode(name, BODY, DocumentTextStyle.DEFAULT, TextAlign.LEFT, 0.0,
                DocumentInsets.zero(), margin);
    }

    /** A composite composed child — the branch that goes through the fixed-box walk. */
    private static DocumentNode section(DocumentInsets margin) {
        return new SectionNode("Card", List.of(paragraph("Body", DocumentInsets.zero())),
                0.0, DocumentInsets.zero(), margin,
                null, null, DocumentCornerRadius.ZERO, DocumentBorders.NONE, false);
    }

    private static TableNode tableWith(DocumentNode composed) {
        return new TableNode(
                "MarginTable",
                List.of(DocumentTableColumn.fixed(260)),
                List.of(List.of(DocumentTableCell.node(composed))),
                DocumentTableStyle.empty(),
                260.0,
                DocumentInsets.zero(),
                DocumentInsets.zero());
    }

    private static LayoutGraph layout(DocumentNode composed) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(tableWith(composed));
            return session.layoutGraph();
        }
    }

    private static PlacedFragment text(LayoutGraph graph) {
        List<PlacedFragment> paragraphs = graph.fragments().stream()
                .filter(f -> f.payload() instanceof ParagraphFragmentPayload)
                .toList();
        assertThat(paragraphs).as("the composed cell contributes paragraph fragments").isNotEmpty();
        return paragraphs.get(paragraphs.size() - 1);
    }

    /** The table row's own box — the room the measure pass reserved. */
    private static PlacedFragment row(LayoutGraph graph) {
        List<PlacedFragment> rows = graph.fragments().stream()
                .filter(f -> f.payload() instanceof TableRowFragmentPayload)
                .toList();
        assertThat(rows).hasSize(1);
        return rows.get(0);
    }

    @Test
    void aHorizontalMarginOnComposedContentDoesNotPushItBelowItsOwnTable() {
        LayoutGraph graph = layout(section(new DocumentInsets(0, 20, 0, 20)));

        assertThat(text(graph).y())
                .describedAs("content measured at one width and placed at another outgrows its row")
                .isGreaterThanOrEqualTo(row(graph).y() - TOLERANCE);
    }

    @Test
    void aVerticalMarginOnComposedContentIsReservedByTheCell() {
        LayoutGraph graph = layout(section(new DocumentInsets(20, 0, 20, 0)));

        // Placement applies margin.top; the cell height has to have counted it,
        // or the content drops straight through the floor of its own table.
        assertThat(text(graph).y())
                .describedAs("a vertical margin the row never reserved drops the content out of it")
                .isGreaterThanOrEqualTo(row(graph).y() - TOLERANCE);
    }

    @Test
    void aMarginOnALeafComposedChildIsNotDiscarded() {
        // A leaf goes straight to its own emitFragments rather than the
        // fixed-box walk, and used to be placed as if it had no margin at all.
        double withMargin = text(layout(paragraph("Leaf", new DocumentInsets(0, 0, 0, 40)))).x();
        double withoutMargin = text(layout(paragraph("Leaf", DocumentInsets.zero()))).x();

        assertThat(withMargin)
                .describedAs("a 40pt left margin must move a leaf composed child right")
                .isCloseTo(withoutMargin + 40.0, within(TOLERANCE));
    }
}
