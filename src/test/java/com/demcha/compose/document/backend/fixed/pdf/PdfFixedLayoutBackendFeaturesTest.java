package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfMetadataOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfProtectionOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkOptions;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfFixedLayoutBackendFeaturesTest {
    private static final double COLOR_OPERATOR_TOLERANCE = 0.001;

    @TempDir
    Path tempDir;

    @Test
    void shouldRenderCanonicalPdfChromeAndSemanticAnnotations() throws Exception {
        Path outputFile = tempDir.resolve("canonical-features.pdf");
        byte[] pdfBytes;
        PdfFixedLayoutBackend backend = PdfFixedLayoutBackend.builder()
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
                .build();

        try (DocumentSession document = GraphCompose.document(outputFile)
                .margin(36, 36, 48, 36)
                .create()) {

            document.dsl()
                    .pageFlow()
                    .name("FeatureFlow")
                    .spacing(14)
                    .addParagraph(paragraph -> paragraph
                            .name("Overview")
                            .text("Visit the canonical GraphCompose docs.")
                            .textStyle(DocumentTextStyle.DEFAULT)
                            .bookmark(new DocumentBookmarkOptions("Overview"))
                            .link(new DocumentLinkOptions("https://example.com/docs")))
                    .addBarcode(barcode -> barcode
                            .name("Qr")
                            .data("https://example.com/invoice/42")
                            .qrCode()
                            .size(96, 96))
                    .addParagraph(paragraph -> paragraph
                            .name("Details")
                            .text("Document-level metadata, watermark, footer, and link annotations should all survive the canonical pipeline.")
                            .textStyle(DocumentTextStyle.DEFAULT))
                    .build();

            pdfBytes = document.render(backend);
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
                            .textStyle(DocumentTextStyle.DEFAULT)
                            .inlineLink("Email", new DocumentLinkOptions("mailto:alex@example.dev"))
                            .inlineText(" | ")
                            .inlineLink("Docs", new DocumentLinkOptions("https://example.com/docs")))
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

    @Test
    void shouldRenderSectionFillAndStrokeFromCanonicalDsl() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("section-fill-and-stroke", "clean", "document-backend");
        DocumentColor fill = DocumentColor.rgb(245, 248, 255);

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(260, 180)
                .margin(DocumentInsets.of(12))
                .create()) {
            document.pageFlow(page -> page
                    .name("CardFlow")
                    .addSection("InfoCard", card -> card
                            .fillColor(fill)
                            .stroke(DocumentStroke.of(DocumentColor.ROYAL_BLUE, 0.8))
                            .cornerRadius(10)
                            .padding(DocumentInsets.of(8))
                            .addParagraph("Block text inside a filled card.", DocumentTextStyle.DEFAULT)));

            document.buildPdf();
        }

        assertThat(outputFile).exists().isNotEmptyFile();
        try (PDDocument document = Loader.loadPDF(outputFile.toFile())) {
            assertThat(hasFillColor(document, fill.color())).isTrue();
            assertThat(hasStrokeColor(document, DocumentColor.ROYAL_BLUE.color())).isTrue();
            assertThat(hasOperator(document, "c")).isTrue();
            assertThat(new PDFTextStripper().getText(document)).contains("Block text inside a filled card.");
        }
    }

    @Test
    void shouldRenderCanonicalLineAndEllipsePrimitives() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("line-and-ellipse-primitives", "clean", "document-backend");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(260, 180)
                .margin(DocumentInsets.of(12))
                .create()) {
            document.pageFlow(page -> page
                    .name("VisualPrimitives")
                    .spacing(8)
                    .addLine(line -> line
                            .name("Rule")
                            .horizontal(120)
                            .thickness(2)
                            .color(DocumentColor.ROYAL_BLUE))
                    .addEllipse(ellipse -> ellipse
                            .name("Badge")
                            .size(32, 20)
                            .fillColor(DocumentColor.ORANGE)
                            .stroke(DocumentStroke.of(DocumentColor.BLACK, 0.5))));

            document.buildPdf();
        }

        assertThat(outputFile).exists().isNotEmptyFile();
        try (PDDocument document = Loader.loadPDF(outputFile.toFile())) {
            assertThat(hasStrokeColor(document, DocumentColor.ROYAL_BLUE.color())).isTrue();
            assertThat(hasFillColor(document, DocumentColor.ORANGE.color())).isTrue();
            assertThat(hasOperator(document, "l")).isTrue();
            assertThat(hasOperator(document, "c")).isTrue();
        }
    }

    @Test
    void shouldRenderCanonicalImageFitModes() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("image-fit-modes", "clean", "document-backend");
        byte[] imageBytes = testPng(30, 10);

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(260, 180)
                .margin(DocumentInsets.of(12))
                .create()) {
            document.pageFlow(page -> page
                    .name("ImageFit")
                    .addImage(image -> image
                            .name("CoverLogo")
                            .source(imageBytes)
                            .fitToBounds(40, 40)
                            .fitMode(DocumentImageFitMode.COVER)));

            document.buildPdf();
        }

        assertThat(outputFile).exists().isNotEmptyFile();
        try (PDDocument document = Loader.loadPDF(outputFile.toFile())) {
            assertThat(document.getPage(0).getResources().getXObjectNames()).isNotEmpty();
            assertThat(hasOperator(document, "Do")).isTrue();
            assertThat(hasOperator(document, "W")).isTrue();
        }
    }

    @Test
    void failedRenderShouldCloseOwnedPdfResourcesAndLeaveCallerStreamOpen() throws Exception {
        LayoutGraph graph;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(180, 180)
                .margin(12, 12, 12, 12)
                .create()) {
            document.add(new ShapeNode(
                    "FailureShape",
                    80,
                    80,
                    Color.LIGHT_GRAY,
                    DocumentStroke.of(DocumentColor.BLACK, 1),
                    DocumentInsets.zero(),
                    DocumentInsets.zero()));
            graph = document.layoutGraph();
        }

        TrackingOutputStream output = new TrackingOutputStream();
        PdfFixedLayoutBackend backend = new PdfFixedLayoutBackend(List.of(new FailingShapeHandler()));

        assertThatThrownBy(() -> backend.write(graph, renderContext(graph, output)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("intentional render failure");
        assertThat(output.closed).isFalse();

        byte[] recovered = new PdfFixedLayoutBackend().render(graph, renderContext(graph, null));
        try (PDDocument document = Loader.loadPDF(recovered)) {
            assertThat(document.getNumberOfPages()).isEqualTo(graph.totalPages());
        }
    }

    @Test
    void renderSessionShouldReuseOneContentStreamPerPageAndRejectUseAfterClose() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage firstPage = new PDPage();
            PDPage secondPage = new PDPage();
            document.addPage(firstPage);
            document.addPage(secondPage);

            PdfRenderSession session = new PdfRenderSession(document, List.of(firstPage, secondPage));
            var firstSurface = session.pageSurface(0);
            assertThat(session.pageSurface(0)).isSameAs(firstSurface);
            assertThat(session.pageSurface(1)).isNotSameAs(firstSurface);

            session.close();
            session.close();
            assertThatThrownBy(() -> session.pageSurface(0))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already closed");
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

    private static boolean hasStrokeColor(PDDocument document, Color expected) throws IOException {
        return hasRgbOperator(document, "RG", expected) || hasRgbOperator(document, "SC", expected);
    }

    private static boolean hasFillColor(PDDocument document, Color expected) throws IOException {
        return hasRgbOperator(document, "rg", expected) || hasRgbOperator(document, "sc", expected);
    }

    private static boolean hasRgbOperator(PDDocument document, String operatorName, Color expected) throws IOException {
        for (var page : document.getPages()) {
            List<Object> tokens = new PDFStreamParser(page).parse();
            for (int index = 3; index < tokens.size(); index++) {
                Object token = tokens.get(index);
                if (token instanceof Operator operator
                        && operatorName.equals(operator.getName())
                        && matchesRgb(tokens.get(index - 3), tokens.get(index - 2), tokens.get(index - 1), expected)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasOperator(PDDocument document, String operatorName) throws IOException {
        for (var page : document.getPages()) {
            List<Object> tokens = new PDFStreamParser(page).parse();
            for (Object token : tokens) {
                if (token instanceof Operator operator && operatorName.equals(operator.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesRgb(Object redToken, Object greenToken, Object blueToken, Color expected) {
        if (!(redToken instanceof COSNumber red)
                || !(greenToken instanceof COSNumber green)
                || !(blueToken instanceof COSNumber blue)) {
            return false;
        }
        return closeTo(red.floatValue(), expected.getRed() / 255.0)
                && closeTo(green.floatValue(), expected.getGreen() / 255.0)
                && closeTo(blue.floatValue(), expected.getBlue() / 255.0);
    }

    private static boolean closeTo(double actual, double expected) {
        return Math.abs(actual - expected) <= COLOR_OPERATOR_TOLERANCE;
    }

    private static byte[] testPng(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, x < width / 2 ? Color.RED.getRGB() : Color.BLUE.getRGB());
            }
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static FixedLayoutRenderContext renderContext(LayoutGraph graph, ByteArrayOutputStream output) {
        return new FixedLayoutRenderContext(
                graph.canvas(),
                List.of(),
                null,
                output);
    }

    private static final class FailingShapeHandler
            implements PdfFragmentRenderHandler<BuiltInNodeDefinitions.ShapeFragmentPayload> {

        @Override
        public Class<BuiltInNodeDefinitions.ShapeFragmentPayload> payloadType() {
            return BuiltInNodeDefinitions.ShapeFragmentPayload.class;
        }

        @Override
        public void render(PlacedFragment fragment,
                           BuiltInNodeDefinitions.ShapeFragmentPayload payload,
                           PdfRenderEnvironment environment) throws Exception {
            environment.pageSurface(fragment.pageIndex());
            throw new IllegalStateException("intentional render failure");
        }
    }

    private static final class TrackingOutputStream extends ByteArrayOutputStream {
        private boolean closed;

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
