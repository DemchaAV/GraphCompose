package com.demcha.compose.document.backend.fixed.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.output.DocumentProtection;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

/**
 * Holds the one combination where correcting a document's glyph maps could cost something.
 *
 * <p>A map is written by the font subsetter, and the subsetter runs while the document is
 * being saved — so a document whose maps need correcting is saved twice: once to build them
 * and once to write the corrected ones. Everything else in a save is idempotent under that.
 * Encryption is the thing that might not be: it is set up as part of writing, and a document
 * that is both password-protected and right-to-left is the only one that reaches that setup
 * more than once.</p>
 *
 * <p>Which is a question about whether the file still opens, so that is what is asked.</p>
 */
class ProtectedShapedDocumentTest {

    private static final String ARABIC = "مرحبا بالعالم";
    private static final String PASSWORD = "keep-out";

    @Test
    void aPasswordProtectedArabicDocumentStillOpensAndReadsBack() throws Exception {
        byte[] pdf;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(300, 120)
                .margin(DocumentInsets.of(20))
                .create()) {

            document.protect(DocumentProtection.builder().userPassword(PASSWORD).build());
            document.pageFlow(page -> page.addParagraph(p -> p.text(ARABIC)
                    .direction(TextDirection.RTL)
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FontName.AMIRI).size(16).build())));

            pdf = document.toPdfBytes();
        }

        try (PDDocument opened = Loader.loadPDF(pdf, PASSWORD)) {
            assertThat(opened.isEncrypted())
                    .describedAs("the protection survives the second write")
                    .isTrue();
            assertThat(new PDFTextStripper().getText(opened).trim())
                    .describedAs("and so does the text, as the word that was written")
                    .isEqualTo(ARABIC);
        }
    }
}
