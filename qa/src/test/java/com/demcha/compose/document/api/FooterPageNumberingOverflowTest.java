package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code Page {page} of {pages}} on an invoice-shaped document whose row count
 * decides the page count.
 *
 * <p>{@code {pages}} is a document-wide total, so it can only be right once
 * pagination has settled — a footer stamped from a first-pass estimate would
 * still read plausibly ("Page 1 of 3") while being wrong. The cases below drive
 * the page count purely through table overflow and then read every page's
 * footer out of the rendered PDF, so both halves of the token pair are pinned
 * against the page count the backend actually produced.</p>
 */
class FooterPageNumberingOverflowTest {

    @ParameterizedTest(name = "{0} invoice rows number every page of {1}")
    @CsvSource({
            "1, 1",
            "12, 2",
            "40, 5"
    })
    void tableOverflowNumbersEveryPage(int rowCount, int expectedPages) throws Exception {
        List<List<DocumentTableCell>> rows = new ArrayList<>(rowCount);
        for (int index = 1; index <= rowCount; index++) {
            rows.add(List.of(
                    DocumentTableCell.text("Line item " + index),
                    DocumentTableCell.text("$" + (index * 10))));
        }

        byte[] pdfBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 220)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.footer(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.FOOTER)
                    .centerText("Page {page} of {pages}")
                    .build());
            session.add(new TableNode(
                    "Invoice",
                    List.of(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(140)),
                    rows,
                    DocumentTableStyle.empty(),
                    340.0,
                    DocumentInsets.zero(),
                    DocumentInsets.zero()));
            pdfBytes = session.toPdfBytes();
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages())
                    .describedAs("%d rows must overflow onto %d pages", rowCount, expectedPages)
                    .isEqualTo(expectedPages);

            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= expectedPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                assertThat(stripper.getText(document))
                        .describedAs("page %d of a %d-page document", page, expectedPages)
                        .contains("Page " + page + " of " + expectedPages);
            }
        }
    }

    @ParameterizedTest(name = "a repeated header row does not disturb the count over {0} rows")
    @CsvSource({
            "12, 2",
            "40, 6"
    })
    void repeatedHeaderRowDoesNotDisturbTheCount(int bodyRowCount, int expectedPages) throws Exception {
        List<List<DocumentTableCell>> rows = new ArrayList<>(bodyRowCount + 1);
        rows.add(List.of(DocumentTableCell.text("Description"), DocumentTableCell.text("Amount")));
        for (int index = 1; index <= bodyRowCount; index++) {
            rows.add(List.of(
                    DocumentTableCell.text("Line item " + index),
                    DocumentTableCell.text("$" + (index * 10))));
        }

        byte[] pdfBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 220)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.chrome().footer(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.FOOTER)
                    .centerText("Page {page} of {pages}")
                    .build());
            session.dsl()
                    .pageFlow()
                    .name("InvoiceFlow")
                    .addTable(table -> {
                        table.name("Invoice")
                                .columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(140));
                        rows.forEach(table::rowCells);
                        table.repeatHeader();
                    })
                    .build();
            pdfBytes = session.toPdfBytes();
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isEqualTo(expectedPages);

            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= expectedPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                assertThat(text)
                        .describedAs("page %d of a %d-page document with a repeated header", page, expectedPages)
                        .contains("Page " + page + " of " + expectedPages);
                assertThat(text)
                        .describedAs("the repeated header row must reach page %d", page)
                        .contains("Description");
            }
        }
    }
}
