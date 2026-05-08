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
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Phase B (v1.6) — composed table cell content via
 * {@link DocumentTableCell#node(com.demcha.compose.document.node.DocumentNode)}.
 *
 * <p>Verifies that a {@code DocumentNode} child placed inside a table
 * cell is prepared against the cell's resolved inner width, sizes the
 * row to its measured height, and contributes its own fragments to
 * the layout graph at the cell's absolute position. The renderer
 * dispatches the child's fragments through the standard
 * {@code NodeDefinition} pipeline so any registered child type
 * works inside a cell automatically.</p>
 */
class TableCellComposedContentTest {

    @Test
    void composedCellEmitsChildParagraphFragmentInsideTableLayout() throws Exception {
        DocumentTableCell composedCell = DocumentTableCell.node(
                new ParagraphNode("CellParagraph", "Composed paragraph in a cell.",
                        DocumentTextStyle.DEFAULT, TextAlign.LEFT, 0.0,
                        DocumentInsets.zero(), DocumentInsets.zero()));
        DocumentTableCell plainCell = DocumentTableCell.text("Plain right");

        TableNode table = new TableNode(
                "ComposedTable",
                List.of(DocumentTableColumn.fixed(160), DocumentTableColumn.fixed(160)),
                List.of(List.of(composedCell, plainCell)),
                DocumentTableStyle.empty(),
                400.0,
                DocumentInsets.zero(),
                DocumentInsets.zero());

        try (DocumentSession session = GraphCompose.document()
                .pageSize(440, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(table);

            LayoutGraph graph = session.layoutGraph();

            // Two row-fragments (one per row of plain text) plus one
            // child paragraph fragment for the composed cell.
            List<PlacedFragment> tableRowFragments = graph.fragments().stream()
                    .filter(f -> f.payload() instanceof TableRowFragmentPayload)
                    .toList();
            List<PlacedFragment> paragraphFragments = graph.fragments().stream()
                    .filter(f -> f.payload() instanceof ParagraphFragmentPayload)
                    .toList();

            assertThat(tableRowFragments).hasSize(1);
            assertThat(paragraphFragments).hasSize(1);

            ParagraphFragmentPayload childPayload = (ParagraphFragmentPayload)
                    paragraphFragments.get(0).payload();
            // The paragraph wraps within the cell's inner width, so the
            // full sentence is split across two visual lines. Combine
            // them before checking the rendered text.
            String composed = childPayload.lines().stream()
                    .map(l -> l.text())
                    .reduce("", (a, b) -> a + " " + b)
                    .trim();
            assertThat(composed).contains("Composed paragraph in")
                    .contains("a cell.");
        }
    }

    @Test
    void composedCellHeightDrivesRowHeightWhenChildIsTallerThanText() throws Exception {
        // Multi-line paragraph that wraps inside the cell's width.
        DocumentTableCell composedCell = DocumentTableCell.node(
                new ParagraphNode(
                        "TallParagraph",
                        "Line one of the composed paragraph. "
                                + "Line two extends the height beyond a single text row "
                                + "so the table row grows to accommodate the prepared child.",
                        DocumentTextStyle.DEFAULT,
                        TextAlign.LEFT,
                        2.0,
                        DocumentInsets.zero(),
                        DocumentInsets.zero()));
        DocumentTableCell plainCell = DocumentTableCell.text("Plain");

        TableNode table = new TableNode(
                "TallTable",
                List.of(DocumentTableColumn.fixed(160), DocumentTableColumn.fixed(160)),
                List.of(List.of(composedCell, plainCell)),
                DocumentTableStyle.empty(),
                360.0,
                DocumentInsets.zero(),
                DocumentInsets.zero());

        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 220)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(table);

            LayoutGraph graph = session.layoutGraph();
            PlacedFragment rowFragment = graph.fragments().stream()
                    .filter(f -> f.payload() instanceof TableRowFragmentPayload)
                    .findFirst().orElseThrow();

            // The single composed paragraph wraps to >= 2 visual lines
            // at this width, so the row must be taller than the
            // single-line plain-text minimum.
            double singleLineHeight = 12.0;
            assertThat(rowFragment.height())
                    .as("Composed cell content drives row height")
                    .isGreaterThan(singleLineHeight * 1.5);
        }
    }

    @Test
    void composedCellRendersInsideCellPadding() throws Exception {
        DocumentTableStyle paddedStyle = DocumentTableStyle.builder()
                .padding(new DocumentInsets(6, 8, 6, 8))
                .build();

        DocumentTableCell composedCell = DocumentTableCell.node(
                        new ParagraphNode("InsetText", "Inset",
                        DocumentTextStyle.DEFAULT, TextAlign.LEFT, 0.0,
                        DocumentInsets.zero(), DocumentInsets.zero()))
                .withStyle(paddedStyle);
        DocumentTableCell plainCell = DocumentTableCell.text("Right").withStyle(paddedStyle);

        TableNode table = new TableNode(
                "InsetTable",
                List.of(DocumentTableColumn.fixed(160), DocumentTableColumn.fixed(160)),
                List.of(List.of(composedCell, plainCell)),
                paddedStyle,
                360.0,
                DocumentInsets.zero(),
                DocumentInsets.zero());

        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(table);

            LayoutGraph graph = session.layoutGraph();
            PlacedFragment rowFragment = graph.fragments().stream()
                    .filter(f -> f.payload() instanceof TableRowFragmentPayload)
                    .findFirst().orElseThrow();
            PlacedFragment childFragment = graph.fragments().stream()
                    .filter(f -> f.payload() instanceof ParagraphFragmentPayload)
                    .findFirst().orElseThrow();

            // Child should be placed inside the row's left padding.
            assertThat(childFragment.x())
                    .as("Composed child sits inside the cell's left padding")
                    .isGreaterThan(rowFragment.x() + 7.5)  // padding.left = 8
                    .isLessThan(rowFragment.x() + 200);    // first column width
        }
    }

    @Test
    void composedCellWritesChildTextToPdfOutput() throws Exception {
        DocumentTableCell composedCell = DocumentTableCell.node(
                new ParagraphNode("SignatureLine", "Composed cell text reaches PDF.",
                        DocumentTextStyle.DEFAULT, TextAlign.LEFT, 0.0,
                        DocumentInsets.zero(), DocumentInsets.zero()));
        DocumentTableCell plainCell = DocumentTableCell.text("Plain");

        TableNode table = new TableNode(
                "PdfTable",
                List.of(DocumentTableColumn.fixed(160), DocumentTableColumn.fixed(160)),
                List.of(List.of(composedCell, plainCell)),
                DocumentTableStyle.empty(),
                360.0,
                DocumentInsets.zero(),
                DocumentInsets.zero());

        byte[] pdfBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(table);
            pdfBytes = session.toPdfBytes();
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            String extracted = new PDFTextStripper().getText(document);
            // The composed paragraph wraps to two visual lines inside
            // the cell width, so check the wrapped fragments instead of
            // the original concatenation.
            assertThat(extracted).contains("Composed cell text");
            assertThat(extracted).contains("reaches PDF.");
            assertThat(extracted).contains("Plain");
        }
    }

    @Test
    void plainTextCellsStayBackCompatibleAfterPhaseBChange() throws Exception {
        DocumentTableCell a = DocumentTableCell.text("Alpha");
        DocumentTableCell b = DocumentTableCell.text("Beta");

        TableNode table = new TableNode(
                "BackCompatTable",
                List.of(DocumentTableColumn.fixed(160), DocumentTableColumn.fixed(160)),
                List.of(List.of(a, b)),
                DocumentTableStyle.empty(),
                360.0,
                DocumentInsets.zero(),
                DocumentInsets.zero());

        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(table);

            LayoutGraph graph = session.layoutGraph();

            // Plain-text rows must NOT emit child paragraph fragments —
            // the cells render text inline through the existing
            // PdfTableRowFragmentRenderHandler.
            long paragraphFragmentsCount = graph.fragments().stream()
                    .filter(f -> f.payload() instanceof ParagraphFragmentPayload)
                    .count();
            assertThat(paragraphFragmentsCount).isZero();

            long rowFragmentsCount = graph.fragments().stream()
                    .filter(f -> f.payload() instanceof TableRowFragmentPayload)
                    .count();
            assertThat(rowFragmentsCount).isEqualTo(1);
        }
    }

    @Test
    void composedCellLayoutSnapshot() throws Exception {
        DocumentTableCell composedCell = DocumentTableCell.node(
                new ParagraphNode("CellParagraph", "Composed paragraph in a cell.",
                        DocumentTextStyle.DEFAULT, TextAlign.LEFT, 0.0,
                        DocumentInsets.zero(), DocumentInsets.zero()));
        DocumentTableCell plainCell = DocumentTableCell.text("Plain right");

        TableNode table = new TableNode(
                "SnapshotTable",
                List.of(DocumentTableColumn.fixed(160), DocumentTableColumn.fixed(160)),
                List.of(List.of(composedCell, plainCell)),
                DocumentTableStyle.empty(),
                360.0,
                DocumentInsets.zero(),
                DocumentInsets.zero());

        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(table);

            LayoutSnapshotAssertions.assertMatches(session, "document/table_cell_with_paragraph");
        }
    }
}
