package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import com.demcha.compose.document.style.DocumentInsets;
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
                .anySatisfy(detail -> assertThat(detail).contains(PULLED_OUT));
    }

    @Test
    void inAPanelAMarginBelowZeroWithinABlocksPaddingIsWritten() throws Exception {
        // 20pt of a block's padding, 12 of them taken back: the text stands 8pt into the cell, as
        // on the page, which loses nothing.
        Report exported = report(page -> page.addSection(panel -> panel.fillColor(GRAY).padding(DocumentInsets.of(6))
                .addSection(block -> block.padding(new DocumentInsets(0, 0, 0, 20))
                        .addParagraph(p -> p.text(TEXT).margin(0, 0, 0, -12)))));
        XWPFParagraph written = firstCellParagraph(exported.document());

        assertThat(DocxTwips.of(indentOf(written).getLeft())).isEqualTo(160);
        assertThat(exported.details()).noneMatch(detail -> detail.contains(PULLED_OUT));
    }

    @Test
    void inAPanelABlocksMarginBelowZeroIsHeldAtTheCellsTextAndNamed() throws Exception {
        Report exported = report(page -> page.addSection(panel -> panel.fillColor(GRAY).padding(DocumentInsets.of(6))
                .addSection(block -> block.margin(new DocumentInsets(0, 0, 0, -12)).addParagraph(TEXT))));

        assertThat(exported.details()).anySatisfy(detail -> assertThat(detail).contains(PULLED_OUT));
    }

    @Test
    void aListInAPanelPulledOutIsNamed() throws Exception {
        Report exported = report(page -> page.addSection(panel -> panel.fillColor(GRAY).padding(DocumentInsets.of(6))
                .addList(list -> list.items("One", "Two").margin(0, 0, 0, -12))));

        assertThat(exported.details()).anySatisfy(detail -> assertThat(detail).contains(PULLED_OUT));
    }

    @Test
    void aWordALineStandsOutForIsNotNamedWhereOtherWordsAre() throws Exception {
        // The one word of a column is let stand out past its box; words a line can break between
        // are not, and keep the cell's side.
        Report word = report(page -> page.addRow(row -> row.weights(4, 1).addParagraph("Name")
                .addParagraph(p -> p.text("linkedin.com/in/alexmorgan").margin(new DocumentInsets(0, -60, 0, 0)))));
        Report words = report(page -> page.addRow(row -> row.weights(4, 1).addParagraph("Name")
                .addParagraph(p -> p.text("My profile page linkedin.com/in/alexmorgan")
                        .margin(new DocumentInsets(0, -60, 0, 0)))));

        assertThat(word.details()).noneMatch(detail -> detail.contains(PULLED_OUT));
        assertThat(words.details()).anySatisfy(detail -> assertThat(detail).contains(PULLED_OUT));
    }

    @Test
    void aRowsFirstChildPulledOutPastTheRowIsNamed() throws Exception {
        // Its Word cell starts at the row's edge: a later child's column starts where the child does.
        Report exported = report(page -> page.addRow(row -> row.weights(1, 1)
                .addParagraph(p -> p.text("Pulled out").margin(0, 0, 0, -12))
                .addParagraph("Right")));

        assertThat(exported.details()).anySatisfy(detail -> assertThat(detail)
                .startsWith("the page sets it 12.0pt out past its row's left edge"));
    }

    @Test
    void aRowsFirstIconPulledOutIsDrawnWhereThePageDrawsItAndNotNamed() throws Exception {
        // An icon is drawn as shapes where the layout puts it, whatever cell it stands in.
        com.demcha.compose.document.svg.SvgIcon icon = com.demcha.compose.document.svg.SvgIcon.parse(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 10\"><rect width=\"10\" height=\"10\"/></svg>");
        Report exported = report(page -> page.addRow(row -> row.weights(1, 4)
                .addSection(glyph -> glyph.margin(new DocumentInsets(0, 0, 0, -4))
                        .addSvgIcon(icon, 10, com.demcha.compose.document.node.HorizontalAlign.LEFT))
                .addParagraph("Due on receipt")));

        assertThat(exported.details()).noneMatch(detail -> detail.contains("out past its row's left edge"));
    }

    @Test
    void aBlockPulledRightInTheFlowSetsItsTextOutAsFar() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, 300, 20, page -> page
                .addSection(block -> block.margin(new DocumentInsets(0, -12, 0, 0)).addParagraph(TEXT)))) {
            assertThat(DocxTwips.of(indentOf(paragraph(document)).getRight())).isEqualTo(-240);
        }
    }

    @Test
    void aPictureInAPanelsBlockPulledRightWritesNoSideBelowZero() throws Exception {
        // Held at the cell's text, as a paragraph's is: the picture's paragraph keeps the cell's side.
        Report exported = report(page -> page.addSection(panel -> panel.fillColor(GRAY).padding(DocumentInsets.of(6))
                .addSection(block -> block.margin(new DocumentInsets(0, -12, 0, 0))
                        .addImage(image -> image.source(png()).size(20, 20)))));
        XWPFParagraph picture = exported.document().getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                .filter(p -> !p.getRuns().isEmpty() && !p.getRuns().get(0).getEmbeddedPictures().isEmpty())
                .findFirst().orElseThrow();
        CTInd indent = picture.getCTP().isSetPPr() && picture.getCTP().getPPr().isSetInd()
                ? picture.getCTP().getPPr().getInd() : null;

        assertThat(indent == null || indent.getRight() == null || DocxTwips.of(indent.getRight()) >= 0).isTrue();
    }

    private static byte[] png() {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(4, 4, java.awt.image.BufferedImage.TYPE_INT_RGB);
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (java.io.IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
    }

    private static final String PULLED_OUT = "is not written past its table cell's text";

    private static final com.demcha.compose.document.style.DocumentColor GRAY =
            com.demcha.compose.document.style.DocumentColor.LIGHT_GRAY;

    private record Report(XWPFDocument document, java.util.List<String> details) {
    }

    private static Report report(java.util.function.Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content)
            throws Exception {
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        byte[] docx;
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content);
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        return new Report(new XWPFDocument(new java.io.ByteArrayInputStream(docx)),
                report.get().notes().stream().map(DocxExportReport.Note::detail).toList());
    }

    private static XWPFParagraph firstCellParagraph(XWPFDocument document) {
        return document.getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                .filter(p -> !p.getText().isBlank()).findFirst().orElseThrow();
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
