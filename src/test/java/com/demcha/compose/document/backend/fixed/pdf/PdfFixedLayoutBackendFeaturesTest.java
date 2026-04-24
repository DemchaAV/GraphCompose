package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfBookmarkOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfLinkOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfMetadataOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfProtectionOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkOptions;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.style.Margin;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfFixedLayoutBackendFeaturesTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRenderCanonicalPdfChromeAndSemanticAnnotations() throws Exception {
        Path outputFile = tempDir.resolve("canonical-features.pdf");
        byte[] pdfBytes;

        try (DocumentSession document = GraphCompose.document(outputFile)
                .margin(36, 36, 48, 36)
                .metadata(PdfMetadataOptions.builder()
                        .title("Canonical Feature Test")
                        .author("GraphCompose")
                        .subject("Semantic PDF")
                        .keywords("canonical, pdf, barcode")
                        .build())
                .watermark(PdfWatermarkOptions.builder()
                        .text("CONFIDENTIAL")
                        .fontSize(44)
                        .opacity(0.08f)
                        .build())
                .protect(PdfProtectionOptions.builder()
                        .ownerPassword("owner-secret")
                        .userPassword("")
                        .canCopyContent(false)
                        .build())
                .header(PdfHeaderFooterOptions.builder()
                        .centerText("Canonical PDF")
                        .showSeparator(true)
                        .build())
                .footer(PdfHeaderFooterOptions.builder()
                        .centerText("Page {page} of {pages}")
                        .build())
                .guideLines(true)
                .create()) {

            document.dsl()
                    .pageFlow()
                    .name("FeatureFlow")
                    .spacing(14)
                    .addParagraph(paragraph -> paragraph
                            .name("Overview")
                            .text("Visit the canonical GraphCompose docs.")
                            .textStyle(TextStyle.DEFAULT_STYLE)
                            .bookmark(new PdfBookmarkOptions("Overview"))
                            .link(new PdfLinkOptions("https://example.com/docs")))
                    .addBarcode(barcode -> barcode
                            .name("Qr")
                            .data("https://example.com/invoice/42")
                            .qrCode()
                            .size(96, 96))
                    .addParagraph(paragraph -> paragraph
                            .name("Details")
                            .text("Document-level metadata, watermark, footer, and link annotations should all survive the canonical pipeline.")
                            .textStyle(TextStyle.DEFAULT_STYLE))
                    .build();

            pdfBytes = document.toPdfBytes();
        }

        Files.write(outputFile, pdfBytes);
        assertThat(outputFile).exists().isNotEmptyFile();

        try (PDDocument document = Loader.loadPDF(pdfBytes, "")) {
            assertThat(document.getDocumentInformation().getTitle()).isEqualTo("Canonical Feature Test");
            assertThat(document.getDocumentInformation().getAuthor()).isEqualTo("GraphCompose");
            assertThat(document.getNumberOfPages()).isGreaterThan(0);

            PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
            assertThat(outline).isNotNull();
            PDOutlineItem firstChild = outline.getFirstChild();
            assertThat(firstChild).isNotNull();
            assertThat(firstChild.getTitle()).isEqualTo("Overview");

            assertThat(document.getPage(0).getAnnotations()).isNotEmpty();
            assertThat(document.getPage(0).getAnnotations().getFirst()).isInstanceOf(PDAnnotationLink.class);
            PDAnnotationLink annotation = (PDAnnotationLink) document.getPage(0).getAnnotations().getFirst();
            assertThat(annotation.getAction()).isInstanceOf(PDActionURI.class);
            assertThat(((PDActionURI) annotation.getAction()).getURI()).isEqualTo("https://example.com/docs");

            assertThat(document.getPage(0).getResources().getXObjectNames()).isNotEmpty();

            String extractedText = new PDFTextStripper().getText(document);
            assertThat(extractedText).contains("Visit the canonical GraphCompose docs.");
            assertThat(extractedText).contains("Canonical PDF");
            assertThat(extractedText).contains("Page 1 of 1");
            assertThat(extractedText.replaceAll("\\s+", "")).contains("CONFIDENTIAL");
        }
    }

    @Test
    void shouldRenderInlineParagraphLinksAsSeparateAnnotations() throws Exception {
        byte[] pdfBytes;

        try (DocumentSession document = GraphCompose.document()
                .margin(36, 36, 36, 36)
                .create()) {
            document.dsl()
                    .pageFlow()
                    .name("InlineLinkFlow")
                    .addParagraph(paragraph -> paragraph
                            .name("ContactLinks")
                            .textStyle(TextStyle.DEFAULT_STYLE)
                            .inlineLink("Email", new PdfLinkOptions("mailto:alex@example.dev"))
                            .inlineText(" | ")
                            .inlineLink("Docs", new PdfLinkOptions("https://example.com/docs")))
                    .build();

            pdfBytes = document.toPdfBytes();
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(linkAnnotationUris(document))
                    .containsExactly("mailto:alex@example.dev", "https://example.com/docs");

            String extractedText = new PDFTextStripper().getText(document);
            assertThat(extractedText).contains("Email | Docs");
        }
    }

    private static List<String> linkAnnotationUris(PDDocument document) throws Exception {
        List<String> uris = new ArrayList<>();
        for (var page : document.getPages()) {
            for (var annotation : page.getAnnotations()) {
                if (annotation instanceof PDAnnotationLink link
                        && link.getAction() instanceof PDActionURI action) {
                    uris.add(action.getURI());
                }
            }
        }
        return List.copyOf(uris);
    }
}
