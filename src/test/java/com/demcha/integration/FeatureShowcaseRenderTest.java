package com.demcha.integration;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.engine.components.components_builders.ComponentBuilder;
import com.demcha.compose.engine.components.content.header_footer.HeaderFooterConfig;
import com.demcha.compose.engine.components.content.metadata.DocumentMetadata;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.content.watermark.WatermarkConfig;
import com.demcha.compose.engine.components.content.watermark.WatermarkLayer;
import com.demcha.compose.engine.components.content.watermark.WatermarkPosition;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.Align;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.style.ComponentColor;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.testing.VisualTestOutputs;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Human-readable visual entry points for document-platform features that should
 * be easy to inspect in generated PDFs.
 *
 * <p>These tests intentionally write artifacts under {@code target/visual-tests}
 * so contributors can quickly inspect barcode output, page breaks, and
 * document-level chrome while engine changes evolve.</p>
 */
class FeatureShowcaseRenderTest {

    private static final Color TITLE_COLOR = new Color(18, 40, 74);
    private static final Color SECTION_COLOR = new Color(28, 83, 135);
    private static final Color BODY_COLOR = new Color(58, 69, 84);
    private static final TextStyle TITLE_STYLE = TextStyle.builder()
            .size(22)
            .decoration(TextDecoration.BOLD)
            .color(TITLE_COLOR)
            .build();
    private static final TextStyle SECTION_STYLE = TextStyle.builder()
            .size(13)
            .decoration(TextDecoration.BOLD)
            .color(SECTION_COLOR)
            .build();
    private static final TextStyle BODY_STYLE = TextStyle.builder()
            .size(9.5)
            .decoration(TextDecoration.DEFAULT)
            .color(BODY_COLOR)
            .build();
    private static final String REPOSITORY_URL = "https://github.com/DemchaAV/GraphCompose";

    @Test
    void shouldRenderBarcodeGalleryWithoutGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("barcode_showcase_render", "clean", "integration", "features");

        renderBarcodeGallery(outputFile, false);

