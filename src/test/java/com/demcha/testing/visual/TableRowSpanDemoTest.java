package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.testing.VisualTestOutputs;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders Phase D.1 row-span demos to PDF artefacts under
 * {@code target/visual-tests/table-rowspan/}. Reviewers can open the
 * files to verify the merged-cell behaviour visually.
 *
 * @author Artem Demchyshyn
 */
class TableRowSpanDemoTest {

    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor RULE = DocumentColor.rgb(180, 188, 200);
    private static final DocumentColor PAPER = DocumentColor.rgb(252, 248, 240);
    private static final DocumentColor TEAL_FILL = DocumentColor.rgb(220, 234, 240);

    @Test
    void twoByTwoMergedCellRendersSuccessfully() throws Exception {
        Path output = VisualTestOutputs.preparePdf("two-by-two-merged", "table-rowspan");
        try (DocumentSession document = GraphCompose.document()
                .pageSize(360, 220)
                .pageBackground(PAPER)
                .margin(DocumentInsets.of(20))
                .create()) {

            DocumentTableStyle bordered = DocumentTableStyle.builder()
                    .stroke(DocumentStroke.of(RULE, 0.6))
                    .padding(DocumentInsets.of(6))
                    .build();
            DocumentTableStyle merged = DocumentTableStyle.builder()
                    .stroke(DocumentStroke.of(RULE, 0.6))
                    .padding(DocumentInsets.of(8))
                    .fillColor(TEAL_FILL)
                    .build();

            document.dsl().pageFlow()
                    .name("RowSpanShowcase")
                    .addTable(table -> table
                            .name("MergedTwoByTwo")
                            .columns(
                                    DocumentTableColumn.auto(),
                                    DocumentTableColumn.auto(),
                                    DocumentTableColumn.auto())
                            .defaultCellStyle(bordered)
                            .rowCells(
                                    DocumentTableCell.text("Merged 2x2").colSpan(2).rowSpan(2).withStyle(merged),
                                    DocumentTableCell.text("Top right"))
                            .rowCells(
                                    DocumentTableCell.text("Bottom right")))
                    .build();

            document.buildPdf(output);
        }
        assertPdf(output);
    }

    @Test
    void middleColumnRowSpanRendersSuccessfully() throws Exception {
        Path output = VisualTestOutputs.preparePdf("middle-column-rowspan", "table-rowspan");
        try (DocumentSession document = GraphCompose.document()
                .pageSize(420, 240)
                .pageBackground(PAPER)
                .margin(DocumentInsets.of(20))
                .create()) {

            DocumentTableStyle bordered = DocumentTableStyle.builder()
                    .stroke(DocumentStroke.of(RULE, 0.6))
                    .padding(DocumentInsets.of(6))
                    .build();
            DocumentTableStyle tall = DocumentTableStyle.builder()
                    .stroke(DocumentStroke.of(RULE, 0.6))
                    .padding(DocumentInsets.of(8))
                    .fillColor(TEAL_FILL)
                    .textStyle(DocumentTextStyle.builder().size(11).color(INK).build())
                    .build();

            document.dsl().pageFlow()
                    .name("MiddleSpanShowcase")
                    .addTable(table -> table
                            .name("MiddleColumnSpan")
                            .columns(
                                    DocumentTableColumn.auto(),
                                    DocumentTableColumn.auto(),
                                    DocumentTableColumn.auto())
                            .defaultCellStyle(bordered)
                            .rowCells(
                                    DocumentTableCell.text("A0"),
                                    DocumentTableCell.text("Tall middle\n(spans 3 rows)").rowSpan(3).withStyle(tall),
                                    DocumentTableCell.text("C0"))
                            .rowCells(
                                    DocumentTableCell.text("A1"),
                                    DocumentTableCell.text("C1"))
                            .rowCells(
                                    DocumentTableCell.text("A2"),
                                    DocumentTableCell.text("C2")))
                    .build();

            document.buildPdf(output);
        }
        assertPdf(output);
    }

    private static void assertPdf(Path output) throws Exception {
        byte[] bytes = Files.readAllBytes(output);
        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
                .isEqualTo("%PDF-");
    }
}
