package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A table keeps its own margins: its left margin holds it in from the edge it is written from,
 * and its top margin stands above it, in a band's layer as anywhere else.
 *
 * <p>VioletGrid sets each education entry's lines in a table held 51.3pt clear of a badge
 * drawn beside them, and 2.3pt down. Neither margin was written: in Word the lines started
 * under the badge and stood 2.4pt high. A table or a row was never indented by its left
 * margin, anywhere — PaymentsInvoice's bank details stood 36.7pt left of the page's.</p>
 */
class DocxTableMarginsTest {

    private static final double INSET = 51.3;
    private static final double DOWN = 2.3;

    @Test
    void aTablesLeftMarginIndentsIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(500, 600, 20, page -> page
                .addParagraph("Above")
                .add(lines()))) {
            assertThat(indentOf(document.getTables().get(0))).as("the table stands its margin in")
                    .isEqualTo(Math.round(INSET * 20));
        }
    }

    @Test
    void aRowsLeftMarginIndentsIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(500, 600, 20, page -> page
                .addParagraph("Above")
                .addRow(row -> row.name("Split").margin(new DocumentInsets(0, 0, 0, 30))
                        .addSection("Left", left -> left.addParagraph("Left"))
                        .addSection("Right", right -> right.addParagraph("Right"))))) {
            assertThat(indentOf(document.getTables().get(0))).as("the row stands its margin in")
                    .isEqualTo(600);
        }
    }

    @Test
    void aTableBesideABadgeKeepsItsMarginsInTheBand() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(500, 600, 20, page -> {
            page.addParagraph("Above");
            body(page);
        })) {
            XWPFTable lines = document.getTables().get(0);
            assertThat(lines.getText()).contains("Degree");
            assertThat(indentOf(lines)).as("clear of the badge, as the page sets it")
                    .isEqualTo(Math.round(INSET * 20));
            XWPFParagraph above = document.getParagraphs().get(0);
            assertThat(above.getText()).isEqualTo("Above");
            assertThat(above.getSpacingAfter()).as("and its top margin stands above it")
                    .isEqualTo((int) Math.round(DOWN * 20));
        }
    }

    @Test
    void aTableOpeningACellBesideABadgeKeepsItsTopMarginOverIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(500, 600, 20, page -> page
                .addParagraph("Above")
                .addRow(row -> row.name("Columns")
                        .addSection("Education", this::bodyIn)
                        .addSection("Other", other -> other.addParagraph("Other"))))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);
            List<IBodyElement> elements = cell.getBodyElements();
            assertThat(elements.get(0)).as("nothing above the table in its cell holds the space")
                    .isInstanceOf(XWPFParagraph.class);
            XWPFParagraph hairline = (XWPFParagraph) elements.get(0);
            assertThat(hairline.getText()).isEmpty();
            assertThat(hairline.getSpacingBefore()).as("so a hairline over it holds its top margin")
                    .isEqualTo((int) Math.round(DOWN * 20));
            XWPFTable lines = (XWPFTable) elements.get(1);
            assertThat(indentOf(lines)).isEqualTo(Math.round(INSET * 20));
        }
    }

    @Test
    void aTableARowPlacesAsAColumnIsNotIndentedTwice() throws Exception {
        // The row's cell starts where the layout placed the table, past its margin already.
        // RowBuilder refuses a table, and a row node built by hand holds one.
        DocumentNode row = new com.demcha.compose.document.node.RowNode("Columns",
                List.of(lines(), new SectionBuilder().name("Other").addParagraph("Other").build()),
                List.of(1.0, 1.0), 0, DocumentInsets.zero(), DocumentInsets.zero(), null, null,
                com.demcha.compose.document.style.DocumentCornerRadius.ZERO, null, List.of());
        try (XWPFDocument document = DocxExports.withLayout(500, 600, 20, page -> page.add(row))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);
            XWPFTable lines = cell.getTables().get(0);
            assertThat(lines.getText()).contains("Degree");
            assertThat(indentOf(lines)).as("the column holds the margin, not the table").isZero();
        }
    }

    @Test
    void aTableOpeningACellOutsideABandHoldsNoHairline() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(500, 600, 20, page -> page
                .addRow(row -> row.name("Columns")
                        .addSection("Education", section -> section.add(lines()))
                        .addSection("Other", other -> other.addParagraph("Other"))))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);
            assertThat(cell.getBodyElements().get(0)).as("nothing resumes there, so nothing is held")
                    .isInstanceOf(XWPFTable.class);
        }
    }

    private void bodyIn(SectionBuilder section) {
        section.addLayerStack(stack -> stack.name("Body")
                .layer(lines(), LayerAlign.TOP_LEFT, 0)
                .position(badge(), 0, 0, LayerAlign.TOP_LEFT, 1));
    }

    private void body(PageFlowBuilder page) {
        page.addSection("Education", this::bodyIn);
    }

    private static DocumentNode lines() {
        return new TableBuilder().name("Lines")
                .margin(new DocumentInsets(DOWN, 0, 0, INSET)).width(150)
                .columns(DocumentTableColumn.fixed(150))
                .defaultCellStyle(DocumentTableStyle.builder().padding(DocumentInsets.zero())
                        .stroke(DocumentStroke.of(DocumentColor.WHITE, 0)).build())
                .rowCells(DocumentTableCell.node(new ParagraphBuilder().text("Degree").build()))
                .rowCells(DocumentTableCell.node(new ParagraphBuilder().text("School").build()))
                .build();
    }

    private static DocumentNode badge() {
        return new ShapeContainerBuilder().name("Badge").circle(40)
                .fillColor(DocumentColor.rgb(200, 180, 240))
                .center(new SectionBuilder().name("Glyph").spacer(10, 10).build())
                .build();
    }

    private static long indentOf(XWPFTable table) {
        var properties = table.getCTTbl().getTblPr();
        if (properties == null || !properties.isSetTblInd()) {
            return 0;
        }
        Object width = properties.getTblInd().getW();
        return width instanceof BigInteger value ? value.longValue() : Long.parseLong(String.valueOf(width));
    }
}
