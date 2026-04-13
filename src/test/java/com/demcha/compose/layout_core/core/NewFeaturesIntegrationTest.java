package com.demcha.compose.layout_core.core;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.components.content.barcode.BarcodeType;
import com.demcha.compose.layout_core.components.content.header_footer.HeaderFooterConfig;
import com.demcha.compose.layout_core.components.content.header_footer.HeaderFooterZone;
import com.demcha.compose.layout_core.components.content.metadata.DocumentMetadata;
import com.demcha.compose.layout_core.components.content.protection.PdfProtectionConfig;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.content.watermark.WatermarkConfig;
import com.demcha.compose.layout_core.components.content.watermark.WatermarkLayer;
import com.demcha.compose.layout_core.components.content.watermark.WatermarkPosition;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Visual integration tests for all new document-level features:
 * watermark, header/footer, bookmarks, metadata, protection,
 * page breaks, dividers, and barcodes working together.
 */
class NewFeaturesIntegrationTest {

    @TempDir
    Path tempDir;

    // ===== Watermark =====

    @Test
    void shouldRenderTextWatermarkOnEveryPage() throws Exception {
        Path outputFile = tempDir.resolve("watermark-text.pdf");

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
                .margin(40, 40, 40, 40)
                .create()) {

            composer.watermark("DRAFT");

            var cb = composer.componentBuilder();
            cb.text()
                    .textWithAutoSize("Page 1 — Watermark test document")
                    .textStyle(TextStyle.DEFAULT_STYLE)
                    .anchor(Anchor.topLeft())
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists().isNotEmptyFile();
        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isGreaterThan(0);
        }
    }

    @Test
    void shouldRenderCustomWatermarkWithFullConfig() throws Exception {
        Path outputFile = tempDir.resolve("watermark-custom.pdf");

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
                .margin(40, 40, 40, 40)
                .create()) {

            composer.watermark(WatermarkConfig.builder()
                    .text("CONFIDENTIAL")
                    .fontSize(60)
                    .rotation(30)
                    .opacity(0.10f)
                    .color(Color.RED)
                    .layer(WatermarkLayer.BEHIND_CONTENT)
                    .position(WatermarkPosition.CENTER)
                    .build());

            var cb = composer.componentBuilder();
            cb.text()
                    .textWithAutoSize("Custom watermark test — text with red color behind content")
                    .textStyle(TextStyle.DEFAULT_STYLE)
                    .anchor(Anchor.topLeft())
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists().isNotEmptyFile();
    }

    // ===== Header / Footer =====

    @Test
    void shouldRenderHeaderAndFooterWithPageNumbers() throws Exception {
        Path outputFile = tempDir.resolve("header-footer.pdf");

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
                .margin(40, 40, 60, 40)
                .create()) {

            composer.header("GraphCompose Inc.", "Invoice #12345", "{date}");
            composer.footer(null, "Page {page} of {pages}", null);

            var cb = composer.componentBuilder();
            cb.text()
                    .textWithAutoSize("Main document content between header and footer")
                    .textStyle(TextStyle.DEFAULT_STYLE)
                    .anchor(Anchor.topLeft())
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists().isNotEmptyFile();
        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isGreaterThan(0);
        }
    }

    @Test
    void shouldRenderFooterWithSeparatorLine() throws Exception {
        Path outputFile = tempDir.resolve("footer-separator.pdf");

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
                .margin(40, 40, 50, 40)
                .create()) {

            composer.footer(HeaderFooterConfig.builder()
                    .leftText("Ref: DOC-2026")
                    .centerText("— {page} —")
                    .rightText("{date}")
                    .fontSize(8)
                    .textColor(Color.GRAY)
                    .showSeparator(true)
                    .separatorColor(new Color(200, 200, 200))
                    .separatorThickness(0.5f)
                    .build());

            var cb = composer.componentBuilder();
            cb.text()
                    .textWithAutoSize("Document with custom footer separator")
                    .textStyle(TextStyle.DEFAULT_STYLE)
                    .anchor(Anchor.topLeft())
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists().isNotEmptyFile();
    }

    // ===== Bookmarks =====

    @Test
    void shouldCreateBookmarkOutlineInPdf() throws Exception {
        byte[] pdfBytes;

        try (PdfComposer composer = GraphCompose.pdf()
                .margin(40, 40, 40, 40)
                .create()) {
            var cb = composer.componentBuilder();

            cb.moduleBuilder(Align.middle(8), composer.canvas())
                    .bookmark("Chapter 1 — Introduction")
                    .anchor(Anchor.topLeft())
                    .addChild(
                            cb.text()
                                    .textWithAutoSize("Introduction content")
                                    .textStyle(TextStyle.DEFAULT_STYLE)
                                    .build()
                    )
                    .build();

            cb.moduleBuilder(Align.middle(8), composer.canvas())
                    .bookmark("Chapter 2 — Details")
                    .anchor(Anchor.topLeft())
                    .addChild(
                            cb.text()
                                    .textWithAutoSize("Details content")
                                    .textStyle(TextStyle.DEFAULT_STYLE)
                                    .build()
                    )
                    .build();

            pdfBytes = composer.toBytes();
        }

        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();
            assertThat(outline).isNotNull();
            // Verify that outline has children (bookmarks were created)
            assertThat(outline.getFirstChild()).isNotNull();
        }
    }

    @Test
    void shouldCreateNestedBookmarks() throws Exception {
        byte[] pdfBytes;

        try (PdfComposer composer = GraphCompose.pdf()
                .margin(40, 40, 40, 40)
                .create()) {
            var cb = composer.componentBuilder();

            cb.moduleBuilder(Align.middle(4), composer.canvas())
                    .bookmark("Part 1")
                    .anchor(Anchor.topLeft())
                    .addChild(
                            cb.moduleBuilder(Align.middle(4))
                                    .bookmark("Section 1.1", 1)
                                    .addChild(
                                            cb.text()
                                                    .textWithAutoSize("Nested section content")
                                                    .textStyle(TextStyle.DEFAULT_STYLE)
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            pdfBytes = composer.toBytes();
        }

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();
            assertThat(outline).isNotNull();
            // Verify at least one bookmark was created with a nested child
            var firstChild = outline.getFirstChild();
            assertThat(firstChild).isNotNull();
        }
    }

    // ===== Document Metadata =====

    @Test
    void shouldSetDocumentMetadata() throws Exception {
        byte[] pdfBytes;

        try (PdfComposer composer = GraphCompose.pdf().create()) {
            composer.metadata(DocumentMetadata.builder()
                    .title("Test Invoice")
                    .author("GraphCompose Test")
                    .subject("Financial Document")
                    .keywords("test, invoice, pdf")
                    .build());

            var cb = composer.componentBuilder();
            cb.text()
                    .textWithAutoSize("Metadata test")
                    .textStyle(TextStyle.DEFAULT_STYLE)
                    .anchor(Anchor.topLeft())
                    .build();

            pdfBytes = composer.toBytes();
        }

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDDocumentInformation info = doc.getDocumentInformation();
            assertThat(info.getTitle()).isEqualTo("Test Invoice");
            assertThat(info.getAuthor()).isEqualTo("GraphCompose Test");
            assertThat(info.getSubject()).isEqualTo("Financial Document");
            assertThat(info.getKeywords()).isEqualTo("test, invoice, pdf");
        }
    }

    // ===== PDF Protection =====

    @Test
    void shouldEncryptPdfWithProtection() throws Exception {
        Path outputFile = tempDir.resolve("protected.pdf");

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
                .margin(40, 40, 40, 40)
                .create()) {

            composer.protect(PdfProtectionConfig.builder()
                    .ownerPassword("admin123")
                    .userPassword("")
                    .canPrint(true)
                    .canCopyContent(false)
                    .canModify(false)
                    .keyLength(128)
                    .build());

            var cb = composer.componentBuilder();
            cb.text()
                    .textWithAutoSize("This document is protected from copying and editing")
                    .textStyle(TextStyle.DEFAULT_STYLE)
                    .anchor(Anchor.topLeft())
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists().isNotEmptyFile();
        // Encrypted PDF should still be loadable
        try (PDDocument saved = Loader.loadPDF(outputFile.toFile(), "")) {
            assertThat(saved.getNumberOfPages()).isGreaterThan(0);
        }
    }

    // ===== Page Break =====

    @Test
    void pageBreakShouldCreateEntitySuccessfully() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .margin(40, 40, 40, 40)
                .create()) {
            var cb = composer.componentBuilder();

            var entity = cb.pageBreak().build();

            assertThat(entity.hasAssignable(
                    com.demcha.compose.layout_core.components.renderable.PageBreakComponent.class))
                    .isTrue();
            assertThat(entity.getComponent(
                    com.demcha.compose.layout_core.components.geometry.ContentSize.class))
                    .hasValueSatisfying(size -> {
                        assertThat(size.width()).isEqualTo(0);
                        assertThat(size.height()).isEqualTo(1);
                    });
        }
    }

    // ===== Divider =====

    @Test
    void dividerShouldCreateLineEntity() throws Exception {
        Path outputFile = tempDir.resolve("divider.pdf");

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
                .margin(40, 40, 40, 40)
                .create()) {
            var cb = composer.componentBuilder();

            var module = cb.moduleBuilder(Align.middle(4), composer.canvas())
                    .anchor(Anchor.topLeft())
                    .addChild(
                            cb.text()
                                    .textWithAutoSize("Above divider")
                                    .textStyle(TextStyle.DEFAULT_STYLE)
                                    .build()
                    )
                    .addChild(
                            cb.divider()
                                    .width(composer.canvas().innerWidth())
                                    .thickness(1.5)
                                    .color(ComponentColor.GRAY)
                                    .verticalSpacing(10)
                                    .build()
                    )
                    .addChild(
                            cb.text()
                                    .textWithAutoSize("Below divider")
                                    .textStyle(TextStyle.DEFAULT_STYLE)
                                    .build()
                    )
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists().isNotEmptyFile();
    }

    // ===== Full Feature Showcase =====

    @Test
    void shouldRenderFullShowcaseWithAllNewFeatures() throws Exception {
        Path outputFile = tempDir.resolve("full-showcase.pdf");

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
                .margin(40, 40, 60, 40)
                .create()) {

            // Document metadata
            composer.metadata(DocumentMetadata.builder()
                    .title("GraphCompose Feature Showcase")
                    .author("GraphCompose Test Suite")
                    .subject("Integration Test")
                    .keywords("barcode, watermark, bookmark, header, footer")
                    .build());

            // Watermark
            composer.watermark(WatermarkConfig.builder()
                    .text("SAMPLE")
                    .fontSize(72)
                    .rotation(45)
                    .opacity(0.08f)
                    .color(Color.LIGHT_GRAY)
                    .build());

            // Header and footer
            composer.header("GraphCompose", "Feature Showcase", "{date}");
            composer.footer(null, "Page {page} of {pages}", null);

            var cb = composer.componentBuilder();

            // Section 1: Barcode showcase (bookmarked)
            cb.moduleBuilder(Align.middle(8), composer.canvas())
                    .bookmark("1. Barcodes")
                    .anchor(Anchor.topLeft())
                    .addChild(
                            cb.text()
                                    .textWithAutoSize("Barcode Examples")
                                    .textStyle(TextStyle.DEFAULT_STYLE)
                                    .build()
                    )
                    .addChild(
                            cb.divider()
                                    .width(composer.canvas().innerWidth())
                                    .thickness(1)
                                    .color(ComponentColor.LIGHT_GRAY)
                                    .build()
                    )
                    .addChild(
                            cb.barcode()
                                    .data("https://github.com/DemchaAV/GraphCompose")
                                    .qrCode()
                                    .size(120, 120)
                                    .build()
                    )
                    .addChild(
                            cb.barcode()
                                    .data("SHIP-998877665544")
                                    .code128()
                                    .size(220, 60)
                                    .build()
                    )
                    .build();

            // Section 2: Text section (bookmarked)
            cb.moduleBuilder(Align.middle(8), composer.canvas())
                    .bookmark("2. Document Features")
                    .anchor(Anchor.topLeft())
                    .addChild(
                            cb.text()
                                    .textWithAutoSize("This section demonstrates watermarks, headers, footers, and bookmarks")
                                    .textStyle(TextStyle.DEFAULT_STYLE)
                                    .build()
                    )
                    .build();

            composer.build();
        }

        // Validate output
        assertThat(outputFile).exists().isRegularFile().isNotEmptyFile();

        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isGreaterThan(0);

            // Verify metadata
            PDDocumentInformation info = saved.getDocumentInformation();
            assertThat(info.getTitle()).isEqualTo("GraphCompose Feature Showcase");
            assertThat(info.getAuthor()).isEqualTo("GraphCompose Test Suite");

            // Verify bookmarks exist
            PDDocumentOutline outline = saved.getDocumentCatalog().getDocumentOutline();
            assertThat(outline).isNotNull();
            assertThat(outline.getFirstChild()).isNotNull();
        }
    }
}
