package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A control character the page draws nothing for is written nowhere in the Word file's text,
 * where XML cannot hold most of them and wrote a question mark.
 *
 * @author Artem Demchyshyn
 */
class DocxTextControlsTest {

    @Test
    void theControlsThePageDrawsNothingForAreTakenOutAndTheLineBreaksStay() {
        assertThat(DocxTextControls.kept("Total\u0007 due")).isEqualTo("Total due");
        assertThat(DocxTextControls.kept("Tab\there")).isEqualTo("Tabhere");
        assertThat(DocxTextControls.kept("Nul\u0000l and \u001Fend\u007F")).isEqualTo("Null and end");
        assertThat(DocxTextControls.kept("one\ntwo\r\nthree")).isEqualTo("one\ntwo\r\nthree");
        String plain = "Invoice INV-1";
        assertThat(DocxTextControls.kept(plain)).isSameAs(plain);
    }

    @Test
    void aBodyParagraphAndATableCellReadAsThePageSetsThem() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph("Total\u0007 due")
                .addParagraph("Tab\there")
                .addTable(table -> table.autoColumns(1).row("Cell\u0001 text")))) {

            assertThat(document.getParagraphs()).extracting(XWPFParagraph::getText)
                    .contains("Total due", "Tabhere")
                    .noneMatch(text -> text.contains("?"));
            assertThat(document.getTables().get(0).getRow(0).getCell(0).getText()).isEqualTo("Cell text");
        }
    }

    @Test
    void aSpaceAControlCharacterStoodBeforeIsKept() throws Exception {
        // "\u0007 due" taken out to " due": an edge space Word keeps only where the text says so.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.inlineText("Total").inlineText("\u0007 due")))) {
            assertThat(document.getDocument().xmlText()).contains("<w:t xml:space=\"preserve\"> due</w:t>");
        }
    }

    @Test
    void textWrittenAsXmlIsCleanedWhereItIsWritten() throws Exception {
        // A list level's marker and a badge's initials go into the file as XML, which writes a
        // character it cannot hold as a question mark before any pass reads the file's text.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addList(list -> list.marker("\u0007•").items("One", "Two"))
                .addContainer(badge -> badge.name("Badge").ellipse(40, 40)
                        .fillColor(com.demcha.compose.document.style.DocumentColor.LIGHT_GRAY)
                        .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("A\u0007B").build())))) {
            String numbering;
            try (java.io.InputStream in = document.getPackage().getPart(
                    org.apache.poi.openxml4j.opc.PackagingURIHelper.createPartName("/word/numbering.xml")).getInputStream()) {
                numbering = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
            assertThat(numbering).contains("w:val=\"•\"").doesNotContain("?•");
            assertThat(document.getDocument().xmlText()).contains(">AB<").doesNotContain("A?B");
        }
    }

    @Test
    void aFootersTextReadsAsThePageSetsIt() throws Exception {
        byte[] docx;
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(com.demcha.compose.document.style.DocumentInsets.of(20)).create()) {
            session.chrome().zone(com.demcha.compose.document.output.DocumentPageZone.footer(20, page ->
                    new com.demcha.compose.document.dsl.ParagraphBuilder().text("Page\u0007 one").build()));
            session.pageFlow(page -> page.addParagraph("Body"));
            docx = session.export(new DocxSemanticBackend());
        }
        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(docx))) {
            assertThat(document.getFooterList().get(0).getText().strip()).isEqualTo("Page one");
        }
    }
}
