package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.TableBuilder;
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
 * Renders Phase D.3 demos for repeated header on page break to PDF
 * artefacts under {@code target/visual-tests/table-repeat-header/}.
 *
 * @author Artem Demchyshyn
 */
class TableRepeatHeaderDemoTest {

    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor RULE = DocumentColor.rgb(180, 188, 200);
    private static final DocumentColor PAPER = DocumentColor.rgb(252, 248, 240);
    private static final DocumentColor ZEBRA_ODD = DocumentColor.rgb(244, 247, 252);
    private static final DocumentColor ZEBRA_EVEN = DocumentColor.WHITE;
    private static final DocumentColor HEADER_FILL = DocumentColor.rgb(20, 60, 75);

    @Test
    void longTableWithRepeatedHeaderPaginatesAcrossSeveralPages() throws Exception {
        Path output = VisualTestOutputs.preparePdf("long-invoice", "table-repeat-header");
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

            document.dsl().pageFlow()
                    .name("LongInvoiceShowcase")
                    .addTable(table -> {
                        TableBuilder configured = table
                                .name("LongInvoice")
                                .columns(
                                        DocumentTableColumn.auto(),
                                        DocumentTableColumn.auto(),
                                        DocumentTableColumn.auto())
                                .defaultCellStyle(bordered)
                                .headerRow("Item", "Qty", "Amount")
                                .headerStyle(headerStyle)
                                .repeatHeader()
                                .zebra(ZEBRA_ODD, ZEBRA_EVEN);
                        for (int i = 1; i <= 50; i++) {
                            configured.row(
                                    "Line item " + i,
                                    String.valueOf(i),
                                    String.format("$%d.00", i * 3));
                        }
                    })
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
