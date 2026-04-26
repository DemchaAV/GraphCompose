package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfMetadataOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfProtectionOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkOptions;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the canonical convenience PDF entrypoints
 * ({@code buildPdf}, {@code writePdf}, {@code toPdfBytes}) honour the
 * document-level options configured directly on {@link DocumentSession}.
 *
 * <p>This guarantees that callers do not need to instantiate
 * {@link com.demcha.compose.document.backend.fixed.pdf.PdfFixedLayoutBackend}
 * just to attach metadata, watermark, protection, or header/footer chrome.</p>
 */
class DocumentSessionConveniencePdfOptionsTest {

    @Test
    void buildPdfAppliesSessionLevelMetadataWatermarkProtectionAndChrome() throws Exception {
        Path outputFile = Files.createTempFile("graphcompose-session-convenience", ".pdf");
        try {
            try (DocumentSession document = GraphCompose.document(outputFile)
                    .margin(36, 36, 48, 36)
                    .create()) {

                document.metadata(PdfMetadataOptions.builder()
                                .title("Convenience Title")
                                .author("Session Author")
                                .subject("Session Subject")
                                .keywords("session, convenience")
                                .build())
                        .watermark(PdfWatermarkOptions.builder()
                                .text("DRAFT")
                                .fontSize(36)
                                .opacity(0.1f)
                                .build())
                        .protect(PdfProtectionOptions.builder()
                                .ownerPassword("owner-pass")
                                .userPassword("")
                                .canCopyContent(false)
                                .canPrint(true)
                                .build())
                        .header(PdfHeaderFooterOptions.builder()
                                .centerText("Convenience Header")
                                .showSeparator(true)
                                .build())
                        .footer(PdfHeaderFooterOptions.builder()
                                .centerText("Page {page} of {pages}")
                                .build());

                document.dsl()
                        .pageFlow()
                        .name("ConvenienceFlow")
                        .addParagraph(paragraph -> paragraph
                                .name("Body")
                                .text("Document-level options configured on the session must apply via buildPdf().")
                                .textStyle(DocumentTextStyle.DEFAULT))
                        .build();

                document.buildPdf();
            }

            assertThat(outputFile).exists().isNotEmptyFile();

            // Load with the empty user password to inherit user-level (restricted) permissions.
            try (PDDocument document = Loader.loadPDF(outputFile.toFile(), "")) {
                assertThat(document.getDocumentInformation().getTitle()).isEqualTo("Convenience Title");
                assertThat(document.getDocumentInformation().getAuthor()).isEqualTo("Session Author");
                assertThat(document.getDocumentInformation().getSubject()).isEqualTo("Session Subject");
                assertThat(document.getDocumentInformation().getKeywords()).isEqualTo("session, convenience");

                assertThat(document.isEncrypted()).isTrue();
                AccessPermission perms = document.getCurrentAccessPermission();
                assertThat(perms.canExtractContent()).isFalse();
                assertThat(perms.canPrint()).isTrue();

                String text = new PDFTextStripper().getText(document);
                assertThat(text).contains("Convenience Header");
                assertThat(text).contains("Page 1 of 1");
                assertThat(text.replaceAll("\\s+", "")).contains("DRAFT");
            }
        } finally {
            Files.deleteIfExists(outputFile);
        }
    }

    @Test
    void toPdfBytesAppliesSessionLevelMetadata() throws Exception {
        byte[] pdfBytes;
        try (DocumentSession document = GraphCompose.document()
                .margin(24, 24, 24, 24)
                .create()) {

            document.metadata(PdfMetadataOptions.builder()
                    .title("In-Memory Title")
                    .author("Bytes Author")
                    .build());

            document.dsl()
                    .pageFlow()
                    .name("BytesFlow")
                    .addParagraph(paragraph -> paragraph
                            .name("Body")
                            .text("toPdfBytes must respect session metadata too.")
                            .textStyle(DocumentTextStyle.DEFAULT))
                    .build();

            pdfBytes = document.toPdfBytes();
        }

        assertThat(pdfBytes).isNotEmpty();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getDocumentInformation().getTitle()).isEqualTo("In-Memory Title");
            assertThat(document.getDocumentInformation().getAuthor()).isEqualTo("Bytes Author");
        }
    }

    @Test
    void clearHeadersAndFootersRemovesPreviouslyRegisteredChrome() throws Exception {
        byte[] pdfBytes;
        try (DocumentSession document = GraphCompose.document()
                .margin(24, 24, 24, 24)
                .create()) {

            document.header(PdfHeaderFooterOptions.builder()
                            .centerText("Should Not Appear")
                            .build())
                    .footer(PdfHeaderFooterOptions.builder()
                            .centerText("Should Also Not Appear")
                            .build())
                    .clearHeadersAndFooters();

            document.dsl()
                    .pageFlow()
                    .name("CleanFlow")
                    .addParagraph(paragraph -> paragraph
                            .name("Body")
                            .text("After clearHeadersAndFooters there should be no chrome text.")
                            .textStyle(DocumentTextStyle.DEFAULT))
                    .build();

            pdfBytes = document.toPdfBytes();
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).doesNotContain("Should Not Appear");
            assertThat(text).doesNotContain("Should Also Not Appear");
        }
    }
}
