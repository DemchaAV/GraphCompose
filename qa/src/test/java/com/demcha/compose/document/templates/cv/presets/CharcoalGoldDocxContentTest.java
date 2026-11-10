package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.IBody;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every word the {@code CharcoalGold} CV prints is in its Word export.
 *
 * <p>Its experience section is a timeline with its markers on the rail, laid out through two
 * engine wrappers the DOCX export once dropped as drawing: 78 of the page's 290 words — the
 * section's companies, dates and every bullet — were missing from the Word file while the PDF
 * showed them. A word is compared as the PDF's text extractor reads it; a token with no letter
 * or digit in it is a drawn separator, not content.</p>
 */
class CharcoalGoldDocxContentTest {

    @Test
    void everyWordOnThePageIsInTheWordFile() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(0f, 0f, 0f, 0f)
                .create()) {
            CharcoalGold.create().compose(session, CharcoalGoldFixtures.canonicalCv());
            String page;
            try (var pdf = Loader.loadPDF(session.toPdfBytes())) {
                page = new PDFTextStripper().getText(pdf);
            }
            String word;
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(
                    session.export(new DocxSemanticBackend())))) {
                StringBuilder text = new StringBuilder(textOf(document));
                document.getHeaderList().forEach(header -> text.append(textOf(header)));
                document.getFooterList().forEach(footer -> text.append(textOf(footer)));
                word = text.toString();
            }

            Set<String> missing = new LinkedHashSet<>(Arrays.asList(page.split("\\s+")));
            missing.removeAll(Arrays.asList(word.split("\\s+")));
            missing.removeIf(token -> token.codePoints().noneMatch(Character::isLetterOrDigit));
            assertThat(missing).as("words on the page and not in the Word file").isEmpty();
        }
    }

    /**
     * The sidebar and the main column stand side by side, as the cells of one row.
     *
     * <p>The columns are the layers of one stack, drawn name first for the reading order. Written
     * one after the other, the main column began below the whole sidebar and the one-page CV ran
     * to three pages in LibreOffice. The name, drawn in a layer of its own, opens the main
     * column's cell, and the stand-ins that keep its place on the page are not written.</p>
     */
    @Test
    void theSidebarAndTheMainColumnAreTheCellsOfOneRow() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(0f, 0f, 0f, 0f)
                .create()) {
            CharcoalGold.create().compose(session, CharcoalGoldFixtures.canonicalCv());
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(
                    session.export(new DocxSemanticBackend())))) {
                var columns = document.getTables().get(0).getRow(0).getTableCells();

                assertThat(columns).hasSize(2);
                assertThat(textOf(columns.get(0))).contains("CONTACT", "EDUCATION")
                        .doesNotContain("EXPERIENCE");
                assertThat(textOf(columns.get(1))).startsWith("ANASTASIA")
                        .contains("EXPERIENCE").doesNotContain("CONTACT");
            }
        }
    }

    /**
     * The text of every paragraph in a body, a header or a footer, tables nested in cells
     * included.
     *
     * <p>{@code XWPFWordExtractor} reads a cell's own paragraphs and not the tables nested in
     * it, and in a column cell the section headings and the language rows are such tables.</p>
     */
    private static String textOf(IBody body) {
        StringBuilder text = new StringBuilder();
        for (IBodyElement element : body.getBodyElements()) {
            if (element instanceof XWPFParagraph paragraph) {
                text.append(paragraph.getText()).append('\n');
            } else if (element instanceof XWPFTable table) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        text.append(textOf(cell));
                    }
                }
            }
        }
        return text.toString();
    }
}
