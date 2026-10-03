package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drawing the export has no Word element for — a dot, a ring round a portrait, a timeline's
 * rail — is a shape anchored to the page where the page draws it, behind the text.
 *
 * @author Artem Demchyshyn
 */
class DocxDrawingsTest {

    private static final DocumentColor ACCENT = DocumentColor.rgb(26, 86, 148);

    @Test
    void anEllipseIsDrawnWhereThePagePutsIt() throws Exception {
        AtomicReference<PlacedFragment> dot = new AtomicReference<>();
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument document = export(report, session -> {
            session.pageFlow(page -> page
                    .addParagraph(p -> p.text("Above"))
                    .addEllipse(e -> e.name("Dot").circle(30).fillColor(ACCENT))
                    .addParagraph(p -> p.text("Below")));
            dot.set(fragmentNamed(session, "Dot"));
        })) {
            String anchor = anchors(document.getDocument().xmlText()).get(0);
            // A fragment's y is its bottom edge, measured up from the page's foot.
            double top = 400 - dot.get().y() - dot.get().height();

            assertThat(anchor).contains("prst=\"ellipse\"")
                    .contains("behindDoc=\"1\"")
                    .contains("<wp:positionH relativeFrom=\"page\"><wp:posOffset>"
                              + Units.toEMU(dot.get().x()) + "</wp:posOffset>")
                    .contains("<wp:positionV relativeFrom=\"page\"><wp:posOffset>"
                              + Units.toEMU(top) + "</wp:posOffset>")
                    .contains("srgbClr val=\"1A5694\"");
            assertThat(document.getParagraphs()).extracting(p -> p.getText())
                    .as("the text around it is written as before")
                    .contains("Above", "Below");
            assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
        }
    }

    @Test
    void aPictureClippedToACircleTakesTheCircle() throws Exception {
        try (XWPFDocument document = export(null, session -> session.add(new ShapeContainerBuilder()
                .name("Portrait")
                .circle(60)
                .stroke(DocumentStroke.of(ACCENT, 1))
                .center(new ImageBuilder().name("Photo").source(pngBytes()).size(60, 60).build())
                .build()))) {
            String body = document.getDocument().xmlText();

            assertThat(document.getAllPictures()).hasSize(1);
            assertThat(pictureGeometry(body)).as("the photo, cropped to the circle").containsExactly("ellipse");
            assertThat(anchors(body)).as("the ring drawn round it")
                    .singleElement().asString().contains("prst=\"ellipse\"");
        }
    }

    @Test
    void aLogoSmallerThanItsRoundBadgeStaysWhole() throws Exception {
        // The page clips to the badge's circle, which a 24pt logo in a 60pt badge stands inside.
        try (XWPFDocument document = export(null, session -> session.add(new ShapeContainerBuilder()
                .name("Badge")
                .circle(60)
                .center(new ImageBuilder().name("Logo").source(pngBytes()).size(24, 24).build())
                .build()))) {
            assertThat(pictureGeometry(document.getDocument().xmlText())).containsExactly("rect");
        }
    }

    @Test
    void aPictureInARectangleStaysARectangle() throws Exception {
        try (XWPFDocument document = export(null, session -> session.add(new ShapeContainerBuilder()
                .name("Frame")
                .rectangle(60, 60)
                .center(new ImageBuilder().name("Photo").source(pngBytes()).size(60, 60).build())
                .build()))) {
            assertThat(pictureGeometry(document.getDocument().xmlText())).containsExactly("rect");
        }
    }

    @Test
    void aGlyphInAPaintedBadgeIsDrawnOverTheBadgeWhereThePagePutsIt() throws Exception {
        // A section title beside its badge: written as a paragraph, the glyph took a line of its
        // own above the title, off the circle's middle.
        AtomicReference<PlacedFragment> glyph = new AtomicReference<>();
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument document = export(report, session -> {
            session.pageFlow(page -> page
                    .addParagraph(p -> p.text("Above"))
                    .addContainer(band -> band.name("Header").rectangle(300, 30)
                            .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                            .centerLeft(badge())
                            .position(new ParagraphBuilder().name("Title").text("EXPERIENCE").build(),
                                    34, 0, LayerAlign.CENTER_LEFT)));
            glyph.set(fragmentNamed(session, "Glyph"));
        })) {
            String body = document.getDocument().xmlText();
            List<String> anchors = anchors(body);
            String picture = anchors.stream().filter(a -> a.contains("<pic:pic")).findFirst().orElseThrow();
            double top = 400 - glyph.get().y() - glyph.get().height();

            assertThat(body).as("no picture is written in the flow").doesNotContain("<wp:inline");
            assertThat(anchors).hasSize(2);
            assertThat(picture).contains("r:embed=\"")
                    .contains("<wp:positionH relativeFrom=\"page\"><wp:posOffset>"
                              + Units.toEMU(glyph.get().x()) + "</wp:posOffset>")
                    .contains("<wp:positionV relativeFrom=\"page\"><wp:posOffset>"
                              + Units.toEMU(top) + "</wp:posOffset>");
            assertThat(relativeHeight(document, "<pic:pic")).as("over the circle")
                    .isGreaterThan(relativeHeight(document, "prst=\"ellipse\""));
            assertThat(document.getAllPictures()).hasSize(1);
            assertThat(document.getParagraphs()).extracting(p -> p.getText()).containsSubsequence("Above", "EXPERIENCE");
            assertThat(report.get().notes()).anyMatch(note -> note.severity() == DocxExportReport.Severity.APPROXIMATED
                                                             && note.detail().contains("over the badge"));
        }
    }

    @Test
    void aTitleBesideItsBadgeStandsWhereThePageCentresItInTheHeader() throws Exception {
        // Written after the glyph's line, the title stood at the header's top; alone, it lost the
        // header's height round it, and everything under it moved up.
        AtomicReference<List<com.demcha.compose.document.layout.PlacedNode>> placed = new AtomicReference<>();
        try (XWPFDocument document = export(null, session -> {
            session.pageFlow(page -> page
                    .addParagraph(p -> p.name("Above").text("Above"))
                    .addContainer(band -> band.name("Header").rectangle(300, 40)
                            .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                            .centerLeft(badge())
                            .position(new ParagraphBuilder().name("Title").text("EXPERIENCE").build(),
                                    34, 0, LayerAlign.CENTER_LEFT))
                    .addParagraph(p -> p.name("Below").text("Below")));
            placed.set(session.layoutGraph().nodes());
        })) {
            var above = placedNamed(placed.get(), "Above");
            var header = placedNamed(placed.get(), "Header");
            var title = placedNamed(placed.get(), "Title");
            var below = placedNamed(placed.get(), "Below");

            assertThat(twipsBefore(document, "EXPERIENCE")).as("from the line above to the title, as on the page")
                    .isCloseTo(Math.round((above.placementY() - title.placementY() - title.placementHeight()) * 20),
                            org.assertj.core.data.Offset.offset(2L));
            assertThat(twipsAfter(document, "EXPERIENCE") + twipsBefore(document, "Below"))
                    .as("from the title past the header's foot to the line below, as on the page")
                    .isCloseTo(Math.round((title.placementY() - below.placementY() - below.placementHeight()) * 20),
                            org.assertj.core.data.Offset.offset(3L));
            assertThat(header.placementHeight()).isEqualTo(40);
        }
    }

    private static com.demcha.compose.document.layout.PlacedNode placedNamed(
            List<com.demcha.compose.document.layout.PlacedNode> nodes, String name) {
        return nodes.stream().filter(node -> name.equals(node.semanticName())).findFirst().orElseThrow();
    }

    private static long twipsBefore(XWPFDocument document, String text) {
        return document.getParagraphs().stream().filter(p -> p.getText().equals(text)).findFirst().orElseThrow()
                .getSpacingBefore();
    }

    private static long twipsAfter(XWPFDocument document, String text) {
        return document.getParagraphs().stream().filter(p -> p.getText().equals(text)).findFirst().orElseThrow()
                .getSpacingAfter();
    }

    @Test
    void aBadgeAloneInTheFlowKeepsItsPlaceWithItsGlyphDrawnOverIt() throws Exception {
        AtomicReference<List<com.demcha.compose.document.layout.PlacedNode>> placed = new AtomicReference<>();
        try (XWPFDocument document = export(null, session -> {
            session.pageFlow(page -> page
                    .addParagraph(p -> p.name("Above").text("Above"))
                    .add(badge())
                    .addParagraph(p -> p.name("Below").text("Below")));
            placed.set(session.layoutGraph().nodes());
        })) {
            var above = placedNamed(placed.get(), "Above");
            var below = placedNamed(placed.get(), "Below");

            assertThat(document.getDocument().xmlText()).doesNotContain("<wp:inline");
            assertThat(twipsAfter(document, "Above") + twipsBefore(document, "Below"))
                    .as("the badge's place, held once between the lines around it")
                    .isCloseTo(Math.round((above.placementY() - below.placementY() - below.placementHeight()) * 20),
                            org.assertj.core.data.Offset.offset(3L));
        }
    }

    @Test
    void aFillUnderTextInAPaintedPanelStaysBehindIt() throws Exception {
        // A track holding only drawing, with its label laid over it: in front, it would cover
        // the label. Only a badge whose glyph is drawn over it frames nothing of the flow.
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .addSection("Card", s -> s.fillColor(DocumentColor.rgb(220, 230, 240))
                        .addLayerStack(stack -> stack
                                .layer(new ShapeContainerBuilder().name("Track").rectangle(200, 20)
                                                .fillColor(DocumentColor.rgb(90, 120, 200))
                                                .center(new com.demcha.compose.document.dsl.ShapeBuilder().name("Fill")
                                                        .size(100, 20).fillColor(ACCENT).build())
                                                .build(),
                                        LayerAlign.TOP_LEFT)
                                .layer(new ParagraphBuilder().name("Label").text("80%").build(),
                                        LayerAlign.CENTER))))) ) {
            List<String> tracks = anchors(document.getDocument().xmlText()).stream()
                    .filter(anchor -> anchor.contains("5A78C8")).toList();

            assertThat(tracks).singleElement().asString().contains("behindDoc=\"1\"");
        }
    }

    @Test
    void aBadgeAndItsGlyphInAPaintedPanelStandInFrontOfItsShading() throws Exception {
        // Kept in the flow, the glyph was written white on the panel's shading and the disc,
        // behind the text, hid under it: NorthlineProposal's acceptance heading lost its badge.
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .addSection("Card", s -> s.fillColor(DocumentColor.rgb(220, 230, 240))
                        .addContainer(band -> band.name("Header").rectangle(300, 30)
                                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                                .centerLeft(badge())
                                .position(new ParagraphBuilder().name("Title").text("EXPERIENCE").build(),
                                        34, 0, LayerAlign.CENTER_LEFT)))))) {
            String body = document.getDocument().xmlText();
            List<String> anchors = anchors(body);

            assertThat(body).as("the glyph is no line of the flow").doesNotContain("<wp:inline");
            assertThat(anchors).hasSize(2);
            assertThat(anchors.get(0)).as("the disc first").contains("prst=\"ellipse\"").contains("behindDoc=\"0\"");
            assertThat(anchors.get(1)).as("its glyph over it").contains("<pic:pic").contains("behindDoc=\"0\"");
        }
    }

    @Test
    void anIconDrawnBesideItsLabelInAPaintedPanelStandsInFrontOfItsShading() throws Exception {
        // Behind the text, the panel's shading hid the tile. It is anchored to the page: placed
        // from a paragraph in a nested cell, Word measured it from the outer cell's top.
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .addSection("Card", s -> s.fillColor(DocumentColor.rgb(220, 230, 240))
                        .addContainer(row -> row.name("Fact").rectangle(300, 30)
                                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                                .centerLeft(new ShapeContainerBuilder().name("Tile").roundedRect(27, 27, 6)
                                        .fillColor(ACCENT)
                                        .center(new com.demcha.compose.document.dsl.EllipseBuilder()
                                                .circle(9).fillColor(DocumentColor.WHITE).build())
                                        .build())
                                .position(new ParagraphBuilder().name("Label").text("Compliant").build(),
                                        34, 0, LayerAlign.CENTER_LEFT)))))) {
            assertThat(anchors(document.getDocument().xmlText())).hasSize(2)
                    .allMatch(anchor -> anchor.contains("behindDoc=\"0\""))
                    .allMatch(anchor -> anchor.contains("<wp:positionV relativeFrom=\"page\">"));
        }
    }

    @Test
    void aPanelsDrawingWithNoParagraphBesideItIsAnchoredToThePage() throws Exception {
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .addParagraph(p -> p.text("Above"))
                .addSection("Card", s -> s.fillColor(DocumentColor.rgb(220, 230, 240))
                        .add(new com.demcha.compose.document.dsl.EllipseBuilder().name("Dot").circle(16)
                                .fillColor(ACCENT).build()))))) {
            assertThat(anchors(document.getDocument().xmlText())).singleElement().asString()
                    .contains("<wp:positionV relativeFrom=\"page\">");
        }
    }

    @Test
    void aRowAtTheTopOfAPaintedPanelKeepsItsTopPadding() throws Exception {
        // Nothing above a table holds space in Word; at a panel's top the row's padding was lost.
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .addSection("Card", s -> s.fillColor(DocumentColor.rgb(220, 230, 240))
                        .addRow("DueRow", row -> row.padding(new com.demcha.compose.document.style.DocumentInsets(16, 0, 0, 0))
                                .columns(com.demcha.compose.document.style.DocumentRowColumn.weight(1))
                                .addParagraph(p -> p.text("26 June 2026"))))))) {
            org.apache.poi.xwpf.usermodel.XWPFTableCell panel = document.getTables().get(0).getRow(0).getCell(0);
            org.apache.poi.xwpf.usermodel.XWPFParagraph holder = panel.getParagraphs().get(0);

            assertThat(panel.getBodyElements().get(0)).as("a paragraph holds the space above the row")
                    .isInstanceOf(org.apache.poi.xwpf.usermodel.XWPFParagraph.class);
            assertThat(holder.getCTP().getPPr().getSpacing().getBefore()).isNotNull();
            assertThat(((Number) holder.getCTP().getPPr().getSpacing().getBefore()).intValue()).isEqualTo(16 * 20);
        }
    }

    @Test
    void anIconInTheSameContainerAsItsTitleStaysInTheFlow() throws Exception {
        // Not a badge: the container holds the title too, and its height is the title's line's.
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .add(new ShapeContainerBuilder().name("Pill").roundedRect(200, 28, 14).fillColor(ACCENT)
                        .centerLeft(new ImageBuilder().name("Icon").source(pngBytes()).size(16, 16).build())
                        .position(new ParagraphBuilder().name("Title").text("Remote").build(),
                                24, 0, LayerAlign.CENTER_LEFT)
                        .build())
                .addParagraph(p -> p.text("Below"))))) {
            String body = document.getDocument().xmlText();

            assertThat(body).contains("<wp:inline");
            assertThat(anchors(body)).noneMatch(anchor -> anchor.contains("<pic:pic"));
        }
    }

    @Test
    void aPictureTheFlowIsWrittenRoundStaysInIt() throws Exception {
        List<com.demcha.compose.document.node.DocumentNode> kept = List.of(
                // Cropped to cover its box.
                new ShapeContainerBuilder().circle(22).fillColor(ACCENT).clipPolicy(ClipPolicy.CLIP_PATH)
                        .center(new ImageBuilder().source(pngBytes()).size(11, 11)
                                .fitMode(com.demcha.compose.document.image.DocumentImageFitMode.COVER).build())
                        .build(),
                // A link lands on it.
                new ShapeContainerBuilder().circle(22).fillColor(ACCENT).clipPolicy(ClipPolicy.CLIP_PATH)
                        .center(new ImageBuilder().source(pngBytes()).size(11, 11).anchor("logo").build())
                        .build(),
                // Nothing painted round it.
                new ShapeContainerBuilder().circle(22).clipPolicy(ClipPolicy.CLIP_PATH)
                        .center(new ImageBuilder().source(pngBytes()).size(11, 11).build())
                        .build(),
                // A photo filling its frame.
                new ShapeContainerBuilder().roundedRect(60, 60, 8).stroke(DocumentStroke.of(ACCENT, 1))
                        .clipPolicy(ClipPolicy.CLIP_PATH)
                        .center(new ImageBuilder().source(pngBytes()).size(60, 60).build())
                        .build());
        for (com.demcha.compose.document.node.DocumentNode container : kept) {
            try (XWPFDocument document = export(null, session -> session.add(container))) {
                assertThat(document.getDocument().xmlText()).as(container.toString()).contains("<wp:inline");
                assertThat(anchors(document.getDocument().xmlText())).noneMatch(anchor -> anchor.contains("<pic:pic"));
            }
        }
    }

    /** A navy disc holding an 11pt glyph, as a section title's badge. */
    private static com.demcha.compose.document.node.DocumentNode badge() {
        return new ShapeContainerBuilder().name("Badge").circle(22).fillColor(ACCENT)
                .clipPolicy(ClipPolicy.CLIP_PATH)
                .center(new ImageBuilder().name("Glyph").source(pngBytes()).size(11, 11).build())
                .build();
    }

    /** The stacking height of the one anchored drawing in the body whose XML carries a marker. */
    private static long relativeHeight(XWPFDocument document, String marker) {
        for (org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph : document.getParagraphs()) {
            for (org.apache.poi.xwpf.usermodel.XWPFRun run : paragraph.getRuns()) {
                for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing drawing
                        : run.getCTR().getDrawingList()) {
                    for (org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor anchor
                            : drawing.getAnchorList()) {
                        if (anchor.xmlText().contains(marker)) {
                            return anchor.getRelativeHeight();
                        }
                    }
                }
            }
        }
        throw new AssertionError("no anchored drawing carries " + marker);
    }

    @Test
    void theBodysDrawingsStandAboveThePageBackgrounds() throws Exception {
        // LibreOffice stacks a header's shapes and the body's together: a column's fill in the
        // header painted over the timeline rail drawn in the body.
        try (XWPFDocument document = export(null, session -> {
            session.pageBackground(DocumentColor.rgb(250, 250, 250));
            session.pageFlow(page -> page
                    .addEllipse(e -> e.name("First").circle(10).fillColor(ACCENT))
                    .addEllipse(e -> e.name("Second").circle(10).fillColor(ACCENT)));
        })) {
            // A page of its own draws its background from the body too (DocxPageBackgroundTest).
            List<org.apache.poi.xwpf.usermodel.XWPFParagraph> paragraphs = new ArrayList<>(document.getParagraphs());
            for (XWPFHeader part : document.getHeaderList()) {
                paragraphs.addAll(part.getParagraphs());
            }
            List<Long> backgrounds = new ArrayList<>();
            List<Long> shapes = new ArrayList<>();
            for (org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph : paragraphs) {
                for (org.apache.poi.xwpf.usermodel.XWPFRun run : paragraph.getRuns()) {
                    for (var drawing : run.getCTR().getDrawingList()) {
                        for (var anchor : drawing.getAnchorList()) {
                            boolean background = anchor.getDocPr().getName().startsWith("Page background");
                            (background ? backgrounds : shapes).add(anchor.getRelativeHeight());
                        }
                    }
                }
            }

            assertThat(backgrounds).hasSize(1);
            assertThat(shapes).hasSize(2).isSorted();
            assertThat(shapes.get(0)).isGreaterThan(backgrounds.get(0));
        }
    }

    @Test
    void aDrawingWithNoParagraphAfterItOnItsPageIsStillDrawn() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument document = export(report, session -> session.pageFlow(page -> page
                .addParagraph(p -> p.text("Only line"))
                .addEllipse(e -> e.name("Last").circle(20).fillColor(ACCENT))))) {
            assertThat(anchors(document.getDocument().xmlText())).singleElement()
                    .asString().contains("prst=\"ellipse\"");
            assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
        }
    }

    @Test
    void aShapeBelowTheLastTextOfItsPageIsAnchoredOnThatPage() throws Exception {
        // The dot is the last block of page 1: the next node starts on page 2, and a shape left
        // waiting for a paragraph on its page would find none and be dropped.
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument document = export(report, session -> session.pageFlow(page -> page
                .addParagraph(p -> p.text("Page one"))
                .addEllipse(e -> e.name("Dot").circle(20).fillColor(ACCENT))
                .addPageBreak(b -> { })
                .addParagraph(p -> p.text("Page two"))))) {
            assertThat(paragraphCarrying(document, "prst=\"ellipse\"")).isEqualTo("Page one");
            assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
        }
    }

    @Test
    void aShapeAtTheTopOfAPageWaitsForThatPagesFirstText() throws Exception {
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .addParagraph(p -> p.text("Page one"))
                .addPageBreak(b -> { })
                .addEllipse(e -> e.name("Dot").circle(20).fillColor(ACCENT))
                .addParagraph(p -> p.text("Page two"))))) {
            assertThat(paragraphCarrying(document, "prst=\"ellipse\"")).isEqualTo("Page two");
        }
    }

    @Test
    void aPageZonesDrawingIsNotDrawnAgainInTheBody() throws Exception {
        // A header's band is the header's: drawn in the body too, it stood over the header's text.
        try (XWPFDocument document = export(null, session -> {
            session.chrome().zone(com.demcha.compose.document.output.DocumentPageZone.header(30,
                    page -> new com.demcha.compose.document.dsl.ShapeBuilder()
                            .name("Band").size(200, 12).fillColor(ACCENT).build()));
            session.pageFlow(page -> page.addParagraph(p -> p.text("Body")));
        })) {
            assertThat(anchors(document.getDocument().xmlText())).isEmpty();
        }
    }

    @Test
    void aLastPageALongTableFillsCarriesItsShapesInTheBodyNotInARow() throws Exception {
        // Page 2 holds nothing but the table's later rows, then the dot. Word prints a shape
        // anchored in a cell clipped to the cell, so the paragraph closing the section carries
        // it, and no row does.
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument document = export(report, session -> session.pageFlow(page -> page
                .addTable(t -> {
                    t.columns(com.demcha.compose.document.table.DocumentTableColumn.auto());
                    for (int row = 0; row < 40; row++) {
                        t.row("Row " + row);
                    }
                })
                .addEllipse(e -> e.name("Dot").circle(20).fillColor(ACCENT))))) {
            assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
            assertThat(document.getTables().get(0).getCTTbl().xmlText()).doesNotContain("prst=\"ellipse\"");
            assertThat(paragraphCarrying(document, "prst=\"ellipse\"")).as("the closing paragraph").isEmpty();
        }
    }

    @Test
    void aFirstPageLaidOutInATableGetsABodyParagraphBeforeItForItsShapes() throws Exception {
        // Two columns written as one row: the page has no body paragraph, and a shape anchored
        // in the sidebar's cell was printed clipped to it. A hairline paragraph opens the page.
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .addLayerStack(stack -> stack
                        .layer(column("Side", 0, 260, side -> side.addParagraph(p -> p.text("Contact"))),
                                com.demcha.compose.document.node.LayerAlign.TOP_LEFT)
                        .layer(column("Main", 100, 0, main -> main
                                .addEllipse(e -> e.name("Ring").circle(40).fillColor(ACCENT))
                                .addParagraph(p -> p.text("Body"))),
                                com.demcha.compose.document.node.LayerAlign.TOP_LEFT))))) {
            List<org.apache.poi.xwpf.usermodel.IBodyElement> body = document.getBodyElements();

            assertThat(body.get(0)).isInstanceOf(org.apache.poi.xwpf.usermodel.XWPFParagraph.class);
            assertThat(((org.apache.poi.xwpf.usermodel.XWPFParagraph) body.get(0)).getCTP().xmlText())
                    .contains("prst=\"ellipse\"");
            assertThat(body.get(1)).as("the columns' row").isInstanceOf(org.apache.poi.xwpf.usermodel.XWPFTable.class);
            org.apache.poi.xwpf.usermodel.XWPFParagraph opening = (org.apache.poi.xwpf.usermodel.XWPFParagraph) body.get(0);
            assertThat(opening.getText()).as("a hairline holding nothing but the drawing").isEmpty();
            assertThat(opening.getCTP().getPPr().getSpacing().getLineRule()).hasToString("exact");
            assertThat(DocxTwips.of(opening.getCTP().getPPr().getSpacing().getLine())).isEqualTo(2L);
            assertThat(DocxTwips.of(opening.getCTP().getPPr().getSpacing().getBefore())).isZero();
            assertThat(DocxTwips.of(opening.getCTP().getPPr().getSpacing().getAfter())).isZero();
            assertThat(document.getTables().get(0).getCTTbl().xmlText()).doesNotContain("wp:anchor");
        }
    }

    @Test
    void aShapeInAPaintedPanelIsDrawnInFrontOfTheText() throws Exception {
        // Both editors paint a shaded cell over what lies behind the text.
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .addSection("Panel", s -> s.fillColor(DocumentColor.rgb(220, 230, 240))
                        .addParagraph(p -> p.text("In the panel"))
                        .addEllipse(e -> e.name("Dot").circle(20).fillColor(ACCENT)))))) {
            assertThat(anchors(document.getDocument().xmlText())).singleElement().asString()
                    .contains("behindDoc=\"0\"");
        }
    }

    @Test
    void aShapeInAPanelInAColumnIsDrawnInFrontToo() throws Exception {
        // Columns lay nothing over anything: a dot in a card in the main column shows.
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .addLayerStack(stack -> stack
                        .layer(column("Side", 0, 260, side -> side.addParagraph(p -> p.text("Contact"))),
                                com.demcha.compose.document.node.LayerAlign.TOP_LEFT)
                        .layer(column("Main", 100, 0, main -> main.addSection("Card", card -> card
                                        .fillColor(DocumentColor.rgb(220, 230, 240))
                                        .addParagraph(p -> p.text("Status"))
                                        .addEllipse(e -> e.name("Dot").circle(10).fillColor(ACCENT)))),
                                com.demcha.compose.document.node.LayerAlign.TOP_LEFT))))) {
            assertThat(anchors(document.getDocument().xmlText())).singleElement().asString()
                    .contains("behindDoc=\"0\"");
        }
    }

    @Test
    void aRingRoundAPhotoInAPaintedCardStaysBehindThePhoto() throws Exception {
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .addSection("Card", card -> card.fillColor(DocumentColor.rgb(220, 230, 240))
                        .add(new ShapeContainerBuilder().name("Portrait").circle(60)
                                .stroke(DocumentStroke.of(ACCENT, 1)).fillColor(ACCENT)
                                .center(new ImageBuilder().name("Photo").source(pngBytes()).size(60, 60).build())
                                .build()))))) {
            assertThat(anchors(document.getDocument().xmlText())).singleElement().asString()
                    .contains("behindDoc=\"1\"");
        }
    }

    @Test
    void aDiscAndItsInitialsInAPaintedPanelAreOneShapeInFront() throws Exception {
        // Behind the initials it frames, the disc was hidden under the panel's shading; written
        // apart from it, the initials parted from it wherever the editor set their line.
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .addSection("Card", s -> s.fillColor(DocumentColor.rgb(220, 230, 240))
                        .addParagraph(p -> p.text("Profile"))
                        .add(new ShapeContainerBuilder().name("Badge").circle(40).fillColor(ACCENT)
                                .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("JR").build())
                                .build()))))) {
            assertThat(anchors(document.getDocument().xmlText())).hasSize(1);
            String anchor = anchors(document.getDocument().xmlText()).get(0);
            assertThat(anchor).contains("behindDoc=\"0\"").contains("prst=\"ellipse\"")
                    .contains("<w:txbxContent>").contains(">JR<").contains("anchor=\"ctr\"")
                    // Unwrapped, Word shrinks the box to the width of its letters.
                    .contains("wrap=\"square\"");
            String body = document.getDocument().xmlText();
            assertThat(body.indexOf(">JR<")).as("the initials are the shape's alone, not a paragraph of the flow")
                    .isEqualTo(body.lastIndexOf(">JR<"));
            assertThat(allText(document)).as("still text a reader extracts").contains("JR");
        }
    }

    @Test
    void aBadgeOutsideAPanelHoldsItsInitialsToo() throws Exception {
        // ObsidianInvoice's footer disc: its "K" stood below the disc's corner in the flow. All its
        // cell holds, the disc is anchored in that cell, where it moves with its row.
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .addRow("Closing", row -> row.columns(com.demcha.compose.document.style.DocumentRowColumn.fixed(40),
                                com.demcha.compose.document.style.DocumentRowColumn.weight(1))
                        .add(new ShapeContainerBuilder().name("Disc").circle(28).fillColor(ACCENT)
                                .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("K").build())
                                .build())
                        .addParagraph(p -> p.text("Thank you for your business."))))) ) {
            assertThat(anchors(document.getDocument().xmlText())).singleElement().asString()
                    .contains("<w:txbxContent>").contains(">K<").contains("behindDoc=\"0\"")
                    .contains("layoutInCell=\"1\"")
                    .contains("<wp:positionV relativeFrom=\"paragraph\"><wp:posOffset>0</wp:posOffset>");
        }
    }

    @Test
    void aBadgeInALayerStackKeepsTheRoomThePageGivesIt() throws Exception {
        // Its initials are drawn, not written: measured round them, the stack held less than its
        // height and what followed ran up under the badge.
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .addParagraph(p -> p.text("Above"))
                .addLayerStack(stack -> stack.name("Mark")
                        .layer(new com.demcha.compose.document.dsl.SpacerBuilder().name("Room").size(88, 88).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .layer(new ShapeContainerBuilder().name("Monogram").circle(88).fillColor(ACCENT)
                                .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("AR").build())
                                .build(), LayerAlign.TOP_LEFT, 0))
                .addParagraph(p -> p.text("Below"))))) {
            // The room between the two paragraphs: every paragraph written between them, and the
            // space written above the second.
            List<org.apache.poi.xwpf.usermodel.XWPFParagraph> paragraphs = document.getParagraphs();
            int above = -1;
            int below = -1;
            for (int index = 0; index < paragraphs.size(); index++) {
                // It carries the badge's anchor, whose text box's text its own text reads too.
                if (paragraphs.get(index).getText().contains("Above")) {
                    above = index;
                } else if (paragraphs.get(index).getText().equals("Below")) {
                    below = index;
                }
            }
            long twips = 0;
            for (int index = above + 1; index <= below; index++) {
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing spacing =
                        paragraphs.get(index).getCTP().getPPr().getSpacing();
                twips += spacing.isSetBefore() ? ((Number) spacing.getBefore()).longValue() : 0;
                if (index < below) {
                    twips += ((Number) spacing.getLine()).longValue()
                             + (spacing.isSetAfter() ? ((Number) spacing.getAfter()).longValue() : 0);
                }
            }
            assertThat(twips).as("the badge's 88pt held between the paragraphs").isBetween(86L * 20, 90L * 20);
        }
    }

    @Test
    void aBadgesTextIsEscapedInItsShape() throws Exception {
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .add(new ShapeContainerBuilder().name("Mark").circle(30).fillColor(ACCENT)
                        .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("A&B").build())
                        .build())))) {
            assertThat(allText(document)).contains("A&B");
            assertThat(anchors(document.getDocument().xmlText())).singleElement().asString()
                    .contains("<w:txbxContent>").contains("A&amp;B</w:t>");
        }
    }

    @Test
    void aBadgesTextReachesTheEdgesOfItsOutline() throws Exception {
        // An ellipse wraps its text in the square inscribed in it: without insets reaching out
        // to the outline, Word broke "MWM" in a 36pt disc after "MW".
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .add(new ShapeContainerBuilder().name("Disc").circle(36).fillColor(ACCENT)
                        .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("MW").build())
                        .build())))) {
            long reach = Units.toEMU(36 * (1 - Math.sqrt(0.5)) / 2);

            assertThat(anchors(document.getDocument().xmlText())).singleElement().asString()
                    .as("reaching out by (1 - cos 45°) / 2 of the disc's width on every side")
                    .contains("lIns=\"-" + reach + "\"").contains("rIns=\"-" + reach + "\"")
                    .contains("tIns=\"-" + reach + "\"").contains("bIns=\"-" + reach + "\"");
        }
    }

    @Test
    void aBadgeItsShapeCannotSetAsThePageDoesIsWrittenAsBefore() throws Exception {
        com.demcha.compose.document.style.DocumentTextStyle gold = com.demcha.compose.document.style.DocumentTextStyle
                .builder().color(DocumentColor.rgb(200, 160, 40)).build();
        List<Consumer<ShapeContainerBuilder>> badges = List.of(
                // set in a corner, where the shape would centre it
                badge -> badge.topLeft(new ParagraphBuilder().text("Q1").build()),
                // two styles, where the shape holds one run
                badge -> badge.center(new ParagraphBuilder().inlineText("J").inlineText("R", gold).build()),
                // right to left, which the shape's paragraph does not say
                badge -> badge.center(new ParagraphBuilder().text("AB")
                        .direction(com.demcha.compose.document.node.TextDirection.RTL).build()));
        for (Consumer<ShapeContainerBuilder> spec : badges) {
            ShapeContainerBuilder badge = new ShapeContainerBuilder().name("Card").roundedRect(80, 40, 6).fillColor(ACCENT);
            spec.accept(badge);
            try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page.add(badge.build())))) {
                assertThat(document.getDocument().xmlText()).doesNotContain("txbxContent");
            }
        }
    }

    @Test
    void aRowOutsideAPaintedPanelIsNotHeldToThePagesHeight() throws Exception {
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .addRow("Pair", row -> row.columns(com.demcha.compose.document.style.DocumentRowColumn.weight(1),
                                com.demcha.compose.document.style.DocumentRowColumn.weight(1))
                        .addParagraph(p -> p.text("Left"))
                        .addParagraph(p -> p.text("Right")))))) {
            assertThat(document.getTables().get(0).getRow(0).getCtRow().getTrPr() == null
                       || document.getTables().get(0).getRow(0).getCtRow().getTrPr().sizeOfTrHeightArray() == 0)
                    .isTrue();
        }
    }

    @Test
    void aBadgeWithMoreThanInitialsIsWrittenAsBefore() throws Exception {
        // A pill with a word in it stays a written paragraph: its text is content, not a mark.
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .add(new ShapeContainerBuilder().name("Pill").roundedRect(120, 24, 12).fillColor(ACCENT)
                        .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("Overdue").build())
                        .build())))) {
            assertThat(document.getDocument().xmlText()).doesNotContain("txbxContent");
            assertThat(allText(document)).contains("Overdue");
        }
    }

    @Test
    void aLaterSectionOpeningWithATableGetsItsBodyParagraphBeforeThatTable() throws Exception {
        DocumentSession first = GraphCompose.document().pageSize(400, 400).margin(DocumentInsets.of(20)).create();
        first.pageFlow(page -> page.addParagraph(p -> p.text("Cover")));
        DocumentSession second = GraphCompose.document().pageSize(400, 400).margin(DocumentInsets.of(20)).create();
        second.pageFlow(page -> page.addLayerStack(stack -> stack
                .layer(column("Side", 0, 260, side -> side.addParagraph(p -> p.text("Contact"))),
                        com.demcha.compose.document.node.LayerAlign.TOP_LEFT)
                .layer(column("Main", 100, 0, main -> main
                                .addEllipse(e -> e.name("Ring").circle(40).fillColor(ACCENT))
                                .addParagraph(p -> p.text("Body"))),
                        com.demcha.compose.document.node.LayerAlign.TOP_LEFT)));
        byte[] docx;
        try (com.demcha.compose.document.api.MultiSectionDocument document = GraphCompose.documents()
                .section(first).section(second).create()) {
            docx = document.toDocxBytes();
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            List<org.apache.poi.xwpf.usermodel.IBodyElement> body = document.getBodyElements();
            int table = body.indexOf(document.getTables().get(0));

            assertThat(table).isGreaterThan(1);
            assertThat(((org.apache.poi.xwpf.usermodel.XWPFParagraph) body.get(table - 1)).getCTP().xmlText())
                    .as("the paragraph just before the second section's table")
                    .contains("prst=\"ellipse\"");
        }
    }

    private static String allText(XWPFDocument document) {
        try (var extractor = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(document)) {
            return extractor.getText();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static com.demcha.compose.document.node.DocumentNode column(String name, double left, double right,
                                                                       Consumer<com.demcha.compose.document.dsl.SectionBuilder> content) {
        com.demcha.compose.document.dsl.SectionBuilder layer = new com.demcha.compose.document.dsl.SectionBuilder();
        layer.name(name).spacing(0).padding(new DocumentInsets(0, right, 0, left));
        layer.addSection(name + "Content", content);
        return layer.build();
    }

    @Test
    void eachSectionAnchorsItsShapesOnItsOwnPages() throws Exception {
        // Pages are counted within a section: the second section's page 1 is not the first's.
        DocumentSession first = GraphCompose.document().pageSize(400, 400).margin(DocumentInsets.of(20)).create();
        first.pageFlow(page -> page.addParagraph(p -> p.text("First section")));
        DocumentSession second = GraphCompose.document().pageSize(400, 400).margin(DocumentInsets.of(20)).create();
        second.pageFlow(page -> page
                .addEllipse(e -> e.name("Dot").circle(20).fillColor(ACCENT))
                .addParagraph(p -> p.text("Second section")));
        byte[] docx;
        try (com.demcha.compose.document.api.MultiSectionDocument document = GraphCompose.documents()
                .section(first).section(second).create()) {
            docx = document.toDocxBytes();
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(paragraphCarrying(document, "prst=\"ellipse\"")).isEqualTo("Second section");
        }
    }

    @Test
    void theReportSaysWhatAShapeLosesInAPanelOrUnderATransform() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument ignored = export(report, session -> session.pageFlow(page -> page
                .addSection("Panel", s -> s.fillColor(DocumentColor.rgb(220, 230, 240))
                        .addParagraph(p -> p.text("In the panel"))
                        .addEllipse(e -> e.name("InPanel").circle(20).fillColor(ACCENT)))
                .addEllipse(e -> e.name("Turned").size(30, 10).fillColor(ACCENT)
                        .transform(com.demcha.compose.document.style.DocumentTransform.rotate(45)))))) {
            List<String> messages = report.get().notes().stream()
                    .filter(note -> note.severity() == DocxExportReport.Severity.APPROXIMATED)
                    .map(DocxExportReport.Note::detail)
                    .toList();
            assertThat(messages).anyMatch(message -> message.contains("shading"))
                    .anyMatch(message -> message.contains("transform is not carried"));
        }
    }

    @Test
    void aStarOutlineIsDrawnAsCustomGeometry() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument document = export(report, session -> session.add(new ShapeContainerBuilder()
                .name("Star")
                .star(60, 60)
                .fillColor(ACCENT)
                .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("New").build())
                .build()))) {
            assertThat(anchors(document.getDocument().xmlText())).singleElement().asString()
                    .contains("<a:custGeom>").contains("<a:close/>").contains("srgbClr val=\"1A5694\"");
            assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
        }
    }

    @Test
    void aFilledPathKeepsItsCurvesAndEverySubpath() throws Exception {
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .addPath(path -> path.name("Drop").size(40, 40)
                        .moveTo(0.5, 1).curveTo(1, 0.5, 0.75, 0, 0.5, 0).closePath()
                        .moveTo(0.1, 0.1).lineTo(0.2, 0.1).lineTo(0.2, 0.2).closePath()
                        .fillColor(ACCENT))))) {
            String anchor = anchors(document.getDocument().xmlText()).get(0);

            assertThat(anchor).contains("<a:cubicBezTo><a:pt x=\"100000\" y=\"50000\"/>"
                                        + "<a:pt x=\"75000\" y=\"100000\"/><a:pt x=\"50000\" y=\"100000\"/></a:cubicBezTo>")
                    .doesNotContain("fill=\"none\"")
                    .contains("<a:solidFill><a:srgbClr val=\"1A5694\"/></a:solidFill>");
            assertThat(anchor.split("<a:moveTo>", -1)).as("two subpaths").hasSize(3);
            assertThat(anchor.split("<a:close/>", -1)).hasSize(3);
        }
    }

    @Test
    void theReportNamesWhatADashedPathLoses() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument ignored = export(report, session -> session.pageFlow(page -> page
                .addPath(path -> path.name("Dashed").size(40, 10).moveTo(0, 0.5).lineTo(1, 0.5)
                        .stroke(DocumentStroke.of(ACCENT, 1)).dashed(3, 2))))) {
            assertThat(report.get().notes()).extracting(DocxExportReport.Note::detail)
                    .anyMatch(detail -> detail.contains("dash pattern"));
        }
    }

    @Test
    void aPathIsDrawnThroughItsPointsWithTheBoxTurnedTheWayDrawingMlCounts() throws Exception {
        // The engine counts y up from the bottom of the box, DrawingML down from the top.
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .addPath(path -> path.name("Triangle").size(40, 20)
                        .moveTo(0, 0).lineTo(1, 0).lineTo(0.5, 1).closePath()
                        .stroke(DocumentStroke.of(ACCENT, 1)))))) {
            String anchor = anchors(document.getDocument().xmlText()).get(0);

            assertThat(anchor).contains("<a:moveTo><a:pt x=\"0\" y=\"100000\"/></a:moveTo>")
                    .contains("<a:lnTo><a:pt x=\"100000\" y=\"100000\"/></a:lnTo>")
                    .contains("<a:lnTo><a:pt x=\"50000\" y=\"0\"/></a:lnTo>")
                    .contains("fill=\"none\"")
                    .contains("cx=\"" + Units.toEMU(40) + "\"");
        }
    }

    /** The text of the body paragraph whose XML holds a marker. */
    private static String paragraphCarrying(XWPFDocument document, String marker) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getCTP().xmlText().contains(marker))
                .map(org.apache.poi.xwpf.usermodel.XWPFParagraph::getText)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void aShapeAloneOnTheLastPageGetsAParagraphOfItsOwn() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument document = export(report, session -> session.pageFlow(page -> page
                .addParagraph(p -> p.text("Page one"))
                .addPageBreak(b -> { })
                .addEllipse(e -> e.name("Dot").circle(20).fillColor(ACCENT))))) {
            assertThat(paragraphCarrying(document, "prst=\"ellipse\"")).as("an empty carrier").isEmpty();
            assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
        }
    }

    private static PlacedFragment fragmentNamed(DocumentSession session, String name) {
        return session.layoutGraph().fragments().stream()
                .filter(fragment -> fragment.path() != null && fragment.path().contains(name))
                .findFirst()
                .orElseThrow();
    }

    /** Every anchored drawing in a part's XML, in document order. */
    private static List<String> anchors(String xml) {
        List<String> anchors = new ArrayList<>();
        Matcher matcher = Pattern.compile("<wp:anchor .*?</wp:anchor>", Pattern.DOTALL).matcher(xml);
        while (matcher.find()) {
            anchors.add(matcher.group());
        }
        return anchors;
    }

    /** The preset geometry of every picture in a part's XML. */
    private static List<String> pictureGeometry(String xml) {
        List<String> shapes = new ArrayList<>();
        Matcher matcher = Pattern.compile("<pic:spPr>.*?prst=\"(\\w+)\"", Pattern.DOTALL).matcher(xml);
        while (matcher.find()) {
            shapes.add(matcher.group(1));
        }
        return shapes;
    }

    /** The stacking height of every anchored drawing in some paragraphs, as written. */
    private static List<Long> heights(List<org.apache.poi.xwpf.usermodel.XWPFParagraph> paragraphs) {
        List<Long> heights = new ArrayList<>();
        for (org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph : paragraphs) {
            for (org.apache.poi.xwpf.usermodel.XWPFRun run : paragraph.getRuns()) {
                for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing drawing
                        : run.getCTR().getDrawingList()) {
                    for (org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor anchor
                            : drawing.getAnchorList()) {
                        heights.add(anchor.getRelativeHeight());
                    }
                }
            }
        }
        return heights;
    }

    private static XWPFDocument export(AtomicReference<DocxExportReport> report,
                                       Consumer<DocumentSession> content) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            content.accept(session);
            byte[] docx = session.export(report == null
                    ? new DocxSemanticBackend()
                    : new DocxSemanticBackend(report::set));
            return new XWPFDocument(new ByteArrayInputStream(docx));
        }
    }

    private static byte[] pngBytes() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
