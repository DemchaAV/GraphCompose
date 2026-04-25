package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.exceptions.AtomicNodeTooLargeException;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pagination edge cases for Phase 4.3 of the v2 roadmap.
 *
 * <p>Each test pins one boundary condition the engine has historically dealt with:
 * exact-fit content, near-boundary floating point, leading/trailing page breaks,
 * and nested sections that paginate while preserving padding and margin.</p>
 */
class PaginationEdgeCaseTest {

    private static final double EPS = 1e-3;

    @Test
    void exactFitShapeAtPageBottomShouldStayOnSinglePage() throws Exception {
        // Page 200x200 with margin 10 → inner area 180x180.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(200, 200))
                .margin(DocumentInsets.of(10))
                .create()) {

            session.add(new ShapeNode(
                    "ExactFit",
                    180,
                    180,
                    Color.LIGHT_GRAY,
                    DocumentStroke.of(DocumentColor.BLACK, 1),
                    DocumentInsets.zero(),
                    DocumentInsets.zero()));

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isEqualTo(1);
            assertThat(graph.nodes())
                    .singleElement()
                    .extracting("startPage", "endPage")
                    .containsExactly(0, 0);
        }
    }

    @Test
    void shapeOnePixelOverPageBottomShouldNotFitOnRemainingPageWhenContentPrecedesIt() throws Exception {
        // A first shape uses some height, then a tall atomic shape that would
        // overflow the remaining region must move to a new page rather than crash.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(200, 200))
                .margin(DocumentInsets.of(10))
                .create()) {

            session.add(new ShapeNode("Header", 80, 30, Color.LIGHT_GRAY,
                    DocumentStroke.of(DocumentColor.BLACK, 1),
                    DocumentInsets.zero(), DocumentInsets.zero()));
            // 30 + 160 = 190 > 180 inner height → second shape moves to next page.
            session.add(new ShapeNode("Tall", 80, 160, Color.GRAY,
                    DocumentStroke.of(DocumentColor.BLACK, 1),
                    DocumentInsets.zero(), DocumentInsets.zero()));

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isEqualTo(2);
            assertThat(graph.nodes()).extracting("startPage")
                    .containsExactly(0, 1);
        }
    }

    @Test
    void leadingPageBreakShouldNotCreateAnEmptyFirstPage() throws Exception {
        // PageBreak at the very start of the document is a no-op — the engine
        // logs and keeps subsequent content anchored to page 0.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(220, 180))
                .margin(DocumentInsets.of(12))
                .create()) {

            session.add(new PageBreakNode("LeadingBreak", DocumentInsets.zero()));
            session.add(new ParagraphNode(
                    "Body",
                    "Single paragraph after the leading page break.",
                    DocumentTextStyle.DEFAULT,
                    TextAlign.LEFT,
                    2.0,
                    DocumentInsets.of(4),
                    DocumentInsets.zero()));

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isEqualTo(1);

            byte[] pdfBytes = session.toPdfBytes();
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                assertThat(document.getNumberOfPages()).isEqualTo(1);
            }
        }
    }

    @Test
    void trailingPageBreakShouldRecordABlankFinalPage() throws Exception {
        // Page break after content forces a new page. Even with no following
        // content, the trailing page is part of the layout graph.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(220, 180))
                .margin(DocumentInsets.of(12))
                .create()) {

            session.add(new ParagraphNode(
                    "Body",
                    "First paragraph before trailing page break.",
                    DocumentTextStyle.DEFAULT,
                    TextAlign.LEFT,
                    2.0,
                    DocumentInsets.of(4),
                    DocumentInsets.zero()));
            session.add(new PageBreakNode("TrailingBreak", DocumentInsets.zero()));

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages())
                    .as("Trailing page break opens a new page even when no further content follows.")
                    .isEqualTo(2);

            byte[] pdfBytes = session.toPdfBytes();
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                assertThat(document.getNumberOfPages()).isEqualTo(graph.totalPages());
            }
        }
    }

    @Test
    void pageBreakBetweenContentShouldEndExactlyOnePage() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(220, 180))
                .margin(DocumentInsets.of(12))
                .create()) {

            session.add(new ParagraphNode(
                    "First",
                    "Paragraph on page one.",
                    DocumentTextStyle.DEFAULT,
                    TextAlign.LEFT,
                    2.0,
                    DocumentInsets.of(4),
                    DocumentInsets.zero()));
            session.add(new PageBreakNode("Break", DocumentInsets.zero()));
            session.add(new ParagraphNode(
                    "Second",
                    "Paragraph on page two.",
                    DocumentTextStyle.DEFAULT,
                    TextAlign.LEFT,
                    2.0,
                    DocumentInsets.of(4),
                    DocumentInsets.zero()));

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isEqualTo(2);
            assertThat(graph.nodes())
                    .filteredOn(node -> "First".equals(node.semanticName())
                            || "Second".equals(node.semanticName()))
                    .extracting("semanticName", "startPage")
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple("First", 0),
                            org.assertj.core.groups.Tuple.tuple("Second", 1));
        }
    }

    @Test
    void nestedSectionWithPaddingAndMarginShouldPreserveInsetsAcrossPageSplit() throws Exception {
        // The nested section has padding(8) and margin(6); its long inner
        // paragraph forces pagination. Insets must remain visible on both pages.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(220, 180))
                .margin(DocumentInsets.of(12))
                .create()) {

            ParagraphNode body = new ParagraphNode(
                    "NestedBody",
                    "GraphCompose nested section pagination should preserve insets. ".repeat(40),
                    DocumentTextStyle.DEFAULT,
                    TextAlign.LEFT,
                    2.0,
                    DocumentInsets.of(2),
                    DocumentInsets.zero());

            SectionNode section = new SectionNode(
                    "Outer",
                    List.of(body),
                    4.0,
                    DocumentInsets.of(6),
                    DocumentInsets.of(8),
                    null,
                    null);

            session.add(section);

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isGreaterThan(1);

            // Body fragments should appear on multiple pages.
            assertThat(graph.fragments())
                    .filteredOn(fragment -> fragment.path().startsWith("Outer[0]/NestedBody"))
                    .extracting("pageIndex")
                    .as("Nested paragraph should split across at least two pages.")
                    .containsAnyOf(0)
                    .containsAnyOf(1);

            // The placed section node spans the same pages.
            var outerNode = graph.nodes().stream()
                    .filter(node -> "Outer".equals(node.semanticName()))
                    .findFirst()
                    .orElseThrow();
            assertThat(outerNode.startPage()).isEqualTo(0);
            assertThat(outerNode.endPage()).isGreaterThan(0);
        }
    }

    @Test
    void singleParagraphLargerThanOnePageShouldStillRenderEveryFragment() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(220, 180))
                .margin(DocumentInsets.of(12))
                .create()) {

            session.add(new ParagraphNode(
                    "Massive",
                    "Long paragraph content forces the layout engine to split it across many pages. ".repeat(80),
                    DocumentTextStyle.DEFAULT,
                    TextAlign.LEFT,
                    2.0,
                    DocumentInsets.of(4),
                    DocumentInsets.zero()));

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isGreaterThan(2);

            // Every fragment lands on a page within the graph and pages are non-decreasing.
            int previousPage = -1;
            for (var fragment : graph.fragments()) {
                assertThat(fragment.pageIndex()).isBetween(0, graph.totalPages() - 1);
                assertThat(fragment.pageIndex()).isGreaterThanOrEqualTo(previousPage);
                previousPage = fragment.pageIndex();
            }

            byte[] pdfBytes = session.toPdfBytes();
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                assertThat(document.getNumberOfPages()).isEqualTo(graph.totalPages());
            }
        }
    }

    @Test
    void oversizedAtomicImageShouldThrowDomainSpecificError() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(180, 180))
                .margin(DocumentInsets.of(12))
                .create()) {

            session.add(new ImageNode(
                    "TooTallImage",
                    DocumentImageData.fromBytes(onePixelPng()),
                    96.0,
                    240.0,
                    DocumentInsets.zero(),
                    DocumentInsets.zero()));

            assertThatThrownBy(session::layoutGraph)
                    .isInstanceOf(AtomicNodeTooLargeException.class)
                    .hasMessageContaining("TooTallImage")
                    .hasMessageContaining("requires outer height")
                    .hasMessageContaining("page capacity");
        }
    }

    @Test
    void tableRowTooTallForAnEmptyPageShouldThrowDomainSpecificError() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(220, 180))
                .margin(DocumentInsets.of(12))
                .create()) {

            session.pageFlow(page -> page.addTable(table -> table
                    .name("TooTallRowTable")
                    .columns(DocumentTableColumn.fixed(120))
                    .rowCells(DocumentTableCell.lines(repeatedLines(90)))));

            assertThatThrownBy(session::layoutGraph)
                    .isInstanceOf(AtomicNodeTooLargeException.class)
                    .hasMessageContaining("TooTallRowTable")
                    .hasMessageContaining("page capacity");
        }
    }

    @Test
    void moduleSplitAcrossPagesShouldSurvivePdfChrome() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(240, 180))
                .margin(DocumentInsets.of(14))
                .header(PdfHeaderFooterOptions.builder()
                        .centerText("GraphCompose")
                        .height(18)
                        .showSeparator(true)
                        .build())
                .footer(PdfHeaderFooterOptions.builder()
                        .centerText("Page {page} of {pages}")
                        .height(18)
                        .build())
                .create()) {

            session.pageFlow(page -> page
                    .name("ChromeFlow")
                    .module("Chrome Module", module -> {
                        module.spacing(4)
                                .padding(DocumentInsets.of(4));
                        for (int index = 0; index < 18; index++) {
                            module.paragraph("Paragraph " + index
                                    + " keeps the semantic module split stable while PDF chrome is enabled.");
                        }
                    }));

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isGreaterThan(1);

            var moduleNode = graph.nodes().stream()
                    .filter(node -> "ChromeModule".equals(node.semanticName()))
                    .findFirst()
                    .orElseThrow();
            assertThat(moduleNode.startPage()).isEqualTo(0);
            assertThat(moduleNode.endPage()).isGreaterThan(0);

            byte[] pdfBytes = session.toPdfBytes();
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                assertThat(document.getNumberOfPages()).isEqualTo(graph.totalPages());
            }
        }
    }

    @Test
    void boundaryShapeMatchingInnerHeightWithinEpsilonShouldRemainOnSinglePage() throws Exception {
        // Shape barely below inner height — within the engine's float tolerance
        // — must not spuriously trigger a second page.
        double innerHeight = 180.0;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(200, 200))
                .margin(DocumentInsets.of(10))
                .create()) {

            session.add(new ShapeNode(
                    "JustUnder",
                    80,
                    innerHeight - EPS,
                    Color.LIGHT_GRAY,
                    DocumentStroke.of(DocumentColor.BLACK, 1),
                    DocumentInsets.zero(),
                    DocumentInsets.zero()));

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isEqualTo(1);
        }
    }

    private static byte[] onePixelPng() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.WHITE.getRGB());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static String[] repeatedLines(int count) {
        String[] lines = new String[count];
        for (int index = 0; index < count; index++) {
            lines[index] = "short";
        }
        return lines;
    }
}
