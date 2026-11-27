package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pdf.PdfMeasurementResources;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.font.FontName;
import com.demcha.compose.font.FontFamilyDefinition;
import org.apache.poi.sl.usermodel.TextShape.TextAutofit;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.sl.draw.DrawPaint;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFFreeformShape;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PptxParagraphFragmentRenderHandlerTest {

    @Test
    void preservesResolvedLineFramesSanitizedTextStylesAndExternalLinks() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 260)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow().name("Text")
                    .spacing(8)
                    .addParagraph(p -> p.rich(r -> r
                            .plain("Plain ")
                            .bold("bold ")
                            .italic("italic ")
                            .underline("under ")
                            .strikethrough("strike ")
                            .link("docs", "https://example.com")
                            .plain(" unsupported: 😀")))
                    .addParagraph(p -> p.text("right aligned").align(TextAlign.RIGHT))
                    .addParagraph(p -> p.text("centered").align(TextAlign.CENTER))
                    .build();

            LayoutGraph graph = session.render(new GraphCapturingBackend());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            PptxGeometryAssertions.assertTextGeometryMatches(graph, pptx);

            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                List<XSLFTextBox> boxes = show.getSlides().get(0).getShapes().stream()
                        .filter(XSLFTextBox.class::isInstance)
                        .map(XSLFTextBox.class::cast)
                        .toList();
                assertThat(boxes).hasSize(3);
                List<XSLFTextRun> runs = boxes.get(0).getTextParagraphs().get(0).getTextRuns();
                assertThat(runs).anyMatch(XSLFTextRun::isBold);
                assertThat(runs).anyMatch(XSLFTextRun::isItalic);
                assertThat(runs).anyMatch(XSLFTextRun::isUnderlined);
                assertThat(runs).anyMatch(XSLFTextRun::isStrikethrough);
                XSLFTextRun linked = runs.stream().filter(run -> "docs".equals(run.getRawText()))
                        .findFirst().orElseThrow();
                assertThat(linked.getHyperlink()).isNull();
                assertThat(linked.getFontColor()).isInstanceOf(PaintStyle.SolidPaint.class);
                PaintStyle.SolidPaint linkedPaint = (PaintStyle.SolidPaint) linked.getFontColor();
                assertThat(DrawPaint.applyColorTransform(linkedPaint.getSolidColor()))
                        .isEqualTo(Color.BLACK);
                assertThat(linked.isUnderlined()).isFalse();
                assertThat(runs).allMatch(run -> "Arial".equals(run.getFontFamily()));
                // No run may carry the DrawingML baseline attribute: PowerPoint
                // renders any non-zero baseline as super/subscript and SHRINKS
                // the glyphs. Seat compensation lives in the frame top instead.
                assertThat(runs).noneMatch(XSLFTextRun::isSuperscript);
                assertThat(runs).noneMatch(XSLFTextRun::isSubscript);
                // Kerning must stay off: the engine measured unkerned advances,
                // and PowerPoint would otherwise auto-kern text above ~12pt.
                assertThat(runs).allMatch(run ->
                        run.getXmlObject() instanceof org.openxmlformats.schemas.drawingml.x2006.main.CTRegularTextRun ctRun
                                && ctRun.isSetRPr() && ctRun.getRPr().isSetKern()
                                && ctRun.getRPr().getKern() == 0);

                XSLFAutoShape linkHotspot = show.getSlides().get(0).getShapes().stream()
                        .filter(XSLFAutoShape.class::isInstance)
                        .map(XSLFAutoShape.class::cast)
                        .filter(shape -> "GraphCompose Link Hotspot".equals(shape.getShapeName()))
                        .findFirst().orElseThrow();
                assertThat(linkHotspot.getHyperlink().getAddress())
                        .isEqualTo("https://example.com");
                assertThat(linkHotspot.getAnchor().getWidth()).isPositive();
                assertThat(linkHotspot.getAnchor().getHeight()).isPositive();
            }
        }
    }

    @Test
    void paintsChipImageShapeAndSvgAtMeasuredInlinePositions() throws Exception {
        byte[] png = onePixelPng();
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10"><path fill="#2563eb" d="M0 0 L10 0 L5 10 Z"/></svg>
                """);
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 180)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new ParagraphBuilder().rich(r -> r
                    .plain("A ")
                    .chip(" READY ", DocumentColor.WHITE, DocumentColor.ROYAL_BLUE)
                    .plain(" ")
                    .image(DocumentImageData.fromBytes(png), 12, 12)
                    .plain(" ")
                    .checkbox(12, true, DocumentColor.DARK_GRAY, DocumentColor.ROYAL_BLUE)
                    .plain(" ")
                    .svgIcon(icon, 12)
                    .plain(" done"))
                    .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(11).build())
                    .build());

            LayoutGraph graph = session.render(new GraphCapturingBackend());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            PptxGeometryAssertions.assertInlineSpanAnchorsMatch(graph, pptx);
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                var shapes = show.getSlides().get(0).getShapes();
                assertThat(shapes).anyMatch(XSLFPictureShape.class::isInstance);
                assertThat(shapes).anyMatch(XSLFFreeformShape.class::isInstance);
                assertThat(shapes.stream().filter(XSLFAutoShape.class::isInstance).count())
                        .as("chip plus checkbox layers")
                        .isGreaterThanOrEqualTo(2);
                // A chip/graphics line renders per-span absolute frames only —
                // no empty shared line box is left behind.
                assertThat(shapes).noneMatch(shape -> shape instanceof XSLFTextBox box
                        && "GraphCompose Text Line".equals(box.getShapeName()));
                assertThat(shapes).anyMatch(shape -> shape instanceof XSLFTextBox box
                        && "GraphCompose Inline Text Span".equals(box.getShapeName()));
                assertThat(shapes).anyMatch(shape -> shape instanceof XSLFTextBox box
                        && "GraphCompose Inline Chip Text".equals(box.getShapeName()));
                assertThat(shapes.stream()
                        .filter(XSLFAutoShape.class::isInstance)
                        .map(XSLFAutoShape.class::cast)
                        .filter(shape -> shape.getShapeType() == org.apache.poi.sl.usermodel.ShapeType.ROUND_RECT)
                        .map(shape -> ((CTShape) shape.getXmlObject()).getSpPr().getPrstGeom()
                                .getAvLst().getGdArray(0).getFmla()))
                        .as("the checkbox frame keeps its 0.18 × size radius")
                        .contains("val 18000");
            }
        }
    }

    @Test
    void emitsEveryPreWrappedLineAsANonWrappingAbsoluteFrame() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(180, 240)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new ParagraphBuilder().text(
                    "This deliberately narrow paragraph wraps into several measured lines "
                            + "without asking PowerPoint to reflow any text.").build());

            LayoutGraph graph = session.render(new GraphCapturingBackend());
            var payload = graph.fragments().stream()
                    .map(fragment -> fragment.payload())
                    .filter(com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload.class::isInstance)
                    .map(com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload.class::cast)
                    .findFirst().orElseThrow();
            assertThat(payload.lines().size()).isGreaterThan(2);

            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            PptxGeometryAssertions.assertTextGeometryMatches(graph, pptx);
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                List<XSLFTextBox> lineBoxes = show.getSlides().get(0).getShapes().stream()
                        .filter(XSLFTextBox.class::isInstance)
                        .map(XSLFTextBox.class::cast)
                        .filter(box -> "GraphCompose Text Line".equals(box.getShapeName()))
                        .toList();
                assertThat(lineBoxes).hasSize(payload.lines().size());
                assertThat(lineBoxes).allSatisfy(box -> {
                    assertThat(box.getWordWrap()).isFalse();
                    assertThat(box.getTextAutofit()).isEqualTo(TextAutofit.NONE);
                    assertThat(box.getTopInset()).isZero();
                    assertThat(box.getRightInset()).isZero();
                    assertThat(box.getBottomInset()).isZero();
                    assertThat(box.getLeftInset()).isZero();
                });
            }
        }
    }

    @Test
    void alignsMixedSizesAndUsesRegisteredViewerFontFamily() throws Exception {
        FontName customName = FontName.of("ParityCustom");
        FontFamilyDefinition customFont = FontFamilyDefinition.classpath(
                        customName, "fonts/google/lato/Lato-Regular.ttf")
                .boldResource("fonts/google/lato/Lato-Bold.ttf")
                .italicResource("fonts/google/lato/Lato-Italic.ttf")
                .boldItalicResource("fonts/google/lato/Lato-BoldItalic.ttf")
                .wordFamily("Lato")
                .build();
        DocumentTextStyle large = DocumentTextStyle.builder()
                .fontName(customName).size(20).build();
        DocumentTextStyle small = DocumentTextStyle.builder()
                .fontName(customName).size(9).build();
        byte[] png = onePixelPng();

        try (DocumentSession session = GraphCompose.document()
                .registerFontFamily(customFont)
                .pageSize(360, 180)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new ParagraphBuilder().rich(r -> r
                            .style("Large", large)
                            .style(" small", small)
                            .image(DocumentImageData.fromBytes(png), 12, 12)
                            .style(" baseline", small))
                    .verticalAlign(TextVerticalAlign.CENTER)
                    .build());

            LayoutGraph graph = session.render(new GraphCapturingBackend());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            PptxGeometryAssertions.assertTextGeometryMatches(graph, pptx, List.of(customFont));
            PptxGeometryAssertions.assertInlineSpanAnchorsMatch(graph, pptx, List.of(customFont));

            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                assertThat(show.getFonts()).singleElement().satisfies(font -> {
                    assertThat(font.getTypeface()).isEqualTo("Lato");
                    assertThat(font.getFacets()).hasSize(4);
                });
                List<XSLFTextBox> spans = show.getSlides().get(0).getShapes().stream()
                        .filter(XSLFTextBox.class::isInstance)
                        .map(XSLFTextBox.class::cast)
                        .filter(box -> "GraphCompose Inline Text Span".equals(box.getShapeName()))
                        .toList();
                assertThat(spans).hasSizeGreaterThanOrEqualTo(3);
                assertThat(spans).allSatisfy(box -> assertThat(
                        box.getTextParagraphs().get(0).getTextRuns().get(0).getFontFamily())
                        .isEqualTo("Lato"));
                assertThat(spans).allSatisfy(box -> assertThat(
                        box.getTextParagraphs().get(0).getTextRuns().get(0).isSuperscript())
                        .as("the identical embedded/PDF font metrics need no synthetic shift")
                        .isFalse());
                assertThat(spans.stream().map(box -> box.getAnchor().getY()).distinct().count())
                        .as("20pt and 9pt spans use their own ascent but share one baseline")
                        .isEqualTo(2);
                assertThat(spans.stream()
                        .map(box -> box.getTextParagraphs().get(0).getTextRuns().get(0).getFontSize()))
                        .contains(20.0, 9.0);
            }
        }
    }

    /**
     * A carried family is seated from the OS/2 metrics PowerPoint reads, not from the PDF
     * descriptor's.
     *
     * <p>The test above cannot see the difference: Lato's two sets of metrics divide the same
     * way, which is why it can note that identical metrics need no synthetic shift. Alegreya
     * Sans is the family in the bundled set where they do not — its descriptor says 900 over
     * 300, its typographic metrics 800 plus a 200 line gap over 200 — so it is the one on which
     * seating a family from the wrong fields is observable at all.</p>
     */
    @Test
    void seatsARegisteredFamilyByTheMetricsPowerPointReadsRatherThanTheDescriptors()
            throws Exception {
        FontName customName = FontName.of("AscentGap");
        FontFamilyDefinition family = FontFamilyDefinition.classpath(
                        customName, "fonts/google/alegreyasans/AlegreyaSans-Regular.ttf")
                .wordFamily("Alegreya Sans")
                .build();
        DocumentTextStyle style = DocumentTextStyle.builder().fontName(customName).size(20).build();

        // The premise, measured here rather than assumed, and hand-checkable: seated from the
        // descriptor this family would stand at 20 × 1.2 × 937.5 / 1237.5 = 18.18pt below the
        // frame top, from its typographic metrics at 20 × 96/101 = 19.01pt. A family whose
        // two sets agreed would make the assertion below pass whatever the backend read.
        try (PdfMeasurementResources measurement =
                     PdfMeasurementResources.open(List.of(family))) {
            PdfFont font = measurement.fontLibrary()
                    .getFont(customName, PdfFont.class).orElseThrow();
            TextStyle resolved = TextStyle.builder().fontName(customName).size(20).build();
            // A plain run of a family whose name carries no style suffix: both flags false
            // whichever way the face is worked out, so the premise below is about the metrics
            // and not about the face.
            double seated = PptxViewerMetrics.seatPoints(resolved, font,
                    PptxViewerMetrics.load(family, family.fontSourceSet().orElseThrow()),
                    false, false);
            PdfFont.VerticalMetrics descriptor = font.verticalMetrics(resolved);
            double fromDescriptor = 20 * PptxViewerMetrics.seatRatio(
                    descriptor.ascent(), descriptor.descent());
            assertThat(seated)
                    .isCloseTo(20 * 96.0 / 101.0, org.assertj.core.data.Offset.offset(1e-9));
            assertThat(seated - fromDescriptor)
                    .as("the premise: the two readings are 0.83pt apart for this family at 20pt")
                    .isCloseTo(20 * (96.0 / 101.0 - 1.2 * 937.5 / 1237.5),
                            org.assertj.core.data.Offset.offset(1e-6));
        }

        try (DocumentSession session = GraphCompose.document()
                .registerFontFamily(family)
                .pageSize(360, 180)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new ParagraphBuilder().rich(r -> r.style("Seated", style)).build());

            LayoutGraph graph = session.render(new GraphCapturingBackend());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());

            // The mirror seats its expectation from the metrics the family embeds with, so
            // this fails by the 0.83pt above if the backend ever seats such a family from the
            // descriptor instead.
            PptxGeometryAssertions.assertTextGeometryMatches(graph, pptx, List.of(family));
            assertThat(PptxDeck.fontParts(pptx)).as("and the program travels").isNotEmpty();
        }
    }

    @Test
    void usesRasterFallbackForExactSvgStrokeStylesAndPrimaryStrokePaintForNativePaths()
            throws Exception {
        SvgIcon dashed = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <path fill="none" stroke="#2563eb" stroke-width="1"
                        stroke-dasharray="2 1" stroke-linecap="round"
                        d="M2 5 L8 5"/>
                </svg>
                """);
        SvgIcon gradientStroke = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <defs><linearGradient id="g"><stop offset="0" stop-color="#ff0000"/>
                    <stop offset="1" stop-color="#0000ff"/></linearGradient></defs>
                  <path fill="none" stroke="url(#g)" stroke-width="1" d="M2 5 L8 5"/>
                </svg>
                """);
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 140)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new ParagraphBuilder().rich(r -> r
                    .plain("A ").svgIcon(dashed, 16).plain(" B ")
                    .svgIcon(gradientStroke, 16)).build());

            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                var shapes = show.getSlides().get(0).getShapes();
                assertThat(shapes.stream().filter(XSLFPictureShape.class::isInstance).count())
                        .as("dashed/capped SVG uses the exact transparent raster fallback")
                        .isEqualTo(1);
                XSLFFreeformShape nativeGradient = shapes.stream()
                        .filter(XSLFFreeformShape.class::isInstance)
                        .map(XSLFFreeformShape.class::cast)
                        .findFirst().orElseThrow();
                assertThat(nativeGradient.getLineColor()).isEqualTo(Color.RED);
            }
        }
    }

    private static byte[] onePixelPng() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, Color.RED.getRGB());
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }
}
