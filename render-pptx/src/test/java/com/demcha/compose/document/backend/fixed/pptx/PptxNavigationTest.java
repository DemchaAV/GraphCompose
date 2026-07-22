package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.drawingml.x2006.main.CTHyperlink;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Navigation emission: fragment- and span-level links become transparent
 * hotspots — external targets as URL hyperlinks, internal targets as
 * slide-jump hyperlinks resolved through the anchor map (including forward
 * references) — and bookmarks name their slides.
 */
class PptxNavigationTest {

    private static final String SLIDE_JUMP_ACTION = "ppaction://hlinksldjump";

    private static DocumentSession composeLinkedDocument() {
        DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(24))
                .create();
        session.add(session.dsl().shape().name("TocEntry").size(120, 30)
                .fillColor(DocumentColor.ROYAL_BLUE)
                .linkTo("details")
                .build());
        session.add(new EllipseBuilder().name("Badge").circle(24)
                .fillColor(DocumentColor.ORANGE)
                .link(new DocumentLinkOptions("https://example.com/gc"))
                .build());
        session.add(new ParagraphBuilder().name("TocLine")
                .inlineLinkTo("See the details section", "details")
                .build());
        session.add(new PageBreakNode("Break", DocumentInsets.zero()));
        session.add(session.dsl().shape().name("Details").size(160, 60)
                .fillColor(DocumentColor.DARK_GRAY)
                .anchor("details")
                .bookmark(new DocumentBookmarkOptions("Details"))
                .build());
        return session;
    }

    @Test
    void internalAndExternalLinksEmitResolvedHotspots() throws Exception {
        try (DocumentSession session = composeLinkedDocument()) {
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                XSLFSlide first = show.getSlides().get(0);
                List<XSLFShape> hotspots = hotspots(first);
                // Fragment-level internal, fragment-level external, plus one
                // deferred hotspot per linked text span — the engine splits
                // inline link text into word spans, so the count mirrors the
                // PDF backend's per-span link annotations.
                assertThat(hotspots).hasSizeGreaterThanOrEqualTo(3);

                // A URL hyperlink carries no slide-jump action (POI writes an
                // empty action attribute).
                XSLFShape external = hotspots.stream()
                        .filter(shape -> !SLIDE_JUMP_ACTION.equals(hyperlink(shape).getAction()))
                        .findFirst().orElseThrow();
                assertThat(first.getPackagePart()
                        .getRelationship(hyperlink(external).getId())
                        .getTargetURI().toString())
                        .isEqualTo("https://example.com/gc");
                // The external hotspot hugs the ellipse fragment box.
                XSLFShape badge = first.getShapes().stream()
                        .filter(shape -> shape instanceof XSLFAutoShape auto
                                && auto.getShapeType() == ShapeType.ELLIPSE)
                        .findFirst().orElseThrow();
                assertThat(external.getAnchor()).isEqualTo(badge.getAnchor());

                List<XSLFShape> internal = hotspots.stream()
                        .filter(shape -> SLIDE_JUMP_ACTION.equals(hyperlink(shape).getAction()))
                        .toList();
                assertThat(internal).hasSize(hotspots.size() - 1);
                for (XSLFShape hotspot : internal) {
                    assertThat(first.getRelationById(hyperlink(hotspot).getId()))
                            .isSameAs(show.getSlides().get(1));
                }
                // One covers the TOC shape's fragment box, the others the
                // measured span line, so their heights differ. The TOC shape
                // is the slide's first rectangle that isn't a hotspot.
                XSLFShape tocShape = first.getShapes().stream()
                        .filter(shape -> shape instanceof XSLFAutoShape auto
                                && auto.getShapeType() == ShapeType.RECT
                                && !"GraphCompose Link Hotspot".equals(shape.getShapeName()))
                        .findFirst().orElseThrow();
                assertThat(internal.stream().map(XSLFShape::getAnchor))
                        .anyMatch(anchor -> anchor.equals(tocShape.getAnchor()));
                assertThat(internal.stream()
                        .map(shape -> ((Rectangle2D) shape.getAnchor()).getHeight())
                        .distinct()).hasSize(2);
            }
        }
    }

    @Test
    void bookmarksNameTheirSlides() throws Exception {
        try (DocumentSession session = composeLinkedDocument()) {
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                assertThat(show.getSlides().get(1).getXmlObject().getCSld().getName())
                        .isEqualTo("Details");
            }
        }
    }

    @Test
    void aLinkToAMissingAnchorIsSkippedNotFatal() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(session.dsl().shape().name("Dangling").size(100, 30)
                    .fillColor(DocumentColor.GRAY)
                    .linkTo("nowhere")
                    .build());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                assertThat(hotspots(show.getSlides().get(0))).isEmpty();
            }
        }
    }

    private static List<XSLFShape> hotspots(XSLFSlide slide) {
        return slide.getShapes().stream()
                .filter(shape -> "GraphCompose Link Hotspot".equals(shape.getShapeName()))
                .toList();
    }

    private static CTHyperlink hyperlink(XSLFShape shape) {
        CTHyperlink hyperlink = ((CTShape) shape.getXmlObject())
                .getNvSpPr().getCNvPr().getHlinkClick();
        assertThat(hyperlink).as("hotspot must carry a hyperlink").isNotNull();
        return hyperlink;
    }
}
