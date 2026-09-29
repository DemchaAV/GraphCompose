package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A line break in a paragraph's text is a line break in Word.
 *
 * <p>Word reads a {@code "\n"} inside {@code w:t} as a space: an invoice's addressee — name,
 * street, city, email and phone, one paragraph the page breaks at each {@code "\n"} — came
 * out as one wrapped line, the block that much shorter and everything under it higher.</p>
 */
class DocxLineBreakTest {

    private static final String ADDRESS = "Attn: Finance Team\n410 Market Avenue\nManchester, UK";

    @Test
    void eachLineOfAParagraphIsItsOwnLineInWord() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(ADDRESS))) {
            XWPFParagraph address = document.getParagraphs().get(0);
            String xml = address.getCTP().xmlText();

            assertThat(xml).as("no line break left inside a text element").doesNotContain("\n");
            assertThat(xml).as("text, break, text, break, text, in that order").containsSubsequence("Attn: Finance Team</w:t>", "<w:br/>", "410 Market Avenue</w:t>",
                    "<w:br/>", "Manchester, UK</w:t>");
            assertThat(address.getText()).contains("Attn: Finance Team").contains("410 Market Avenue")
                    .contains("Manchester, UK");
        }
    }

    @Test
    void aWindowsLineEndingIsOneBreakAndAnEmptyLineStaysALine() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph("Northwind\r\n\r\nManchester"))) {
            String xml = document.getParagraphs().get(0).getCTP().xmlText();

            assertThat(xml).doesNotContain("\r").doesNotContain("\n");
            assertThat(xml.split("<w:br/>", -1)).as("the empty line between the two is kept").hasSize(3);
        }
    }

    @Test
    void aStyledRunBreaksItsLinesToo() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.inlineText("Bill to").inlineText("\nNorthwind Systems\nManchester")))) {
            String xml = document.getParagraphs().get(0).getCTP().xmlText();

            assertThat(xml).doesNotContain("\n");
            assertThat(xml.split("<w:br/>", -1)).hasSize(3);
        }
    }

    @Test
    void aListItemBreaksItsLinesToo() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addList(list -> list.addItem("Bank transfer\nSort code 40-05-30")))) {
            String body = document.getDocument().xmlText();

            assertThat(body).contains("<w:br/>").doesNotContain("transfer\nSort");
        }
    }
}
