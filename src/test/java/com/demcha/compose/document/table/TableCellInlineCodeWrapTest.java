package com.demcha.compose.document.table;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression for the reported "long inline-code chip overflows its table column":
 * a composed cell holding a single over-wide {@code inlineCode(...)} coordinate in a
 * narrow fixed column must break the chip <em>inside</em> the cell (fill intact per
 * fragment), never draw past the cell box, and never throw.
 */
class TableCellInlineCodeWrapTest {

    /** Fixed column widths: a deliberately narrow middle column holds the chip. */
    private static final double MIDDLE_CELL_WIDTH = 60.0;
    private static final String COORDINATE = "org.junit.jupiter:junit-jupiter:5.10.2";

    private static TableNode narrowCodeTable(DocumentSession session) {
        DocumentTableCell left = DocumentTableCell.text("L");
        DocumentTableCell mid = DocumentTableCell.node(
                session.dsl().paragraph().inlineCode(COORDINATE).build());
        DocumentTableCell right = DocumentTableCell.text("R");
        return new TableNode(
                "CodeTable",
                List.of(DocumentTableColumn.fixed(40),
                        DocumentTableColumn.fixed(MIDDLE_CELL_WIDTH),
                        DocumentTableColumn.fixed(40)),
                List.of(List.of(left, mid, right)),
                DocumentTableStyle.empty(),
                140.0,
                DocumentInsets.zero(),
                DocumentInsets.zero());
    }

    @Test
    void narrowCellInlineCodeBreaksWithinCell() throws Exception {
        byte[] pdf;
        List<ParagraphLine> chipLines;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(220, 200)
                .margin(DocumentInsets.of(10))
                .create()) {
            session.add(narrowCodeTable(session));

            LayoutGraph graph = session.layoutGraph();
            chipLines = graph.fragments().stream()
                    .map(PlacedFragment::payload)
                    .filter(ParagraphFragmentPayload.class::isInstance)
                    .map(ParagraphFragmentPayload.class::cast)
                    .flatMap(payload -> payload.lines().stream())
                    .filter(l -> l.spans().stream()
                            .anyMatch(s -> s instanceof ParagraphTextSpan ts && ts.background() != null))
                    .toList();
            pdf = session.toPdfBytes();
        }

        assertThat(chipLines).as("the over-wide chip breaks to >= 2 fragments in the cell")
                .hasSizeGreaterThanOrEqualTo(2);
        for (ParagraphLine line : chipLines) {
            ParagraphTextSpan fragment = (ParagraphTextSpan) line.spans().stream()
                    .filter(s -> s instanceof ParagraphTextSpan ts && ts.background() != null)
                    .findFirst().orElseThrow();
            assertThat(fragment.width()).as("no chip fragment draws past the cell's inner width")
                    .isLessThanOrEqualTo(MIDDLE_CELL_WIDTH + 0.5);
            assertThat(fragment.background()).as("each fragment keeps its fill").isNotNull();
        }

        // Human-inspectable artifact: the chip wraps inside the middle column.
        Path output = VisualTestOutputs.preparePdf("inline_code_wrap_narrow_cell", "tables");
        Files.write(output, pdf);
        assertThat(new String(pdf, 0, 8, StandardCharsets.US_ASCII)).startsWith("%PDF-");

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(document).replaceAll("\\s+", "");
            assertThat(text).contains(COORDINATE).doesNotContain("?");
        }
    }

    @Test
    void narrowCellInlineCodeLayoutSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(220, 200)
                .margin(DocumentInsets.of(10))
                .create()) {
            session.add(narrowCodeTable(session));
            LayoutSnapshotAssertions.assertMatches(session, "tables/inline_code_wrap_narrow_cell");
        }
    }
}
