package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A paragraph or a list a margin below zero pulls out past a side of the page's text stands out
 * as far in Word, as on the page: its indent is written below zero.
 *
 * <p>The export held every side at zero, and a paragraph the page set 12pt into the left margin
 * stood at the margin in Word, its lines broken elsewhere.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxNegativeMarginTest {

    private static final String TEXT = "Pulled out by twelve points, long enough to wrap onto a second line";

    @Test
    void aParagraphPulledLeftStandsOutPastTheLeftMargin() throws Exception {
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        byte[] docx;
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(300, 300).margin(com.demcha.compose.document.style.DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addParagraph(p -> p.text(TEXT).margin(0, 0, 0, -12)));
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(docx))) {
            assertThat(DocxTwips.of(indentOf(paragraph(document)).getLeft())).isEqualTo(-240);
        }
        assertThat(report.get().notes()).extracting(DocxExportReport.Note::detail)
                .as("written as the page sets it, it loses nothing")
                .noneMatch(detail -> detail.contains("margin below zero"));
    }

    @Test
    void aParagraphPulledRightStandsOutPastTheRightMargin() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, 300, 20, page -> page
                .addParagraph(p -> p.text(TEXT).margin(0, -12, 0, 0)))) {
            assertThat(DocxTwips.of(indentOf(paragraph(document)).getRight())).isEqualTo(-240);
        }
    }

    @Test
    void aListPulledLeftSetsItsMarkersOutAsFar() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, 300, 20, page -> page
                .addList(list -> list.items("One", "Two").margin(0, 0, 0, -12)))) {
            CTInd indent = indentOf(paragraph(document));
            assertThat(left(indent) - DocxTwips.of(indent.getHanging())).as("the marker's place").isEqualTo(-240);
        }
    }

    @Test
    void inAPanelAMarginBelowZeroIsKeptInItsCellAndNamed() throws Exception {
        // The panel is a Word table cell: there the side is held at the cell's text, and named.
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        byte[] docx;
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(com.demcha.compose.document.style.DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addSection(panel -> panel
                    .fillColor(com.demcha.compose.document.style.DocumentColor.LIGHT_GRAY)
                    .padding(com.demcha.compose.document.style.DocumentInsets.of(6))
                    .addParagraph(p -> p.text("Pulled out of its panel").margin(0, 0, 0, -12))));
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(docx))) {
            XWPFParagraph pulled = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                    .filter(p -> !p.getText().isBlank()).findFirst().orElseThrow();
            CTInd indent = pulled.getCTP().isSetPPr() && pulled.getCTP().getPPr().isSetInd()
                    ? pulled.getCTP().getPPr().getInd() : null;
            assertThat(indent == null || indent.getLeft() == null || DocxTwips.of(indent.getLeft()) >= 0)
                    .as("no indent below zero, which Word would cut at the cell's side").isTrue();
        }
        assertThat(report.get().notes()).extracting(DocxExportReport.Note::detail)
                .anySatisfy(detail -> assertThat(detail).contains("its margin below zero is not written"));
    }

    @Test
    void inAPanelAMarginBelowZeroMovesNothingTheCellWrites() throws Exception {
        // Held at the cell's text, the side writes nothing the cell would not: no indent below zero,
        // and the paragraph's own room, which the measure Word is given is reckoned from, as without
        // the margin; nor does a block inside the panel pulled out write a side below zero.
        String plain = panelIndents(inner -> { }, paragraph -> { });
        assertThat(panelIndents(inner -> { }, paragraph -> paragraph.margin(0, 0, 0, -12))).isEqualTo(plain);
        assertThat(panelIndents(inner -> { }, paragraph -> paragraph.margin(0, -12, 0, 0))).isEqualTo(plain);
        assertThat(panelIndents(inner -> inner.margin(new com.demcha.compose.document.style.DocumentInsets(0, -12, 0, 0)),
                paragraph -> { })).isEqualTo(plain);
    }

    /** The indents a panel's paragraph is written with, the block round it and its own sides set as asked. */
    private static String panelIndents(java.util.function.Consumer<com.demcha.compose.document.dsl.SectionBuilder> block,
                                       java.util.function.Consumer<com.demcha.compose.document.dsl.ParagraphBuilder> sides)
            throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addSection(panel -> panel
                .fillColor(com.demcha.compose.document.style.DocumentColor.LIGHT_GRAY)
                .padding(com.demcha.compose.document.style.DocumentInsets.of(6))
                .addSection(inner -> {
                    block.accept(inner);
                    inner.addParagraph(paragraph -> {
                        paragraph.text("Inside the panel, long enough to wrap onto a second line of its own here");
                        sides.accept(paragraph);
                    });
                })))) {
            XWPFParagraph written = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                    .filter(p -> !p.getText().isBlank()).findFirst().orElseThrow();
            return written.getCTP().isSetPPr() && written.getCTP().getPPr().isSetInd()
                    ? written.getCTP().getPPr().getInd().xmlText() : "";
        }
    }

    private static XWPFParagraph paragraph(XWPFDocument document) {
        return document.getParagraphs().stream().filter(p -> !p.getText().isBlank()).findFirst().orElseThrow();
    }

    private static CTInd indentOf(XWPFParagraph paragraph) {
        assertThat(paragraph.getCTP().getPPr().isSetInd()).as("an indent written").isTrue();
        return paragraph.getCTP().getPPr().getInd();
    }

    private static long left(CTInd indent) {
        return indent.getLeft() == null ? 0 : DocxTwips.of(indent.getLeft());
    }
}