        assertPdfLooksValid(outputFile, 1);
    }

    @Test
    void shouldRenderBarcodeGalleryWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("barcode_showcase_render_guides", "guides", "integration", "features");

        renderBarcodeGallery(outputFile, true);

        assertPdfLooksValid(outputFile, 1);
    }

    @Test
    void shouldRenderPaginatedDocumentFeaturesWithoutGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("document_features_paginated", "clean", "integration", "features");

        renderPaginatedDocumentFeatures(outputFile, false);

        assertPaginatedFeaturePdf(outputFile);
    }

    @Test
    void shouldRenderPaginatedDocumentFeaturesWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("document_features_paginated_guides", "guides", "integration", "features");

        renderPaginatedDocumentFeatures(outputFile, true);

        assertPaginatedFeaturePdf(outputFile);
    }

    private void renderBarcodeGallery(Path outputFile, boolean guideLines) throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(24, 24, 24, 24)
                .guideLines(guideLines)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();
            double contentWidth = composer.canvas().innerWidth();

            cb.vContainer(Align.middle(12))
                    .entityName("BarcodeShowcaseRoot")
                    .size(contentWidth, 0)
                    .anchor(Anchor.topLeft())
                    .addChild(title(cb, "Barcode and QR Showcase", "BarcodeShowcaseTitle"))
                    .addChild(body(cb,
                            "This artifact is meant for manual inspection. It keeps the composition simple so contributors can quickly verify how QR, Code 128, and EAN-13 output look in the real PDF renderer.",
                            contentWidth,
                            "BarcodeShowcaseLead"))
                    .addChild(barcodeModule(
                            composer,
                            cb,
                            "QR Code",
                            cb.barcode()
                                    .entityName("BarcodeQrSample")
                                    .data(REPOSITORY_URL)
                                    .qrCode()
                                    .size(118, 118)
                                    .foreground(new Color(22, 77, 122))
                                    .anchor(Anchor.topCenter())
                                    .build(),
                            "This QR now points directly to the GraphCompose repository so the showcase PDF doubles as a shareable project handoff artifact.",
                            "BarcodeQrModule"))
                    .addChild(barcodeModule(
                            composer,
                            cb,
                            "Code 128 Shipping Label",
                            cb.barcode()
                                    .entityName("BarcodeCode128Sample")
                                    .data("SHIP-998877665544-ALPHA")
                                    .code128()
                                    .size(320, 72)
                                    .quietZone(4)
                                    .anchor(Anchor.topCenter())
                                    .build(),
                            "Code 128 is the practical workhorse for logistics and warehouse labels where dense alphanumeric payloads matter.",
                            "BarcodeCode128Module"))
                    .addChild(barcodeModule(
                            composer,
                            cb,
                            "EAN-13 Retail Code",
                            cb.barcode()
                                    .entityName("BarcodeEan13Sample")
                                    .data("4006381333931")
                                    .ean13()
                                    .size(240, 96)
                                    .background(new Color(252, 252, 252))
                                    .quietZone(2)
                                    .anchor(Anchor.topCenter())
                                    .build(),
                            "EAN-13 gives a familiar retail-style output so library users can validate product-code scenarios without writing custom PDFBox code.",
                            "BarcodeEan13Module"))
                    .build();

            if (!guideLines) {
                LayoutSnapshotAssertions.assertMatches(composer.layoutSnapshot(), "integration/features/barcode_showcase_render");
            }
            composer.build();
        }
    }

    private void renderPaginatedDocumentFeatures(Path outputFile, boolean guideLines) throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(28, 28, 42, 28)
                .markdown(true)
                .guideLines(guideLines)
                .create()) {

            composer.metadata(DocumentMetadata.builder()
                    .title("GraphCompose Document Features Showcase")
                    .author("GraphCompose integration tests")
                    .subject("Visual verification for pagination and document features")
                    .keywords("graphcompose, pagination, barcode, watermark, header, footer")
                    .build());
            composer.watermark(WatermarkConfig.builder()
                    .text("PREVIEW")
                    .fontSize(82)
                    .rotation(38)
                    .opacity(0.08f)
                    .color(new Color(54, 92, 131))
                    .layer(WatermarkLayer.BEHIND_CONTENT)
                    .position(WatermarkPosition.CENTER)
                    .build());
            composer.header("GraphCompose", "Document Features Showcase", "{date}");
            composer.footer(HeaderFooterConfig.builder()
                    .leftText("integration/features")
                    .centerText("Page {page} of {pages}")
                    .rightText("visual smoke")
                    .showSeparator(true)
                    .build());

            ComponentBuilder cb = composer.componentBuilder();
            double contentWidth = composer.canvas().innerWidth();

            var root = cb.vContainer(Align.middle(12))
                    .entityName("DocumentFeaturesRoot")
                    .size(contentWidth, 0)
                    .anchor(Anchor.topLeft());

            root.addChild(title(cb, "Paginated Document Features", "DocumentFeaturesTitle"));
            root.addChild(body(cb,
                    "This document combines visible PDF chrome with layout-sensitive content. The goal is to give contributors a single artifact where they can inspect header/footer placement, watermark layering, explicit page breaks, bookmarks, and natural multi-page flow.",
                    contentWidth,
                    "DocumentFeaturesLead"));

            root.addChild(cb.moduleBuilder(Align.middle(8), composer.canvas())
                    .entityName("OverviewModule")
                    .bookmark("1. Overview")
                    .anchor(Anchor.topLeft())
                    .addChild(sectionTitle(cb, "Overview", "OverviewHeading"))
                    .addChild(body(cb,
                            "The first section keeps a compact summary plus a QR code so the document shows that barcode entities can live inside an otherwise text-heavy report composition.",
                            contentWidth,
                            "OverviewBody"))
                    .addChild(cb.divider()
                            .entityName("OverviewDivider")
                            .width(contentWidth)
                            .thickness(1)
                            .color(ComponentColor.LIGHT_GRAY)
                            .verticalSpacing(6)
                            .build())
                    .addChild(cb.barcode()
                            .entityName("DocumentFeaturesQr")
                            .data(REPOSITORY_URL)
                            .qrCode()
                            .size(104, 104)
                            .foreground(new Color(18, 40, 74))
                            .anchor(Anchor.topCenter())
                            .build())
                    .build());

            root.addChild(cb.pageBreak()
                    .entityName("ForcedPageBreakAfterOverview")
                    .build());

            root.addChild(cb.moduleBuilder(Align.middle(8), composer.canvas())
                    .entityName("ManualPageBreakModule")
                    .bookmark("2. Manual Page Break")
                    .anchor(Anchor.topLeft())
                    .addChild(sectionTitle(cb, "Manual Page Break", "ManualPageBreakHeading"))
                    .addChild(body(cb,
                            "This section starts on a fresh page because the previous child in the root flow is an explicit page-break entity. That makes the artifact useful for validating both the visual result and the pagination contract.",
                            contentWidth,
                            "ManualPageBreakBody"))
                    .addChild(cb.divider()
                            .entityName("ManualPageBreakDivider")
                            .width(contentWidth)
                            .thickness(1.2)
                            .color(ComponentColor.GRAY)
                            .verticalSpacing(8)
                            .build())
                    .addChild(body(cb,
                            "Below the divider we intentionally switch back to ordinary flowing content so the transition from forced page break into normal pagination stays visible in one PDF.",
                            contentWidth,
                            "ManualPageBreakFollowUp"))
                    .build());

            var narrative = cb.moduleBuilder(Align.middle(8), composer.canvas())
                    .entityName("NarrativePaginationModule")
                    .bookmark("3. Natural Pagination")
                    .anchor(Anchor.topLeft())
                    .addChild(sectionTitle(cb, "Natural Pagination", "NarrativePaginationHeading"))
                    .addChild(body(cb,
                            "The paragraphs below are intentionally long so the layout engine has to continue onto later pages without losing spacing rhythm between sections.",
                            contentWidth,
                            "NarrativePaginationLead"));

            for (int i = 1; i <= 9; i++) {
                narrative.addChild(body(
                        cb,
                        repeatedNarrativeParagraph(i),
                        contentWidth,
                        "NarrativeParagraph" + i));
            }

            root.addChild(narrative.build());
            root.build();

            if (!guideLines) {
                LayoutSnapshotAssertions.assertMatches(composer.layoutSnapshot(), "integration/features/document_features_paginated");
            }
            composer.build();
        }
    }

    private Entity barcodeModule(EngineComposerHarness composer,
                                 ComponentBuilder cb,
                                 String heading,
                                 Entity barcode,
                                 String description,
                                 String entityName) {
        double contentWidth = composer.canvas().innerWidth();
        return cb.moduleBuilder(Align.middle(8), composer.canvas())
                .entityName(entityName)
                .anchor(Anchor.topLeft())
                .addChild(sectionTitle(cb, heading, entityName + "Heading"))
                .addChild(cb.divider()
                        .entityName(entityName + "Divider")
                        .width(contentWidth)
                        .thickness(1)
                        .color(ComponentColor.LIGHT_GRAY)
                        .verticalSpacing(6)
                        .build())
                .addChild(barcode)
                .addChild(body(cb, description, contentWidth, entityName + "Body"))
                .build();
    }

    private Entity title(ComponentBuilder cb, String text, String entityName) {
        return cb.text()
                .entityName(entityName)
                .textWithAutoSize(text)
                .textStyle(TITLE_STYLE)
                .anchor(Anchor.topLeft())
                .margin(Margin.bottom(4))
                .build();
    }

    private Entity sectionTitle(ComponentBuilder cb, String text, String entityName) {
        return cb.text()
                .entityName(entityName)
                .textWithAutoSize(text)
                .textStyle(SECTION_STYLE)
                .anchor(Anchor.topLeft())
                .build();
    }

    private Entity body(ComponentBuilder cb, String text, double width, String entityName) {
        return cb.blockText(Align.left(4), BODY_STYLE)
                .entityName(entityName)
                .size(width, 2)
                .anchor(Anchor.topLeft())
                .padding(Padding.of(4))
                .margin(Margin.bottom(2))
                .text(List.of(text), BODY_STYLE, Padding.zero(), Margin.zero())
                .build();
    }

    private void assertPdfLooksValid(Path outputFile, int minimumPages) throws Exception {
        assertThat(outputFile).exists().isRegularFile().isNotEmptyFile();
        try (PDDocument document = Loader.loadPDF(outputFile.toFile())) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(minimumPages);
        }
    }

    private void assertPaginatedFeaturePdf(Path outputFile) throws Exception {
        assertThat(outputFile).exists().isRegularFile().isNotEmptyFile();
        try (PDDocument document = Loader.loadPDF(outputFile.toFile())) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(3);

            PDDocumentInformation info = document.getDocumentInformation();
            assertThat(info.getTitle()).isEqualTo("GraphCompose Document Features Showcase");

            PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
            assertThat(outline).isNotNull();
            assertThat(outline.getFirstChild()).isNotNull();
        }
    }

    private String repeatedNarrativeParagraph(int index) {
        String lead = "Paragraph %d documents a realistic report-style body where the same layout primitives need to stay readable even as content keeps flowing. ".formatted(index);
        String detail = "GraphCompose should preserve vertical rhythm, carry the module across page boundaries, and keep the surrounding document chrome visually stable while the text continues. ";
        String closing = "This makes the artifact useful both for debugging pagination and for quickly checking whether a renderer-side change altered the final PDF appearance.";
        return lead + detail.repeat(5) + closing;
    }
}
