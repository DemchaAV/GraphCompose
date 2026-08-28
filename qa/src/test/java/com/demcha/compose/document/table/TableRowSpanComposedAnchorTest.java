package com.demcha.compose.document.table;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.TableRowFragmentPayload;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.engine.components.content.table.TableCellLayoutStyle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A composed cell that spans rows sits where its anchor says, not at the
 * foot of the span.
 *
 * <p>{@code emitComposedCellFragments} accepted the cell's height and never
 * read it: a composed child was placed at {@code cellLocalY + padding.bottom()}
 * whatever the cell's anchor said. On a single row that is invisible, because
 * the row is usually as tall as its tallest cell. On a cell with
 * {@code rowSpan(2)} the cell's height is the whole span, so a badge sat at the
 * foot of both rows — and {@code TOP_LEFT} did nothing, because no branch read
 * the anchor at all.</p>
 *
 * <p>The assertions are differential on purpose. An absolute y would have to be
 * compared against a row geometry these tests do not otherwise pin down, and
 * would go stale the moment a default font metric moved. What the defect is
 * about is whether the anchor is READ: if {@code TOP_LEFT} and
 * {@code BOTTOM_LEFT} put the child in the same place, it is not.</p>
 *
 * @author Artem Demchyshyn
 */
class TableRowSpanComposedAnchorTest {

    private static final double EPS = 0.5;

    /** A short composed child, so the span is much taller than it is. */
    private static DocumentTableCell badge(DocumentTableStyle style) {
        DocumentTableCell cell = DocumentTableCell.node(
                new ParagraphNode("Badge", "B",
                        DocumentTextStyle.DEFAULT, TextAlign.LEFT, 0.0,
                        DocumentInsets.zero(), DocumentInsets.zero()))
                .rowSpan(2);
        return style == null ? cell : cell.withStyle(style);
    }

    /**
     * Two rows: a short one and a deliberately tall one, so the span has
     * slack for the anchor to place the child within.
     */
    private static TableNode spanningTable(DocumentTableStyle cellStyle,
                                           DocumentTableStyle tableDefault) {
        return new TableNode(
                "SpanTable",
                List.of(DocumentTableColumn.fixed(60), DocumentTableColumn.fixed(200)),
                List.of(
                        List.of(badge(cellStyle), DocumentTableCell.text("TITLE")),
                        List.of(DocumentTableCell.lines(
                                "body", "body", "body", "body", "body", "body"))),
                tableDefault == null ? DocumentTableStyle.empty() : tableDefault,
                280.0,
                DocumentInsets.zero(),
                DocumentInsets.zero());
    }

    /** The composed child's own fragment, which is the thing being placed. */
    private static PlacedFragment badgeFragment(LayoutGraph graph) {
        List<PlacedFragment> paragraphs = graph.fragments().stream()
                .filter(f -> f.payload() instanceof ParagraphFragmentPayload)
                .toList();
        assertThat(paragraphs)
                .as("the composed cell should contribute exactly one paragraph fragment")
                .hasSize(1);
        return paragraphs.get(0);
    }

    /** The top edge of the span, which is the top edge of its first row. */
    private static double spanTop(LayoutGraph graph) {
        List<PlacedFragment> rows = graph.fragments().stream()
                .filter(f -> f.payload() instanceof TableRowFragmentPayload)
                .toList();
        assertThat(rows).hasSize(2);
        // PDF coordinates grow upward, so the first row is the higher one.
        return rows.stream().mapToDouble(f -> f.y() + f.height()).max().orElseThrow();
    }

    private static double badgeY(DocumentTableStyle cellStyle, DocumentTableStyle tableDefault) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(spanningTable(cellStyle, tableDefault));
            return badgeFragment(session.layoutGraph()).y();
        }
    }

    private static DocumentTableStyle anchored(DocumentTableTextAnchor anchor) {
        return DocumentTableStyle.builder().textAnchor(anchor).build();
    }

    @Test
    void topAnchorLiftsAComposedSpanningCellClearOfTheFootOfItsSpan() {
        double top = badgeY(anchored(DocumentTableTextAnchor.TOP_LEFT), null);
        double bottom = badgeY(anchored(DocumentTableTextAnchor.BOTTOM_LEFT), null);

        // The whole defect in one assertion: before the fix these were equal,
        // because the anchor was never read.
        assertThat(top)
                .as("TOP_LEFT must place the child higher than BOTTOM_LEFT does")
                .isGreaterThan(bottom + EPS);
    }

    @Test
    void topAnchorPutsTheChildAtTheTopOfTheSpanRatherThanTheTopOfItsOwnRow() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(spanningTable(anchored(DocumentTableTextAnchor.TOP_LEFT), null));
            LayoutGraph graph = session.layoutGraph();

            // The span's top edge is the cell's OUTER top; the child is placed
            // inside the cell's padding, so the expected position is one top
            // padding down from it. Read from the resolved default rather than
            // written as 4.0, so a change to that default fails as a default
            // change instead of as a mysterious four points.
            double topPadding = TableCellLayoutStyle.DEFAULT.padding().top();

            PlacedFragment badge = badgeFragment(graph);
            assertThat(badge.y() + badge.height())
                    .as("a TOP-anchored child's top edge is the span's top edge, inside the padding")
                    .isCloseTo(spanTop(graph) - topPadding, within(EPS));
        }
    }

    @Test
    void theAnchorIsReadFromTheTableDefaultAsWellAsFromTheCell() {
        // The report this test comes from tried all three declaration sites and
        // saw no difference at any of them. The cell's own style is covered
        // above; this is the table default, which reaches the cell through the
        // same merge.
        double top = badgeY(null, anchored(DocumentTableTextAnchor.TOP_LEFT));
        double bottom = badgeY(null, anchored(DocumentTableTextAnchor.BOTTOM_LEFT));

        assertThat(top)
                .as("a TOP anchor on defaultCellStyle must reach a composed spanning cell")
                .isGreaterThan(bottom + EPS);
    }

    @Test
    void aChildTallerThanItsCellStillStartsAtTheBottomRatherThanBelowIt() {
        // Slack is clamped at zero. Without the clamp an over-tall child would
        // be pushed below its own cell by a negative offset, which is worse
        // than the overflow it is already doing.
        DocumentTableCell tall = DocumentTableCell.node(
                new ParagraphNode("TallBadge",
                        "a rather long composed paragraph that will wrap several times "
                                + "inside a sixty point column and so exceed the row it sits in",
                        DocumentTextStyle.DEFAULT, TextAlign.LEFT, 0.0,
                        DocumentInsets.zero(), DocumentInsets.zero()))
                .withStyle(anchored(DocumentTableTextAnchor.TOP_LEFT));

        TableNode table = new TableNode(
                "OverTall",
                List.of(DocumentTableColumn.fixed(60), DocumentTableColumn.fixed(200)),
                List.of(List.of(tall, DocumentTableCell.text("X"))),
                DocumentTableStyle.empty(),
                280.0,
                DocumentInsets.zero(),
                DocumentInsets.zero());

        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(table);
            LayoutGraph graph = session.layoutGraph();

            List<PlacedFragment> rows = graph.fragments().stream()
                    .filter(f -> f.payload() instanceof TableRowFragmentPayload)
                    .toList();
            assertThat(rows).hasSize(1);

            assertThat(badgeFragment(graph).y())
                    .as("an over-tall child is not pushed below its own row")
                    .isGreaterThanOrEqualTo(rows.get(0).y() - EPS);
        }
    }
}
