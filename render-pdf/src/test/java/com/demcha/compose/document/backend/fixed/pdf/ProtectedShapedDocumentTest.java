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
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Holds the combination that once made the correction a silent no-op.
 *
 * <p>Encrypting happens while a document is being saved, and it writes the ciphertext back
 * into the streams it encrypted — so a glyph map built by a protected first save is
 * unreadable to the correction that runs after it. The first version of this test asked
 * only whether the file still opened and extracted, both of which were true before the
 * correction existed, and it stayed green while the shipped map still named the shaped
 * forms. So the map itself is what is asserted now, read out of the decrypted file: the
 * letters the author wrote, under password, which is only true if protection was applied
 * <em>after</em> the correction rather than before it.</p>
 */
class ProtectedShapedDocumentTest {

    private static final String ARABIC = "مرحبا بالعالم";
    private static final String PASSWORD = "keep-out";

    @Test
    void aPasswordProtectedArabicDocumentCarriesCorrectedGlyphMaps() throws Exception {
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
                    .describedAs("the protection reached the file")
                    .isTrue();
            assertThat(new PDFTextStripper().getText(opened).trim())
                    .describedAs("the text extracts as the words that were written")
                    .isEqualTo(ARABIC);

            // The half a green open-and-extract cannot see: what the file itself says its
            // glyphs mean. Presentation forms here would mean the correction never ran —
            // which is exactly what an encrypted first save used to cause.
            for (COSName name : opened.getPage(0).getResources().getFontNames()) {
                PDFont font = opened.getPage(0).getResources().getFont(name);
                COSBase raw = font.getCOSObject().getDictionaryObject(COSName.TO_UNICODE);
                assertThat(raw).isInstanceOf(COSStream.class);
                String cmap;
                try (InputStream in = ((COSStream) raw).createInputStream()) {
                    cmap = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
                }
                assertThat(cmap)
                        .describedAs("the decrypted map names letters, not shaped forms")
                        .doesNotContainPattern("<FE[0-9A-Fa-f]{2}>")
                        .containsPattern("<06[0-9A-Fa-f]{2}>");
            }
        }
    }
}
