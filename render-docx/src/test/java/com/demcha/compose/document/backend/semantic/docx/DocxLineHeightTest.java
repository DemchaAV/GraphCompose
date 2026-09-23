package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A line of text is as tall as the engine measured it, wherever the paragraph sits.
 *
 * <p>Line height is a property of the face, and Word uses its own: measured through Word
 * 16.0 against the reference render, a body line came out at 13.9pt where the document
 * says 9.7, and LibreOffice at 12.1. Nothing about that is visible in one paragraph — it
 * is visible by the bottom of the page, where everything sits lower than it should and the
 * gap has grown with every line since the first.</p>
 *
 * <p>The number is written as {@code w:lineRule="exact"} rather than as a multiple. It is a
 * measurement in points; a multiple would be measured again by the editor, against whatever
 * font it substituted, which is the thing being replaced.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxLineHeightTest {

    private static final double PAGE_WIDTH = 400;
    private static final double MARGIN = 20;
    private static final double TWIPS_PER_POINT = 20.0;

    @Test
    void aBodyParagraphCarriesTheMeasuredHeightExactly() throws Exception {
        try (XWPFDocument document = withLayout(page -> page.addParagraph(p -> p
                .text("A sentence long enough to wrap onto a second line in this column.")
                .textStyle(DocumentTextStyle.DEFAULT.withSize(10.5))))) {

            long line = lineTwips(document.getParagraphs().get(0));
            assertThat(lineRule(document.getParagraphs().get(0))).isEqualTo("exact");
            // 10.5pt of Helvetica measures 9.71pt from ascent to descent, so the line is
            // shorter than the type size — which is the direction this always moves, and
            // the reason a multiple could not express it.
            assertThat(line).isBetween(190L, 196L);
            assertThat(line).isLessThan(Math.round(10.5 * TWIPS_PER_POINT));
        }
    }

    @Test
    void aParagraphInsideARowCarriesItToo() throws Exception {
        // A row's columns are table cells, and a cell's paragraph is written down its own
        // path. The direction mark reached the body and not the cells once already; this
        // pins that the line height did not repeat it.
        try (XWPFDocument document = withLayout(page -> page.addRow(r -> r
                .addParagraph(p -> p.text("Left column"))
                .addParagraph(p -> p.text("Right column"))))) {

            List<XWPFParagraph> cells = document.getTables().get(0).getRow(0).getTableCells()
                    .stream()
                    .flatMap(cell -> cell.getParagraphs().stream())
                    .filter(paragraph -> !paragraph.getText().isBlank())
                    .toList();

            assertThat(cells).hasSize(2);
            for (XWPFParagraph paragraph : cells) {
                assertThat(lineRule(paragraph))
                        .as("the cell paragraph '%s'", paragraph.getText())
                        .isEqualTo("exact");
                assertThat(lineTwips(paragraph)).isGreaterThan(0);
            }
        }
    }

    @Test
    void aListItemCarriesItAsWell() throws Exception {
        try (XWPFDocument document = withLayout(page -> page.addList(l -> l
                .bullet()
                .items("First item", "Second item")))) {

            List<XWPFParagraph> items = document.getParagraphs().stream()
                    .filter(paragraph -> !paragraph.getText().isBlank())
                    .toList();

            assertThat(items).hasSize(2);
            items.forEach(item -> assertThat(lineRule(item)).isEqualTo("exact"));
        }
    }

    @Test
    void differentTextStylesGetDifferentHeights() throws Exception {
        try (XWPFDocument document = withLayout(page -> page
                .addParagraph(p -> p.text("Heading")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(21)))
                .addParagraph(p -> p.text("Body")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(10.5))))) {

            long heading = lineTwips(document.getParagraphs().get(0));
            long body = lineTwips(document.getParagraphs().get(1));
            assertThat(heading)
                    .as("twice the type size is about twice the line")
                    .isBetween(body * 2 - 20, body * 2 + 20);
        }
    }

    @Test
    void withNothingMeasuredNoHeightIsInvented() throws Exception {
        // The export still writes a complete document with no layout behind it — it just
        // has no measurement to state, and Word's own line height applies as before.
        try (XWPFDocument document = DocxExports.withoutLayout(PAGE_WIDTH, 600, MARGIN,
                page -> page.addParagraph(p -> p.text("Body")))) {

            // Narrowly: no line height. The same w:spacing element also carries the space
            // above and below a paragraph, which is the document's own number and is
            // written either way — looking for the element could not tell them apart.
            CTPPr properties = document.getParagraphs().get(0).getCTP().getPPr();
            assertThat(properties == null || !properties.isSetSpacing()
                       || !properties.getSpacing().isSetLine())
                    .as("no w:line, rather than a guessed one")
                    .isTrue();
        }
    }

    @Test
    void aTableCellCarriesTheHeightItsRowWasSizedWith() throws Exception {
        // A text cell's paragraph was written with no line height at all, so Word set it at
        // its own spacing for the font: a totals row whose style states a 14pt face came out
        // 3pt taller than the page draws it. The layout now carries the measurement on the
        // resolved cell, and the cell's paragraph states it exactly.
        try (XWPFDocument document = withLayout(page -> page.addTable(t -> t
                .name("Billing")
                .autoColumns(2)
                .row("Item", "Amount")
                .totalRow("Total", "2 726.00")))) {

            XWPFParagraph body = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0);
            XWPFParagraph totals = document.getTables().get(0).getRow(1).getCell(0).getParagraphs().get(0);

            assertThat(lineRule(body)).isEqualTo("exact");
            assertThat(lineRule(totals)).isEqualTo("exact");
            // 14pt of Helvetica measures 12.95pt from ascent to descent — the table's default
            // cell face and the totals row's alike.
            assertThat(lineTwips(body)).isEqualTo(259L);
            assertThat(lineTwips(totals)).isEqualTo(259L);
        }
    }

    @Test
    void aCellWhoseStyleStatesALargerFaceGetsATallerLine() throws Exception {
        try (XWPFDocument document = withLayout(page -> page.addTable(t -> t
                .name("Sized")
                .autoColumns(1)
                .row("Body")
                .row("Large")
                .rowStyle(1, com.demcha.compose.document.table.DocumentTableStyle.builder()
                        .textStyle(DocumentTextStyle.builder().size(20).build())
                        .build())))) {

            long body = lineTwips(document.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0));
            long large = lineTwips(document.getTables().get(0).getRow(1).getCell(0).getParagraphs().get(0));
            assertThat(large).isGreaterThan(body);
        }
    }

    @Test
    void aCellWithNoAuthoredFaceIsWrittenInTheEnginesDefaultCellFace() throws Exception {
        // With nothing in its cascade stating a face, a cell was written with no run
        // properties and took the document's Normal — 10.5pt here — while the page draws it
        // in the engine's default cell face, 14pt. The row was the right height and the text
        // in it visibly smaller than the page's.
        try (XWPFDocument document = withLayout(page -> page
                .addParagraph(p -> p.text("Body text sets the document's Normal.")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(10.5)))
                .addTable(t -> t.name("Plain").autoColumns(1).row("Cell")))) {

            org.apache.poi.xwpf.usermodel.XWPFRun run = document.getTables().get(0)
                    .getRow(0).getCell(0).getParagraphs().get(0).getRuns().get(0);
            assertThat(run.getFontSizeAsDouble()).isEqualTo(14.0);
        }
    }

    @Test
    void aCellsAuthoredFaceStillWins() throws Exception {
        try (XWPFDocument document = withLayout(page -> page
                .addParagraph(p -> p.text("Body").textStyle(DocumentTextStyle.DEFAULT.withSize(10.5)))
                .addTable(t -> t.name("Styled").autoColumns(1)
                        .defaultCellStyle(com.demcha.compose.document.table.DocumentTableStyle.builder()
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(9))
                                .build())
                        .row("Cell")))) {

            org.apache.poi.xwpf.usermodel.XWPFRun run = document.getTables().get(0)
                    .getRow(0).getCell(0).getParagraphs().get(0).getRuns().get(0);
            assertThat(run.getFontSizeAsDouble()).isEqualTo(9.0);
        }
    }

    private static XWPFDocument withLayout(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(PAGE_WIDTH, 600, MARGIN, content);
    }

    private static long lineTwips(XWPFParagraph paragraph) {
        return DocxTwips.of(paragraph.getCTP().getPPr().getSpacing().getLine());
    }

    private static String lineRule(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        return properties == null || !properties.isSetSpacing()
                ? null
                : properties.getSpacing().getLineRule().toString();
    }
}
