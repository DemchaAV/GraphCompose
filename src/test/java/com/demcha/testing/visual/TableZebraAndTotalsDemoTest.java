package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.testing.VisualTestOutputs;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders Phase D.2 demos for zebra rows + totals row to PDF artefacts
 * under {@code target/visual-tests/table-zebra-totals/}.
 *
 * @author Artem Demchyshyn
 */
class TableZebraAndTotalsDemoTest {

    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor RULE = DocumentColor.rgb(180, 188, 200);
    private static final DocumentColor PAPER = DocumentColor.rgb(252, 248, 240);
    private static final DocumentColor ZEBRA_ODD = DocumentColor.rgb(244, 247, 252);
    private static final DocumentColor ZEBRA_EVEN = DocumentColor.WHITE;
    private static final DocumentColor HEADER_FILL = DocumentColor.rgb(20, 60, 75);
    private static final DocumentColor TOTAL_FILL = DocumentColor.rgb(232, 220, 180);

    @Test
    void zebraInvoiceRendersSuccessfully() throws Exception {
        Path output = VisualTestOutputs.preparePdf("zebra-invoice", "table-zebra-totals");
        try (DocumentSession document = GraphCompose.document()
                .pageSize(420, 280)
                .pageBackground(PAPER)
                .margin(DocumentInsets.of(20))
                .create()) {

            DocumentTableStyle bordered = DocumentTableStyle.builder()
                    .stroke(DocumentStroke.of(RULE, 0.6))
                    .padding(DocumentInsets.of(6))
                    .build();
            DocumentTableStyle headerStyle = DocumentTableStyle.builder()
                    .fillColor(HEADER_FILL)
                    .stroke(DocumentStroke.of(RULE, 0.6))
                    .padding(DocumentInsets.of(7))
                    .textStyle(DocumentTextStyle.builder()
                            .decoration(DocumentTextDecoration.BOLD)
                            .color(DocumentColor.WHITE)
                            .build())
                    .build();
            DocumentTableStyle totalStyle = DocumentTableStyle.builder()
                    .fillColor(TOTAL_FILL)
                    .stroke(DocumentStroke.of(RULE, 0.6))
                    .padding(DocumentInsets.of(7))
                    .textStyle(DocumentTextStyle.builder()
                            .decoration(DocumentTextDecoration.BOLD)
                            .color(INK)
                            .build())
                    .build();

            document.dsl().pageFlow()
                    .name("ZebraInvoiceShowcase")
                    .addTable(table -> table
                            .name("InvoiceTable")
                            .columns(
                                    DocumentTableColumn.auto(),
                                    DocumentTableColumn.auto(),
                                    DocumentTableColumn.auto())
                            .defaultCellStyle(bordered)
                            .headerRow("Item", "Qty", "Amount")
                            .row("Apples",       "12",  "$24.00")
                            .row("Pears",        "6",   "$18.00")
                            .row("Strawberries", "20",  "$40.00")
                            .row("Mangoes",      "4",   "$16.00")
                            .totalRow(totalStyle, "Total", "42",  "$98.00")
                            .headerStyle(headerStyle)
                            .zebra(ZEBRA_ODD, ZEBRA_EVEN))
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
