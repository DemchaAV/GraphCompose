package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A stack of overlapping layers is written as one band, as the page places its content.
 *
 * <p>A CV sidebar opens with a monogram: a spacer as tall as the badge keeps the badge's place
 * in the flow, and the badge — a ring, drawn, and the initials centred in it — is laid over the
 * spacer. Written layer after layer, the initials came after the spacer's full height, 60pt
 * below where the page draws them, and the sidebar under them with it.</p>
 */
class DocxOverlayBandTest {

    private static final double BADGE = 80;

    @Test
    void aBadgeOverItsPlaceHolderIsWrittenWhereThePageDrawsIt() throws Exception {
        try (Export export = export()) {
            List<XWPFParagraph> paragraphs = export.document().getParagraphs();
            List<String> texts = paragraphs.stream().map(XWPFParagraph::getText).toList();

            assertThat(texts).as("the spacer that holds the badge's place is not written")
                    .containsExactly("Above", "JR", "Below");
            PlacedNode stack = export.placed("Frame");
            PlacedNode initials = export.placed("Initials");
            double above = (stack.placementY() + stack.placementHeight())
                           - (initials.placementY() + initials.placementHeight());
            double below = initials.placementY() - stack.placementY();
            assertThat(before(paragraphs.get(1))).as("the initials sit where the ring centres them")
                    .isCloseTo(Math.round((6 + above) * 20), org.assertj.core.data.Offset.offset(2L));
            assertThat(before(paragraphs.get(2))).as("and the badge keeps its height below them")
                    .isCloseTo(Math.round((below + 12) * 20), org.assertj.core.data.Offset.offset(2L));
        }
    }

    @Test
    void aBandNestedUnderADrawingIsMeasuredPastItsOwnPlaceHolder() throws Exception {
        // A background drawn behind the frame: the outer band's first block is the initials,
        // not the spacer that holds the badge's place in the frame.
        DocumentSession session = GraphCompose.document().pageSize(300, 500).margin(DocumentInsets.of(20)).create();
        session.pageFlow(page -> page
                .addParagraph(p -> p.text("Above").margin(DocumentInsets.bottom(6)))
                .addLayerStack(outer -> outer
                        .name("Outer")
                        .back(new com.demcha.compose.document.dsl.ShapeBuilder().name("Backdrop").size(200, BADGE).build())
                        .layer(frame(), LayerAlign.TOP_LEFT))
                .addParagraph(p -> p.text("Below")));
        try (session; XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(
                session.export(new DocxSemanticBackend())))) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            PlacedNode stack = session.layoutGraph().nodes().stream()
                    .filter(node -> "Outer".equals(node.semanticName())).findFirst().orElseThrow();
            PlacedNode initials = session.layoutGraph().nodes().stream()
                    .filter(node -> "Initials".equals(node.semanticName())).findFirst().orElseThrow();
            double above = (stack.placementY() + stack.placementHeight())
                           - (initials.placementY() + initials.placementHeight());
            double below = initials.placementY() - stack.placementY();

