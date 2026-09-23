package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.style.DocumentColor;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two tables in a row stay two tables in Word.
 *
 * <p>An editor joins two tables with nothing between them into one, laying the second one's
 * rows on the first one's column grid — measured in LibreOffice, a zebra table followed by a
 * narrower one came out at half its width, its text broken letter by letter. A tenth-of-a-point
 * paragraph now keeps them apart.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxAdjacentTablesTest {

    @Test
    void twoTablesInARowAreKeptApartByAParagraphATenthOfAPointTall() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addTable(t -> t.autoColumns(2).row("Item", "Amount"))
                .addTable(t -> t.autoColumns(3).row("A", "B", "C")))) {
            List<IBodyElement> body = document.getBodyElements();

            assertThat(body).hasSize(3);
            assertThat(body.get(0)).isInstanceOf(XWPFTable.class);
            assertThat(body.get(1)).isInstanceOf(XWPFParagraph.class);
            assertThat(body.get(2)).isInstanceOf(XWPFTable.class);
            XWPFParagraph separator = (XWPFParagraph) body.get(1);
            assertThat(separator.getText()).isEmpty();
            assertThat(DocxTwips.of(separator.getCTP().getPPr().getSpacing().getLine()))
                    .as("a tenth of a point, so it keeps the tables apart without opening a line between them")
                    .isEqualTo(2L);
        }
    }

    @Test
    void twoRowsInARowAreTwoTablesToo() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addRow(r -> r.addParagraph(p -> p.text("Name")).addParagraph(p -> p.text("Role")))
                .addRow(r -> r.addParagraph(p -> p.text("Ada")).addParagraph(p -> p.text("Engineer"))))) {
            List<IBodyElement> body = document.getBodyElements();

            assertThat(body).extracting(element -> element.getClass().getSimpleName())
                    .containsExactly("XWPFTable", "XWPFParagraph", "XWPFTable");
        }
    }

    @Test
    void theGapBetweenThemIsTheLayoutsLessTheSeparatorsPoint() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addTable(t -> t.autoColumns(1).row("Above"))
                .addTable(t -> t.autoColumns(1).row("Below").margin(
                        com.demcha.compose.document.style.DocumentInsets.top(12))))) {
            XWPFParagraph separator = (XWPFParagraph) document.getBodyElements().get(1);

            assertThat(DocxTwips.of(separator.getCTP().getPPr().getSpacing().getBefore()))
                    .as("12pt above the second table: 11.9pt of spacing and the separator's own tenth of a point")
                    .isEqualTo(238L);
        }
    }

    @Test
    void aParagraphAlreadyBetweenThemIsEnough() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addTable(t -> t.autoColumns(1).row("Above"))
                .addParagraph(p -> p.text("Between"))
                .addTable(t -> t.autoColumns(1).row("Below")))) {
            assertThat(document.getBodyElements()).hasSize(3);
            assertThat(document.getParagraphs()).extracting(XWPFParagraph::getText).containsExactly("Between");
        }
    }

    @Test
    void spaceOwedBelowATableGoesBelowItNotAboveIt() throws Exception {
        // A card ending in a table, followed by a page break: the card's bottom padding is
        // owed after its table, and used to be paid on the last paragraph written — the
        // card's title, above the table, opening a gap between the title and the table.
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addSection(s -> s.padding(com.demcha.compose.document.style.DocumentInsets.of(10))
                        .addParagraph(p -> p.text("Title"))
                        .addTable(t -> t.autoColumns(1).row("Last")))
                .addPageBreak(b -> { })
                .addParagraph(p -> p.text("Next page")))) {
            XWPFParagraph title = document.getParagraphs().get(0);
            var spacing = title.getCTP().getPPr().getSpacing();

            assertThat(spacing == null || !spacing.isSetAfter())
                    .as("the card's bottom padding is below its table, not between its title and the table")
                    .isTrue();
        }
    }

    @Test
    void theSeparatorInsideAPanelKeepsTheBandUnbroken() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addSection(s -> s.fillColor(DocumentColor.rgb(238, 243, 249))
                        .addTable(t -> t.autoColumns(1).row("Above"))
                        .addTable(t -> t.autoColumns(1).row("Below"))))) {
            XWPFParagraph separator = (XWPFParagraph) document.getBodyElements().get(1);

            assertThat(separator.getCTP().getPPr().isSetShd()).isTrue();
        }
    }
}
