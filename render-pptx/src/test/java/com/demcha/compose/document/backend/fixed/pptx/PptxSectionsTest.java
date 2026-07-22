package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.SectionUnit;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Multi-section concatenation: sections keep their own chrome with
 * section-local page numbering, navigation resolves across section boundaries
 * against the combined deck, and differing page sizes are rejected — a PPTX
 * deck carries exactly one slide size.
 */
class PptxSectionsTest {

    private static final String SLIDE_JUMP_ACTION = "ppaction://hlinksldjump";

    private static SectionUnit sectionUnit(DocumentSession session,
                                           PptxFixedLayoutBackend chrome) throws Exception {
        LayoutGraph graph = session.render(new GraphCapturingBackend());
        return new SectionUnit(graph, graph.canvas(), List.of(), chrome);
    }

    @Test
    void sectionsConcatenateWithSectionLocalChromeAndCrossSectionLinks() throws Exception {
        PptxFixedLayoutBackend coverChrome = PptxFixedLayoutBackend.builder()
                .footer(DocumentHeaderFooter.builder().centerText("{page} of {pages}").build())
                .build();
        PptxFixedLayoutBackend bodyChrome = PptxFixedLayoutBackend.builder()
                .footer(DocumentHeaderFooter.builder().centerText("{page} of {pages}").build())
                .build();

        try (DocumentSession cover = GraphCompose.document()
                .pageSize(400, 300).margin(DocumentInsets.of(24)).create();
             DocumentSession body = GraphCompose.document()
                     .pageSize(400, 300).margin(DocumentInsets.of(24)).create()) {
            cover.add(cover.dsl().shape().name("CoverCard").size(140, 40)
                    .fillColor(DocumentColor.ROYAL_BLUE)
                    .linkTo("summary")
                    .build());
            body.add(body.dsl().shape().name("BodyCard").size(140, 40)
                    .fillColor(DocumentColor.ORANGE).build());
            body.add(new PageBreakNode("Break", DocumentInsets.zero()));
            body.add(body.dsl().shape().name("Summary").size(160, 60)
                    .fillColor(DocumentColor.DARK_GRAY)
                    .anchor("summary")
                    .bookmark(new DocumentBookmarkOptions("Summary"))
                    .build());

            byte[] pptx = coverChrome.renderSections(List.of(
                    sectionUnit(cover, coverChrome),
                    sectionUnit(body, bodyChrome)));

            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                assertThat(show.getSlides()).hasSize(3);

                // Footers restart per section: the cover numbers itself 1 of 1,
                // the body numbers its own two pages.
                assertThat(footerText(show.getSlides().get(0))).isEqualTo("1 of 1");
                assertThat(footerText(show.getSlides().get(1))).isEqualTo("1 of 2");
                assertThat(footerText(show.getSlides().get(2))).isEqualTo("2 of 2");

                // The cover's link resolves across the section boundary to the
                // combined deck's third slide.
                XSLFSlide first = show.getSlides().get(0);
                XSLFShape hotspot = first.getShapes().stream()
                        .filter(shape -> "GraphCompose Link Hotspot".equals(shape.getShapeName()))
                        .findFirst().orElseThrow();
                var hyperlink = ((CTShape) hotspot.getXmlObject())
                        .getNvSpPr().getCNvPr().getHlinkClick();
                assertThat(hyperlink.getAction()).isEqualTo(SLIDE_JUMP_ACTION);
                assertThat(first.getRelationById(hyperlink.getId()))
                        .isSameAs(show.getSlides().get(2));

                assertThat(show.getSlides().get(2).getXmlObject().getCSld().getName())
                        .isEqualTo("Summary");
            }
        }
    }

    @Test
    void crossSectionLinksResolveIntoARasterSection() throws Exception {
        PptxFixedLayoutBackend vectorChrome = new PptxFixedLayoutBackend();
        PptxFixedLayoutBackend rasterChrome = PptxFixedLayoutBackend.builder()
                .rasterSlides(96)
                .build();
        try (DocumentSession cover = GraphCompose.document()
                .pageSize(400, 300).margin(DocumentInsets.of(24)).create();
             DocumentSession body = GraphCompose.document()
                     .pageSize(400, 300).margin(DocumentInsets.of(24)).create()) {
            cover.add(cover.dsl().shape().name("CoverCard").size(140, 40)
                    .fillColor(DocumentColor.ROYAL_BLUE)
                    .linkTo("summary")
                    .build());
            body.add(body.dsl().shape().name("Summary").size(160, 60)
                    .fillColor(DocumentColor.DARK_GRAY)
                    .anchor("summary")
                    .bookmark(new DocumentBookmarkOptions("Summary"))
                    .build());

            byte[] pptx = vectorChrome.renderSections(List.of(
                    sectionUnit(cover, vectorChrome),
                    sectionUnit(body, rasterChrome)));

            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                assertThat(show.getSlides()).hasSize(2);
                // The raster section's content is one picture, but its anchor
                // and bookmark still resolve as combined-deck destinations.
                XSLFSlide first = show.getSlides().get(0);
                XSLFShape hotspot = first.getShapes().stream()
                        .filter(shape -> "GraphCompose Link Hotspot".equals(shape.getShapeName()))
                        .findFirst().orElseThrow();
                var hyperlink = ((CTShape) hotspot.getXmlObject())
                        .getNvSpPr().getCNvPr().getHlinkClick();
                assertThat(hyperlink.getAction()).isEqualTo(SLIDE_JUMP_ACTION);
                assertThat(first.getRelationById(hyperlink.getId()))
                        .isSameAs(show.getSlides().get(1));
                assertThat(show.getSlides().get(1).getXmlObject().getCSld().getName())
                        .isEqualTo("Summary");
            }
        }
    }

    @Test
    void deterministicSectionRendersAreByteIdentical() throws Exception {
        PptxFixedLayoutBackend deterministic = PptxFixedLayoutBackend.builder()
                .deterministic(java.time.Instant.parse("2024-05-05T12:34:56Z"))
                .build();
        try (DocumentSession first = GraphCompose.document()
                .pageSize(400, 300).margin(DocumentInsets.of(24)).create();
             DocumentSession second = GraphCompose.document()
                     .pageSize(400, 300).margin(DocumentInsets.of(24)).create()) {
            first.add(first.dsl().shape().name("A").size(100, 30)
                    .fillColor(DocumentColor.GRAY).build());
            second.add(second.dsl().shape().name("B").size(120, 30)
                    .fillColor(DocumentColor.ORANGE).build());
            List<SectionUnit> sections = List.of(
                    sectionUnit(first, deterministic),
                    sectionUnit(second, deterministic));
            assertThat(deterministic.renderSections(sections))
                    .isEqualTo(deterministic.renderSections(sections));
        }
    }

    @Test
    void sectionsWithDifferingPageSizesAreRejected() throws Exception {
        PptxFixedLayoutBackend chrome = new PptxFixedLayoutBackend();
        try (DocumentSession letter = GraphCompose.document()
                .pageSize(400, 300).margin(DocumentInsets.of(24)).create();
             DocumentSession wide = GraphCompose.document()
                     .pageSize(500, 300).margin(DocumentInsets.of(24)).create()) {
            letter.add(letter.dsl().shape().name("A").size(100, 30)
                    .fillColor(DocumentColor.GRAY).build());
            wide.add(wide.dsl().shape().name("B").size(100, 30)
                    .fillColor(DocumentColor.GRAY).build());
            List<SectionUnit> sections = List.of(
                    sectionUnit(letter, chrome),
                    sectionUnit(wide, chrome));
            assertThatThrownBy(() -> chrome.renderSections(sections))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("one slide size");
        }
    }

    private static String footerText(XSLFSlide slide) {
        return slide.getShapes().stream()
                .filter(shape -> "GraphCompose Footer".equals(shape.getShapeName()))
                .map(shape -> ((XSLFTextBox) shape).getText())
                .findFirst()
                .orElseThrow(() -> new AssertionError("No footer on the slide"));
    }
}
