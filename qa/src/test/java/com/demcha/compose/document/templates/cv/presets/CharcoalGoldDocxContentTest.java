package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
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
                    session.export(new DocxSemanticBackend())));
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                word = extractor.getText();
            }

            Set<String> missing = new LinkedHashSet<>(Arrays.asList(page.split("\\s+")));
            missing.removeAll(Arrays.asList(word.split("\\s+")));
            missing.removeIf(token -> token.codePoints().noneMatch(Character::isLetterOrDigit));
            assertThat(missing).as("words on the page and not in the Word file").isEmpty();
        }
    }
}
