package com.demcha.compose.document.layout;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * A row nested in a fixed rectangle keeps its horizontal band.
 *
 * <p>Fixed rectangles — a {@link LayerStackNode} layer, a composed
 * {@code TableNode} cell — are laid out by a walk that seats children with a
 * vertical cursor. That is right for a section or a container and wrong for a
 * row, whose children share one band and split its width. When the walk had no
 * horizontal branch, a nested row stacked its children downwards; because the
 * band was measured as one row tall, everything after the first child also
 * spilled out of the rectangle. Nothing threw and nothing was missing from the
 * page, so only geometry catches it — these cases pin the nested row's
 * fragments against the same row laid out at page level.</p>
 */
class RowInFixedSlotLayoutTest {

    private static final double TOLERANCE = 0.01;

    private static ParagraphNode paragraph(String name, String text) {
        return new ParagraphNode(name, text, DocumentTextStyle.DEFAULT, TextAlign.LEFT, 0.0,
                DocumentInsets.zero(), DocumentInsets.zero());
    }

    private static RowNode twoColumnRow(String name) {
        return new RowNode(name,
                List.of(paragraph("Left", "LEFT-TEXT"), paragraph("Right", "RIGHT-TEXT")),
                List.of(1.0, 1.0), 8.0,
                DocumentInsets.zero(), DocumentInsets.zero(),
                null, null, DocumentCornerRadius.ZERO);
    }

    private static List<PlacedFragment> paragraphFragments(DocumentNode root) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(root);
            return session.layoutGraph().fragments().stream()
                    .filter(fragment -> fragment.payload() instanceof ParagraphFragmentPayload)
                    .toList();
        }
    }

    @Test
    void rowInsideAStackLayerSeatsChildrenExactlyLikeAPageLevelRow() throws Exception {
        List<PlacedFragment> pageLevel = paragraphFragments(twoColumnRow("PageRow"));
        List<PlacedFragment> inLayer = paragraphFragments(new LayerStackNode(
                "Stack",
                List.of(new LayerStackNode.Layer(twoColumnRow("LayerRow"))),
                DocumentInsets.zero(), DocumentInsets.zero()));

        assertThat(pageLevel).hasSize(2);
        assertThat(inLayer).hasSize(2);
        for (int index = 0; index < 2; index++) {
            assertThat(inLayer.get(index).x())
                    .describedAs("child %d must sit at the same x as at page level", index)
                    .isCloseTo(pageLevel.get(index).x(), within(TOLERANCE));
            assertThat(inLayer.get(index).y())
                    .describedAs("child %d must sit at the same y as at page level", index)
                    .isCloseTo(pageLevel.get(index).y(), within(TOLERANCE));
        }
    }

    @Test
    void rowInsideAComposedTableCellKeepsItsChildrenOnOneBand() throws Exception {
        TableNode table = new TableNode(
                "RowCellTable",
                List.of(DocumentTableColumn.fixed(260), DocumentTableColumn.fixed(80)),
                List.of(List.of(
                        DocumentTableCell.node(twoColumnRow("CellRow")),
                        DocumentTableCell.text("Neighbour"))),
                DocumentTableStyle.empty(),
                340.0,
                DocumentInsets.zero(),
                DocumentInsets.zero());

        List<PlacedFragment> fragments = paragraphFragments(table);

        assertThat(fragments).hasSize(2);
        PlacedFragment left = fragments.get(0);
        PlacedFragment right = fragments.get(1);
        assertThat(right.y())
                .describedAs("a row in a cell shares one band, so both children sit at the same y")
                .isCloseTo(left.y(), within(TOLERANCE));
        assertThat(right.x())
                .describedAs("a row in a cell splits the cell width, so the second child sits to the right")
                .isGreaterThan(left.x());
    }

    @Test
    void aRowNestedInsideAnotherRowIsRejectedInsideAFixedRectangleToo() {
        RowNode inner = new RowNode("Inner",
                List.of(paragraph("InnerLeft", "I-A"), paragraph("InnerRight", "I-B")),
                List.of(1.0, 1.0), 4.0,
                DocumentInsets.zero(), DocumentInsets.zero(),
                null, null, DocumentCornerRadius.ZERO);
        RowNode outer = new RowNode("Outer",
                List.of(inner, paragraph("OuterRight", "O-B")),
                List.of(1.0, 1.0), 4.0,
                DocumentInsets.zero(), DocumentInsets.zero(),
                null, null, DocumentCornerRadius.ZERO);

        assertThatThrownBy(() -> paragraphFragments(new LayerStackNode(
                "Stack",
                List.of(new LayerStackNode.Layer(outer)),
                DocumentInsets.zero(), DocumentInsets.zero())))
                .describedAs("a fixed-rectangle row must reject a nested horizontal row "
                             + "with the same diagnostic the page-level row band gives")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nested horizontal row");
    }

    @Test
    void aRowCellDoesNotSpillBelowItsTableRow() throws Exception {
        TableNode table = new TableNode(
                "RowCellBoundsTable",
                List.of(DocumentTableColumn.fixed(260), DocumentTableColumn.fixed(80)),
                List.of(List.of(
                        DocumentTableCell.node(twoColumnRow("CellRow")),
                        DocumentTableCell.text("Neighbour"))),
                DocumentTableStyle.empty(),
                340.0,
                DocumentInsets.zero(),
                DocumentInsets.zero());

        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(table);

            LayoutGraph graph = session.layoutGraph();
            PlacedNode tableNode = graph.nodes().stream()
                    .filter(node -> "RowCellBoundsTable".equals(node.semanticName()))
                    .findFirst()
                    .orElseThrow();
            double tableBottom = tableNode.placementY();

            List<PlacedFragment> fragments = graph.fragments().stream()
                    .filter(fragment -> fragment.payload() instanceof ParagraphFragmentPayload)
                    .toList();
            assertThat(fragments).hasSize(2);
            for (PlacedFragment fragment : fragments) {
                assertThat(fragment.y())
                        .describedAs("a row child must stay inside the table it was composed into")
                        .isGreaterThanOrEqualTo(tableBottom - TOLERANCE);
            }
        }
    }
}
