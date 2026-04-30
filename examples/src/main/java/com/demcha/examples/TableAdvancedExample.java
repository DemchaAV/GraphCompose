package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for the v1.5 Phase D table features:
 * {@code colSpan} + {@code rowSpan}, the {@code zebra} row alternation,
 * the {@code totalRow} shortcut, and {@code repeatHeader} pagination.
 * The output combines every feature on one document so the PDF tells
 * a coherent story instead of fragmenting into one demo per file.
 *
 * @author Artem Demchyshyn
 */
public final class TableAdvancedExample {
    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(102, 106, 118);
    private static final DocumentColor RULE = DocumentColor.rgb(180, 188, 200);
    private static final DocumentColor HEADER_FILL = DocumentColor.rgb(20, 60, 75);
    private static final DocumentColor TOTAL_FILL = DocumentColor.rgb(232, 220, 180);
    private static final DocumentColor ZEBRA_ODD = DocumentColor.rgb(244, 247, 252);
    private static final DocumentColor ZEBRA_EVEN = DocumentColor.WHITE;
    private static final DocumentColor MERGED_FILL = DocumentColor.rgb(220, 234, 240);

    private TableAdvancedExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("table-advanced.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(THEME.pageBackground())
                .margin(28, 28, 28, 28)
                .create()) {

            DocumentTableStyle bordered = DocumentTableStyle.builder()
                    .stroke(DocumentStroke.of(RULE, 0.6))
                    .padding(DocumentInsets.of(7))
                    .build();
            DocumentTableStyle headerStyle = DocumentTableStyle.builder()
                    .fillColor(HEADER_FILL)
                    .stroke(DocumentStroke.of(RULE, 0.6))
                    .padding(DocumentInsets.of(8))
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FontName.HELVETICA_BOLD)
                            .decoration(DocumentTextDecoration.BOLD)
                            .color(DocumentColor.WHITE)
                            .build())
                    .build();
            DocumentTableStyle totalStyle = DocumentTableStyle.builder()
                    .fillColor(TOTAL_FILL)
                    .stroke(DocumentStroke.of(RULE, 0.6))
                    .padding(DocumentInsets.of(8))
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FontName.HELVETICA_BOLD)
                            .decoration(DocumentTextDecoration.BOLD)
                            .color(INK)
                            .build())
                    .build();
            DocumentTableStyle mergedNote = DocumentTableStyle.builder()
                    .fillColor(MERGED_FILL)
                    .stroke(DocumentStroke.of(RULE, 0.6))
                    .padding(DocumentInsets.of(8))
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FontName.HELVETICA)
                            .size(9)
                            .color(INK)
                            .build())
                    .build();

            document.pageFlow()
                    .name("TableAdvancedShowcase")
                    .spacing(14)
                    .addSection("Hero", section -> section
                            .softPanel(THEME.palette().surfaceMuted(), 10, 14)
                            .accentLeft(DocumentColor.rgb(196, 153, 76), 4)
                            .spacing(6)
                            .addParagraph(p -> p
                                    .text("Advanced tables")
                                    .textStyle(THEME.text().h1())
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Row span, zebra rows, totals row, and a header that ")
                                    .accent("repeats on every continuation page", DocumentColor.rgb(196, 153, 76))
                                    .plain(".")))
                    .addSection("RowSpanCallout", section -> section
                            .spacing(8)
                            .addParagraph(p -> p
                                    .text("Row span")
                                    .textStyle(THEME.text().h2())
                                    .margin(DocumentInsets.zero()))
                            .addTable(table -> table
                                    .name("RowSpanTable")
                                    .columns(
                                            DocumentTableColumn.auto(),
                                            DocumentTableColumn.auto(),
                                            DocumentTableColumn.auto())
                                    .defaultCellStyle(bordered)
                                    .rowCells(
                                            DocumentTableCell.text("Q1"),
                                            DocumentTableCell.text("Quarterly note\nspans the next two rows so the\nsidebar text breathes")
                                                    .rowSpan(3)
                                                    .withStyle(mergedNote),
                                            DocumentTableCell.text("$1,200"))
                                    .rowCells(
                                            DocumentTableCell.text("Q2"),
                                            DocumentTableCell.text("$1,450"))
                                    .rowCells(
                                            DocumentTableCell.text("Q3"),
                                            DocumentTableCell.text("$1,710")))
                            .addParagraph(p -> p
                                    .text("Authors only specify cells that aren't covered by a prior row's span.")
                                    .textStyle(caption())
                                    .margin(DocumentInsets.zero())))
                    .addSection("LongInvoice", section -> section
                            .spacing(8)
                            .addParagraph(p -> p
                                    .text("Repeated header + zebra + totals")
                                    .textStyle(THEME.text().h2())
                                    .margin(DocumentInsets.zero()))
                            .addTable(table -> {
                                TableBuilder configured = table
                                        .name("AdvancedInvoice")
                                        .columns(
                                                DocumentTableColumn.auto(),
                                                DocumentTableColumn.auto(),
                                                DocumentTableColumn.auto())
                                        .defaultCellStyle(bordered)
                                        .headerRow("Item", "Qty", "Amount")
                                        .headerStyle(headerStyle)
                                        .repeatHeader()
                                        .zebra(ZEBRA_ODD, ZEBRA_EVEN);
                                int totalQty = 0;
                                int totalAmount = 0;
                                for (int i = 1; i <= 36; i++) {
                                    int qty = (i % 6) + 2;          // 2..7
                                    int amount = qty * 12;          // arbitrary
                                    totalQty += qty;
                                    totalAmount += amount;
                                    configured.row(
                                            String.format("Line item %02d", i),
                                            String.valueOf(qty),
                                            String.format("$%d.00", amount));
                                }
                                configured.totalRow(totalStyle,
                                        "Total",
                                        String.valueOf(totalQty),
                                        String.format("$%d.00", totalAmount));
                            })
                            .addParagraph(p -> p
                                    .text("Long enough to span more than one page — the header re-emits on every continuation.")
                                    .textStyle(caption())
                                    .margin(DocumentInsets.zero())))
                    .build();

            document.buildPdf();
        }

        return outputFile;
    }

    private static DocumentTextStyle caption() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(9)
                .color(MUTED)
                .build();
    }
}