            assertThat(paragraphs).extracting(XWPFParagraph::getText).containsExactly("Above", "JR", "Below");
            assertThat(before(paragraphs.get(1))).as("the initials where the ring centres them")
                    .isCloseTo(Math.round((6 + above) * 20), org.assertj.core.data.Offset.offset(2L));
            assertThat(before(paragraphs.get(2))).as("and the badge's height below them")
                    .isCloseTo(Math.round(below * 20), org.assertj.core.data.Offset.offset(2L));
        }
    }

    @Test
    void theInitialsStandAcrossWhereTheRingCentresThem() throws Exception {
        // Written one after the other, the layers ran from the stack's left edge: the initials
        // the page centres in the ring stood at the left of the column.
        try (Export export = export()) {
            XWPFParagraph initials = export.document().getParagraphs().get(1);
            PlacedNode frame = export.placed("Frame");
            PlacedNode placed = export.placed("Initials");
            double fromLeft = placed.placementX() - frame.placementX();
            double fromRight = frame.placementX() + frame.placementWidth()
                               - (placed.placementX() + placed.placementWidth());
            double slack = Math.max(2, placed.placementWidth() * 0.05);

            // The layer centres the paragraph; its text is set from the left of its box, so the
            // slack goes on the right and the text starts where the page starts it.
            assertThat(DocxTwips.of(initials.getCTP().getPPr().getInd().getLeft()))
                    .as("held in to where the ring centres the initials")
                    .isCloseTo(Math.round(fromLeft * 20), org.assertj.core.data.Offset.offset(2L));
            // The paragraph's own box leaves a few points to spare; the badge round it adds none.
            assertThat(DocxTwips.of(initials.getCTP().getPPr().getInd().getRight()))
                    .isCloseTo(Math.round((fromRight - slack) * 20), org.assertj.core.data.Offset.offset(2L));
        }
    }

    @Test
    void aShapeContainersLayerStandsAcrossWhereThePagePutsIt() throws Exception {
        // A section title set beside its badge: the title starts after the badge, not under it.
        DocumentSession session = GraphCompose.document().pageSize(300, 500).margin(DocumentInsets.of(20)).create();
        session.pageFlow(page -> page.add(new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .name("Header").rectangle(260, 24)
                .clipPolicy(com.demcha.compose.document.style.ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ParagraphBuilder().name("Title").text("EXPERIENCE").build(), 30, 0, LayerAlign.CENTER_LEFT)
                .build()));
        try (session; XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(
                session.export(new DocxSemanticBackend())))) {
            XWPFParagraph title = document.getParagraphs().stream()
                    .filter(paragraph -> "EXPERIENCE".equals(paragraph.getText())).findFirst().orElseThrow();

            assertThat(DocxTwips.of(title.getCTP().getPPr().getInd().getLeft()))
                    .as("30pt in, where the page sets the title").isCloseTo(600L, org.assertj.core.data.Offset.offset(2L));
        }
    }

    @Test
    void aShapeContainersLayerIsMeasuredFromInsideItsPadding() throws Exception {
        DocumentSession session = GraphCompose.document().pageSize(300, 500).margin(DocumentInsets.of(20)).create();
        session.pageFlow(page -> page.add(new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .name("Header").rectangle(240, 44).padding(DocumentInsets.of(10))
                .clipPolicy(com.demcha.compose.document.style.ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ParagraphBuilder().name("Title").text("EXPERIENCE").build(), 30, 0, LayerAlign.CENTER_LEFT)
                .build()));
        try (session; XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(
                session.export(new DocxSemanticBackend())))) {
            XWPFParagraph title = document.getParagraphs().stream()
                    .filter(paragraph -> "EXPERIENCE".equals(paragraph.getText())).findFirst().orElseThrow();
            PlacedNode header = session.layoutGraph().nodes().stream()
                    .filter(node -> "Header".equals(node.semanticName())).findFirst().orElseThrow();
            PlacedNode placed = session.layoutGraph().nodes().stream()
                    .filter(node -> "Title".equals(node.semanticName())).findFirst().orElseThrow();

            assertThat(DocxTwips.of(title.getCTP().getPPr().getInd().getLeft()))
                    .as("where the page sets the title, measured from the container's own edge")
                    .isCloseTo(Math.round((placed.placementX() - header.placementX()) * 20),
                            org.assertj.core.data.Offset.offset(2L));
        }
    }

    @Test
    void aDrawingThatIsNotWrittenStillTakesItsRoom() throws Exception {
        // A portrait drawn as a path, in the flow and inside a layer stack alike: none of it
        // reaches the file, and its height is still space above what follows.
        DocumentSession session = GraphCompose.document().pageSize(300, 500).margin(DocumentInsets.of(20)).create();
        session.pageFlow(page -> page
                .addParagraph(p -> p.text("Above"))
                // An icon is itself a stack of paths, inside the portrait's stack: held once.
                .addLayerStack(stack -> stack.name("Portrait")
                        .layer(new LayerStackBuilder().name("Icon")
                                .layer(new com.demcha.compose.document.dsl.PathBuilder().name("Face").size(50, 50)
                                        .moveTo(0, 0).lineTo(1, 0).lineTo(0.5, 1).closePath().build())
                                .layer(new com.demcha.compose.document.dsl.PathBuilder().name("Hair").size(50, 20)
                                        .moveTo(0, 0).lineTo(1, 0).lineTo(0.5, 1).closePath().build())
                                .build()))
                .addParagraph(p -> p.text("Below")));
        try (session; XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(
                session.export(new DocxSemanticBackend())))) {
            XWPFParagraph below = document.getParagraphs().stream()
                    .filter(paragraph -> "Below".equals(paragraph.getText())).findFirst().orElseThrow();

            assertThat(before(below)).isEqualTo(50L * 20);
        }
    }

    @Test
    void aCellOfDrawingAloneHoldsAHairline() throws Exception {
        // Nothing in the cell is written, and no space is carried into it: the paragraph a cell
        // ends with is a hairline, not a line of the document's font beside a shorter label.
        // MidnightNavy's skill row: held in a layer, so it can sit in its column's section.
        com.demcha.compose.document.dsl.SectionBuilder skill = new com.demcha.compose.document.dsl.SectionBuilder();
        skill.name("SkillHolder").addRow("SkillRow", row -> row
                .verticalAlign(com.demcha.compose.document.node.RowVerticalAlign.CENTER)
                .columns(com.demcha.compose.document.style.DocumentRowColumn.weight(1),
                        com.demcha.compose.document.style.DocumentRowColumn.fixed(60))
                .addParagraph(p -> p.text("Beside"))
                .addSection("MeterCell", cell -> cell.addLayerStack(stack -> stack.name("Meter")
                        .layer(new com.demcha.compose.document.dsl.LineBuilder().name("Track").horizontal(60)
                                .thickness(1).color(DocumentColor.rgb(120, 120, 120)).build(), LayerAlign.CENTER_LEFT, 0)
                        .position(new com.demcha.compose.document.dsl.ShapeBuilder().name("Knob").size(6, 4)
                                .fillColor(DocumentColor.rgb(0, 0, 0)).build(), 30, 0, LayerAlign.CENTER_LEFT, 1))));
        com.demcha.compose.document.node.DocumentNode held = skill.build();
        try (XWPFDocument document = DocxExports.withLayout(300, 500, 20, page -> page
                .addSection("Column", column -> column.addLayerStack(stack -> stack.name("SkillLayer")
                        .layer(held, LayerAlign.TOP_LEFT, 0))))) {
            XWPFParagraph holder = document.getTables().get(0).getRow(0).getCell(1).getParagraphs().get(0);
            CTPPr properties = holder.getCTP().getPPr();

            assertThat(holder.getRuns()).as("nothing of the meter is written in its cell").isEmpty();
            assertThat(properties).as("the cell's paragraph is written").isNotNull();
            assertThat(properties.getSpacing().getLineRule())
                    .isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.EXACT);
            assertThat(DocxTwips.of(properties.getSpacing().getLine())).isEqualTo(2L);
        }
    }

    @Test
    void aShapeContainersStackedLinesStandWhereTheOutlineSetsThem() throws Exception {
        // Two initials centred in a ring, one above the other: the space above the first and
        // below the last is the ring's, as for a single layer.
        DocumentSession session = GraphCompose.document().pageSize(300, 500).margin(DocumentInsets.of(20)).create();
        session.pageFlow(page -> page.add(new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                        .name("Ring").circle(76)
                        .stroke(DocumentStroke.of(DocumentColor.rgb(0, 0, 0), 1))
                        .position(new ParagraphBuilder().name("First").text("A")
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(24)).build(), 0, -13, LayerAlign.CENTER)
                        .position(new ParagraphBuilder().name("Second").text("M")
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(24)).build(), 0, 13, LayerAlign.CENTER)
                        .build())
                .addParagraph(p -> p.name("Below").text("Below")));
        try (session; XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(
                session.export(new DocxSemanticBackend())))) {
            PlacedNode ring = placed(session, "Ring");
            PlacedNode first = placed(session, "First");
            PlacedNode second = placed(session, "Second");
            PlacedNode next = placed(session, "Below");
            double above = (ring.placementY() + ring.placementHeight()) - (first.placementY() + first.placementHeight());
            double below = second.placementY() - ring.placementY();
            double gap = ring.placementY() - (next.placementY() + next.placementHeight());
            XWPFParagraph initial = document.getParagraphs().stream()
                    .filter(paragraph -> "A".equals(paragraph.getText())).findFirst().orElseThrow();
            XWPFParagraph after = document.getParagraphs().stream()
                    .filter(paragraph -> "Below".equals(paragraph.getText())).findFirst().orElseThrow();

            assertThat(above).as("the initials stand inside the ring").isGreaterThan(5);
            assertThat(before(initial)).as("written from where the ring sets the first initial")
                    .isCloseTo(Math.round(above * 20), org.assertj.core.data.Offset.offset(2L));
            assertThat(before(after)).as("and the ring's foot as far under the last as the page has it")
                    .isCloseTo(Math.round((below + gap) * 20), org.assertj.core.data.Offset.offset(2L));
        }
    }

    private static PlacedNode placed(DocumentSession session, String name) {
        return session.layoutGraph().nodes().stream()
                .filter(node -> name.equals(node.semanticName())).findFirst().orElseThrow();
    }

    @Test
    void aCellOfSpaceAndDrawingKeepsItsHeight() throws Exception {
        // A masthead's hairline column: padding round a vertical line. Nothing in it is
        // written, and the row is as tall as it only if its space is still there.
        try (XWPFDocument document = DocxExports.withLayout(300, 500, 20, page -> page
                .addRow(row -> row
                        .addSection("Hairline", cell -> cell.spacing(0)
                                .padding(new DocumentInsets(10, 0, 10, 0))
                                .addLine(line -> line.name("Rule").vertical(40).thickness(1)
                                        .color(DocumentColor.rgb(0, 0, 0))))
                        .addParagraph(p -> p.text("Beside"))))) {
            var cell = document.getTables().get(0).getRow(0).getCell(0);
            XWPFParagraph holder = cell.getParagraphs().get(0);

            assertThat(before(holder)).as("10 above, the line's 40, 10 below").isEqualTo(60L * 20);
        }
    }

    @Test
    void aLayersNegativeBottomEdgePullsNothingOutOfTheBand() throws Exception {
        // The layers overlap on the page: a pull out of one's foot moves neither the next layer
        // nor what follows the band, which the page measures from their boxes.
        assertThat(beforesWithLayerBottoms(-4)).isEqualTo(beforesWithLayerBottoms(0));
    }

    @Test
    void theLastLayersNegativeBottomEdgeIsTakenOnceBelowTheBand() throws Exception {
        // The band measures the space below it from that layer's box: the text running 4pt past
        // the stack's foot takes 4pt off the 10pt above the next paragraph, and no more.
        DocumentSession session = GraphCompose.document().pageSize(300, 500).margin(DocumentInsets.of(20)).create();
        session.pageFlow(page -> page
                .addLayerStack(stack -> stack.name("Stack")
                        .back(new SpacerNode("Space", 200, BADGE, DocumentInsets.zero(), DocumentInsets.zero()))
                        .layer(new ParagraphBuilder().name("Low").text("Low")
                                .margin(new DocumentInsets(0, 0, -4, 0)).build(), LayerAlign.BOTTOM_LEFT))
                .addParagraph(p -> p.text("Below").margin(DocumentInsets.top(10))));
        try (session; XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(
                session.export(new DocxSemanticBackend())))) {
            XWPFParagraph below = document.getParagraphs().stream()
                    .filter(paragraph -> "Below".equals(paragraph.getText())).findFirst().orElseThrow();

            assertThat(before(below)).isEqualTo((10 - 4) * 20L);
        }
    }

    @Test
    void textHangingBelowABandTakesItsPlaceOutOfTheSpaceAboveTheNextBand() throws Exception {
        // ConsultingInvoice's address runs past its band, and the contact band under it opened at
        // the gap whole: the band's first block resumes the space the page measures, and the
        // text hanging into it from above already stands in it.
        DocumentSession session = GraphCompose.document().pageSize(300, 500).margin(DocumentInsets.of(20)).create();
        session.pageFlow(page -> page
                .spacing(10)
                .addLayerStack(stack -> stack.name("First")
                        .back(new SpacerNode("Space", 200, BADGE, DocumentInsets.zero(), DocumentInsets.zero()))
                        .layer(new ParagraphBuilder().name("Low").text("Low")
                                .margin(new DocumentInsets(0, 0, -4, 0)).build(), LayerAlign.BOTTOM_LEFT))
                .addLayerStack(stack -> stack.name("Second")
                        .back(new SpacerNode("Room", 200, 40, DocumentInsets.zero(), DocumentInsets.zero()))
                        .layer(new ParagraphBuilder().name("Next").text("Next").build(), LayerAlign.TOP_LEFT)));
        try (session; XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(
                session.export(new DocxSemanticBackend())))) {
            XWPFParagraph next = document.getParagraphs().stream()
                    .filter(paragraph -> "Next".equals(paragraph.getText())).findFirst().orElseThrow();

            assertThat(before(next)).as("the 10pt gap, less the 4pt the text above hangs into it")
                    .isEqualTo((10 - 4) * 20L);
        }
    }

    @Test
    void aBandOpeningTheNextPageLeavesTheHangAboveOnItsPage() throws Exception {
        // 360pt, the gap and the first stack leave the second no room: it opens the next page,
        // and what the first stack's text hangs below it stays on the page above.
        DocumentSession session = GraphCompose.document().pageSize(300, 500).margin(DocumentInsets.of(20)).create();
        session.pageFlow(page -> page
                .spacing(10)
                .spacer(0, 360)
                .addLayerStack(stack -> stack.name("First")
                        .back(new SpacerNode("Space", 200, BADGE, DocumentInsets.zero(), DocumentInsets.zero()))
                        .layer(new ParagraphBuilder().name("Low").text("Low")
                                .margin(new DocumentInsets(0, 0, -4, 0)).build(), LayerAlign.BOTTOM_LEFT))
                .addLayerStack(stack -> stack.name("Second")
                        .back(new SpacerNode("Room", 200, 40, DocumentInsets.zero(), DocumentInsets.zero()))
                        .layer(new ParagraphBuilder().name("Next").text("Next").build(), LayerAlign.TOP_LEFT)));
        assertThat(session.layoutGraph().nodes().stream()
                .filter(node -> "Second".equals(node.semanticName())).findFirst().orElseThrow().startPage())
                .as("the second stack opens the next page").isEqualTo(1);
        try (session; XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(
                session.export(new DocxSemanticBackend())))) {
            XWPFParagraph next = document.getParagraphs().stream()
                    .filter(paragraph -> "Next".equals(paragraph.getText())).findFirst().orElseThrow();

            assertThat(before(next)).as("the gap whole, the hang left above").isEqualTo(10 * 20L);
        }
    }

    @Test
    void aLayersNegativeBottomEdgeInAShapeContainerMovesNothingBelowIt() throws Exception {
        // The outline is its own size; the layer's shorter box is already in the space below it.
        assertThat(beforesWithLayerBottoms(-4).get("After")).isEqualTo(beforesWithLayerBottoms(0).get("After"));
    }

    private static java.util.Map<String, Long> beforesWithLayerBottoms(double bottom) throws Exception {
        DocumentInsets edge = new DocumentInsets(0, 0, bottom, 0);
        DocumentSession session = GraphCompose.document().pageSize(300, 500).margin(DocumentInsets.of(20)).create();
        session.pageFlow(page -> page
                .addParagraph(p -> p.text("Above"))
                .addLayerStack(stack -> stack.name("Stack")
                        .back(new SpacerNode("Space", 200, BADGE, DocumentInsets.zero(), DocumentInsets.zero()))
                        .layer(new ParagraphBuilder().name("Top").text("Top").margin(edge).build(), LayerAlign.TOP_LEFT)
                        .layer(new ParagraphBuilder().name("Low").text("Low").build(), LayerAlign.BOTTOM_LEFT))
                .addParagraph(p -> p.text("Below"))
                .add(new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                        .name("Header").rectangle(240, 60)
                        .clipPolicy(com.demcha.compose.document.style.ClipPolicy.OVERFLOW_VISIBLE)
                        .position(new ParagraphBuilder().name("Title").text("TITLE").margin(edge).build(),
                                0, 0, LayerAlign.TOP_LEFT)
                        .build())
                .addParagraph(p -> p.text("After")));
        try (session; XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(
                session.export(new DocxSemanticBackend())))) {
            java.util.Map<String, Long> befores = new java.util.LinkedHashMap<>();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                befores.put(paragraph.getText(), before(paragraph));
            }
            assertThat(befores).containsKeys("Low", "Below", "After");
            return befores;
        }
    }

    private static long before(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetBefore()) {
            return 0;
        }
        return DocxTwips.of(properties.getSpacing().getBefore());
    }

    /** The badge over the spacer that keeps its place: a band of its own. */
    private static LayerStackNode frame() {
        LayerStackNode badge = new LayerStackBuilder()
                .name("Badge")
                .back(new EllipseBuilder().name("Ring").size(BADGE, BADGE)
                        .stroke(DocumentStroke.of(DocumentColor.rgb(0, 0, 0), 1)).build())
                .layer(new ParagraphBuilder().name("Initials").text("JR")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(28)).build(), LayerAlign.CENTER)
                .build();
        return new LayerStackBuilder()
                .name("Frame")
                .back(new SpacerNode("Space", 200, BADGE, DocumentInsets.zero(), DocumentInsets.zero()))
                .layer(badge, LayerAlign.TOP_CENTER)
                .build();
    }

    private static Export export() throws Exception {
        LayerStackNode badge = new LayerStackBuilder()
                .name("Badge")
                .back(new EllipseBuilder().name("Ring").size(BADGE, BADGE)
                        .stroke(DocumentStroke.of(DocumentColor.rgb(0, 0, 0), 1)).build())
                .layer(new ParagraphBuilder().name("Initials").text("JR")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(28)).build(), LayerAlign.CENTER)
                .build();
        DocumentSession session = GraphCompose.document().pageSize(300, 500).margin(DocumentInsets.of(20)).create();
        session.pageFlow(page -> page
                .addParagraph(p -> p.text("Above").margin(DocumentInsets.bottom(6)))
                .addLayerStack(frame -> frame
                        .name("Frame")
                        .margin(DocumentInsets.bottom(12))
                        .back(new SpacerNode("Space", 200, BADGE, DocumentInsets.zero(), DocumentInsets.zero()))
                        .layer(badge, LayerAlign.TOP_CENTER))
                .addParagraph(p -> p.text("Below")));
        XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend())));
        return new Export(session, document);
    }

    private record Export(DocumentSession session, XWPFDocument document) implements AutoCloseable {

        PlacedNode placed(String name) {
            return session.layoutGraph().nodes().stream()
                    .filter(node -> name.equals(node.semanticName()))
                    .findFirst()
                    .orElseThrow();
        }

        @Override
        public void close() throws Exception {
            document.close();
            session.close();
        }
    }
}
