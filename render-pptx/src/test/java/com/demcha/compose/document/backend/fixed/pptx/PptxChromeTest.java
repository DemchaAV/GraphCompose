package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.output.DocumentPageNumbering;
import com.demcha.compose.document.output.DocumentPageNumberStyle;
import com.demcha.compose.document.output.DocumentWatermark;
import com.demcha.compose.document.output.DocumentWatermarkLayer;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxFontMapping;
import com.demcha.compose.font.FontName;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFConnectorShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Document chrome on the PPTX backend: metadata lands in the OPC core
 * properties, watermarks render per slide at the PDF's placement math and on
 * the PDF's layer, and repeating headers/footers resolve their page tokens
 * with the numbering rules.
 */
class PptxChromeTest {

    private static DocumentSession composeTwoPages() {
        DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(24))
                .create();
        session.add(session.dsl().shape().name("First").size(120, 40)
                .fillColor(DocumentColor.ROYAL_BLUE).build());
        session.add(new PageBreakNode("Break", DocumentInsets.zero()));
        session.add(session.dsl().shape().name("Second").size(120, 40)
                .fillColor(DocumentColor.ORANGE).build());
        return session;
    }

    @Test
    void metadataLandsInTheCoreProperties() throws Exception {
        try (DocumentSession session = composeTwoPages()) {
            byte[] pptx = session.render(PptxFixedLayoutBackend.builder()
                    .metadata(DocumentMetadata.builder()
                            .title("Quarterly Deck")
                            .author("DemchaAV")
                            .subject("Chrome")
                            .keywords("graphs, decks")
                            .build())
                    .build());
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                var core = show.getProperties().getCoreProperties();
                assertThat(core.getTitle()).isEqualTo("Quarterly Deck");
                assertThat(core.getCreator()).isEqualTo("DemchaAV");
                assertThat(core.getSubject()).isEqualTo("Chrome");
                assertThat(core.getKeywords()).isEqualTo("graphs, decks");
                // The canonical default creating application.
                assertThat(show.getProperties().getExtendedProperties().getApplication())
                        .isEqualTo("GraphCompose");
            }
        }
    }

    @Test
    void behindContentWatermarkRendersFirstOnEverySlide() throws Exception {
        try (DocumentSession session = composeTwoPages()) {
            byte[] pptx = session.render(PptxFixedLayoutBackend.builder()
                    .watermark(DocumentWatermark.builder().text("DRAFT").build())
                    .build());
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                for (XSLFSlide slide : show.getSlides()) {
                    XSLFShape first = slide.getShapes().get(0);
                    assertThat(first.getShapeName()).isEqualTo("GraphCompose Watermark");
                    XSLFTextBox watermark = (XSLFTextBox) first;
                    assertThat(watermark.getText()).isEqualTo("DRAFT");
                    // Engine rotation is counter-clockwise in page space;
                    // PowerPoint's positive rotation is clockwise.
                    assertThat(watermark.getRotation()).isEqualTo(-45.0);
                    assertThat(slide.getShapes().size()).isGreaterThan(1);
                }
            }
        }
    }

    @Test
    void theWatermarkFrameOrbitsThePdfBaselineRotationPivot() throws Exception {
        // Hand-derived for DRAFT @ Helvetica-Bold 72pt, CENTER, 45° on a
        // 480x320 page: text width = 3388/1000*72 = 243.936pt, Arial viewer
        // ascent = 72*1854/2048 = 65.180pt, PDF baseline start
        // (118.032, 124) y-up. Orbiting the unrotated frame centre
        // counter-clockwise about that point lands it at (191.315, 96.724)
        // in slide space; PowerPoint then rotates the frame -45 about its
        // own centre. A sign flip in the orbit math moves the centre by
        // tens of points, so this pins the placement, not just the angle.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(480, 320)
                .margin(DocumentInsets.of(24))
                .create()) {
            session.add(session.dsl().shape().name("Content").size(100, 40)
                    .fillColor(DocumentColor.GRAY).build());
            byte[] pptx = session.render(PptxFixedLayoutBackend.builder()
                    .watermark(DocumentWatermark.builder().text("DRAFT").build())
                    .build());
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                XSLFTextBox watermark = (XSLFTextBox) show.getSlides().get(0).getShapes().get(0);
                assertThat(watermark.getShapeName()).isEqualTo("GraphCompose Watermark");
                var anchor = watermark.getAnchor();
                assertThat(anchor.getCenterX()).isCloseTo(191.315, within(0.05));
                assertThat(anchor.getCenterY()).isCloseTo(96.724, within(0.05));
                assertThat(watermark.getRotation()).isEqualTo(-45.0);
            }
        }
    }

    @Test
    void aboveContentWatermarkRendersLast() throws Exception {
        try (DocumentSession session = composeTwoPages()) {
            byte[] pptx = session.render(PptxFixedLayoutBackend.builder()
                    .watermark(DocumentWatermark.builder()
                            .text("CONFIDENTIAL")
                            .layer(DocumentWatermarkLayer.ABOVE_CONTENT)
                            .build())
                    .build());
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                for (XSLFSlide slide : show.getSlides()) {
                    List<XSLFShape> shapes = slide.getShapes();
                    assertThat(shapes.get(shapes.size() - 1).getShapeName())
                            .isEqualTo("GraphCompose Watermark");
                }
            }
        }
    }

    @Test
    void headersAndFootersResolveTokensPerPageWithNumberingRules() throws Exception {
        try (DocumentSession session = composeTwoPages()) {
            byte[] pptx = session.render(PptxFixedLayoutBackend.builder()
                    .header(DocumentHeaderFooter.builder()
                            .leftText("Engine Report")
                            .rightText("Page {page} of {pages}")
                            .showSeparator(true)
                            .build())
                    .footer(DocumentHeaderFooter.builder()
                            .centerText("{page}")
                            .numbering(DocumentPageNumbering.builder()
                                    .showOnFirstPage(false)
                                    .style(DocumentPageNumberStyle.UPPER_ROMAN)
                                    .build())
                            .build())
                    .build());
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                List<String> firstHeaders = zoneTexts(show.getSlides().get(0), "GraphCompose Header");
                assertThat(firstHeaders).containsExactlyInAnyOrder("Engine Report", "Page 1 of 2");
                assertThat(zoneTexts(show.getSlides().get(1), "GraphCompose Header"))
                        .contains("Page 2 of 2");

                // showOnFirstPage=false suppresses the whole zone on page 1;
                // UPPER_ROMAN formats the counted value on page 2.
                assertThat(zoneTexts(show.getSlides().get(0), "GraphCompose Footer")).isEmpty();
                assertThat(zoneTexts(show.getSlides().get(1), "GraphCompose Footer"))
                        .containsExactly("II");

                // Header text sits inside the reserved 30pt top zone, the
                // footer inside the bottom zone.
                XSLFTextBox header = (XSLFTextBox) findShape(show.getSlides().get(0), "GraphCompose Header");
                assertThat(header.getAnchor().getY()).isBetween(0.0, 30.0);
                XSLFTextBox footer = (XSLFTextBox) findShape(show.getSlides().get(1), "GraphCompose Footer");
                assertThat(footer.getAnchor().getY()).isBetween(270.0, 300.0);

                XSLFShape separator = findShape(show.getSlides().get(0), "GraphCompose Header Separator");
                assertThat(separator).isInstanceOf(XSLFConnectorShape.class);
                assertThat(separator.getAnchor().getY()).isEqualTo(30.0);
            }
        }
    }

    @Test
    void aZoneNamingAFamilyTypesetsItsSlideRunInThatFamily() throws Exception {
        try (DocumentSession session = composeTwoPages()) {
            byte[] pptx = session.render(PptxFixedLayoutBackend.builder()
                    .footer(DocumentHeaderFooter.builder()
                            .centerText("Стр. {page}")
                            .fontName(FontName.PT_SANS)
                            .build())
                    .build());

            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                XSLFTextBox footer = (XSLFTextBox) findShape(show.getSlides().get(0), "GraphCompose Footer");

                assertThat(footer.getTextParagraphs().get(0).getTextRuns().get(0).getFontFamily())
                        .as("the family the zone named has to reach the slide run, or PowerPoint"
                                + " typesets the footer in the theme font")
                        .isEqualTo(PptxFontMapping.familyFor(FontName.PT_SANS))
                        .isNotEqualTo(PptxFontMapping.familyFor(FontName.HELVETICA));
                assertThat(footer.getText())
                        .as("and the text survives, rather than being substituted glyph by glyph")
                        .contains("Стр.");
            }
        }
    }

    private static List<String> zoneTexts(XSLFSlide slide, String shapeName) {
        return slide.getShapes().stream()
                .filter(shape -> shapeName.equals(shape.getShapeName()))
                .map(shape -> ((XSLFTextBox) shape).getText())
                .toList();
    }

    private static XSLFShape findShape(XSLFSlide slide, String shapeName) {
        return slide.getShapes().stream()
                .filter(shape -> shapeName.equals(shape.getShapeName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No shape named " + shapeName));
    }
}
