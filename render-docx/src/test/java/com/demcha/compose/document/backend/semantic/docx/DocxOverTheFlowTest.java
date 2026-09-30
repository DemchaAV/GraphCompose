package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A container the page gives no room — its margins take back its whole height — is laid over
 * the flow, and its text is set in text boxes where the page sets it rather than in the flow.
 *
 * <p>{@code LumaStudioInvoice}'s sidebar is one: pulled up over the page's top margin and handing
 * its height back below, it holds the brand block and the lockup's five lines. Written in the
 * flow, those lines pushed the masthead beside them down in Word and the invoice onto a second
 * page.</p>
 */
class DocxOverTheFlowTest {

    private static final double STUB = 120;

    @Test
    void theTextOfAContainerGivenNoRoomIsSetInTextBoxesNotInTheFlow() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page
                .add(sidebar(new DocumentInsets(-40, 0, 40 - STUB, -40)))
                .addParagraph("Masthead"))) {
            String body = document.getDocument().getBody().xmlText();

            assertThat(document.getParagraphs()).extracting(DocxOverTheFlowTest::ownText)
                    .as("nothing of the lockup is written in the flow").doesNotContain("L", "&Co.")
                    .contains("Masthead");
            assertThat(body).as("its lines are in text boxes").contains("<w:txbxContent>")
                    .contains(">&amp;Co.<");
        }
    }

    @Test
    void theFlowAfterItStandsAsIfItWereNotThere() throws Exception {
        try (XWPFDocument withSidebar = DocxExports.withLayout(400, 600, 40, page -> page
                .add(sidebar(new DocumentInsets(-40, 0, 40 - STUB, -40)))
                .addParagraph(p -> p.text("Masthead").margin(DocumentInsets.top(6))));
             XWPFDocument without = DocxExports.withLayout(400, 600, 40, page -> page
                     .addParagraph(p -> p.text("Masthead").margin(DocumentInsets.top(6))))) {
            assertThat(before(masthead(withSidebar))).as("the space above the masthead is its own")
                    .isEqualTo(before(masthead(without)));
            assertThat(withSidebar.getBodyElements().get(0)).as("and nothing stands above it in the flow")
                    .isSameAs(masthead(withSidebar));
        }
    }

    @Test
    void aLineIsGivenRoomOnTheSideItsTextDoesNotLeanOn() throws Exception {
        // A right-to-left line is set against the page's right side as a right-aligned one is,
        // so it is given its room on the left too.
        double left = boxLeft(p -> p.text("Studio").align(com.demcha.compose.document.node.TextAlign.LEFT));
        double right = boxLeft(p -> p.text("Studio").align(com.demcha.compose.document.node.TextAlign.RIGHT));
        double rightToLeft = boxLeft(p -> p.text("Studio")
                .direction(com.demcha.compose.document.node.TextDirection.RTL));

        assertThat(right).as("a right-aligned line's box reaches out to the left").isLessThan(left);
        assertThat(rightToLeft).as("and a right-to-left one's the same way").isEqualTo(right);
    }

    @Test
    void inAPaintedPanelItIsWrittenAsBefore() throws Exception {
        // The panel's shading is painted over a box behind the text: the lockup stays in the flow.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page
                .addSection("Card", card -> card.fillColor(DocumentColor.rgb(230, 230, 230))
                        .add(sidebar(new DocumentInsets(0, 0, -STUB, 0))).addParagraph("Masthead")))) {
            assertThat(document.getDocument().getBody().xmlText()).as("no text box, the lockup in the cell")
                    .doesNotContain("<w:txbxContent>").contains(">&amp;Co.<");
        }
    }

    @Test
    void aLinkAnAnchorOrAPictureKeepsItInTheFlow() throws Exception {
        // Each is something the flow carries and a text box does not: the whole node is
        // written as before, its text in the flow.
        assertThat(floats(new ParagraphBuilder().name("Site").text("Site")
                .linkTarget(com.demcha.compose.document.node.DocumentLinkTarget.external("https://example.org"))
                .build())).as("a link").isFalse();
        assertThat(floats(new ParagraphBuilder().name("Named").text("Named").anchor("brand").build()))
                .as("an anchor").isFalse();
        assertThat(floats(new com.demcha.compose.document.dsl.SectionBuilder().name("Held").anchor("held")
                .addParagraph("Held").build())).as("a block with an anchor").isFalse();
        assertThat(floats(new com.demcha.compose.document.dsl.ImageBuilder().name("Logo")
                .source(com.demcha.compose.document.image.DocumentImageData.fromBytes(png())).size(20, 20).build()))
                .as("a picture").isFalse();
        assertThat(floats(new ParagraphBuilder().name("Plain").text("Plain").build()))
                .as("while plain text is set in a text box").isTrue();
    }

    @Test
    void inATableCellItIsWrittenAsBefore() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page
                .addRow(row -> row.addSection(cell -> cell.add(sidebar(new DocumentInsets(-10, 0, 10 - STUB, 0))))
                        .addParagraph("Beside")))) {
            assertThat(document.getDocument().getBody().xmlText()).as("no text box, the lockup in the cell")
                    .doesNotContain("<w:txbxContent>").contains(">&amp;Co.<");
        }
    }

    @Test
    void itsTextBoxesStandInFrontOfTheTextAndItsDrawingsInTheirLayersOrder() throws Exception {
        // In front, a fill or a panel's shading laid under a box cannot cover it. The drawings
        // are stacked by their layers' order, not the order they were given in.
        DocumentNode sidebar = new ShapeContainerBuilder().name("Sidebar")
                .rectangle(100, STUB).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .margin(new DocumentInsets(-40, 0, 40 - STUB, -40))
                .position(new ShapeBuilder().name("Upper").size(100, 50)
                        .fillColor(DocumentColor.rgb(200, 0, 0)).build(), 0, 0, LayerAlign.TOP_LEFT, 2)
                .position(new ParagraphBuilder().name("Line").text("Studio").build(), 10, 10, LayerAlign.TOP_LEFT, 3)
                .position(new ShapeBuilder().name("Lower").size(100, STUB)
                        .fillColor(DocumentColor.rgb(0, 0, 200)).build(), 0, 0, LayerAlign.TOP_LEFT, 1)
                .build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page
                .add(sidebar).addParagraph("Masthead"))) {
            List<String> drawings = List.of(document.getDocument().getBody().xmlText().split("<wp:anchor"));
            String box = drawings.stream().filter(chunk -> chunk.contains("<w:txbxContent>")).findFirst().orElseThrow();

            assertThat(box).as("the text box in front of the text").contains("behindDoc=\"0\"");
            assertThat(stackHeight(drawings, "0000C8")).as("the lower layer under the upper one")
                    .isLessThan(stackHeight(drawings, "C80000"));
        }
    }

    private static long stackHeight(List<String> drawings, String colour) {
        String chunk = drawings.stream().filter(candidate -> candidate.contains("srgbClr val=\"" + colour + "\""))
                .findFirst().orElseThrow();
        java.util.regex.Matcher height = java.util.regex.Pattern.compile("relativeHeight=\"(\\d+)\"").matcher(chunk);
        assertThat(height.find()).isTrue();
        return Long.parseLong(height.group(1));
    }

    /** Whether a sidebar laid over the flow, a brand block and the given layer in it, is set in text boxes. */
    private static boolean floats(DocumentNode layer) throws Exception {
        DocumentNode sidebar = new ShapeContainerBuilder().name("Sidebar")
                .rectangle(100, STUB).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .margin(new DocumentInsets(-40, 0, 40 - STUB, -40))
                .position(new ShapeBuilder().name("BrandBlock").size(100, STUB)
                        .fillColor(DocumentColor.rgb(160, 80, 50)).build(), 0, 0, LayerAlign.TOP_LEFT, 0)
                .position(new ParagraphBuilder().name("Monogram").text("L").build(), 10, 10, LayerAlign.TOP_LEFT, 1)
                .position(layer, 10, 40, LayerAlign.TOP_LEFT, 2)
                .build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page
                .add(sidebar).addParagraph("Masthead"))) {
            return document.getDocument().getBody().xmlText().contains("<w:txbxContent>");
        }
    }

    private static byte[] png() {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(20, 20,
                    java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    /** Where the text box holding a sidebar's one line starts, in EMU from the page's left edge. */
    private static double boxLeft(java.util.function.Consumer<ParagraphBuilder> line) throws Exception {
        ParagraphBuilder paragraph = new ParagraphBuilder().name("Line");
        line.accept(paragraph);
        DocumentNode sidebar = new ShapeContainerBuilder().name("Sidebar")
                .rectangle(100, STUB).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .margin(new DocumentInsets(-40, 0, 40 - STUB, -40))
                .position(paragraph.build(), 10, 10, LayerAlign.TOP_LEFT)
                .build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page
                .add(sidebar).addParagraph("Masthead"))) {
            String body = document.getDocument().getBody().xmlText();
            java.util.regex.Matcher offset = java.util.regex.Pattern
                    .compile("positionH relativeFrom=\"page\"><wp:posOffset>(-?\\d+)</wp:posOffset>.*?<w:txbxContent>",
                            java.util.regex.Pattern.DOTALL)
                    .matcher(body);
            assertThat(offset.find()).as("a text box").isTrue();
            return Double.parseDouble(offset.group(1));
        }
    }

    @Test
    void aTitleAndItsDatesInsideItAreSetInTextBoxesToo() throws Exception {
        // A title and its dates at either end of a band are written as one line in the flow
        // wherever else they stand; laid over the flow, they are two text boxes.
        DocumentNode pair = new ShapeContainerBuilder().name("EntryHead")
                .rectangle(90, 12).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ParagraphBuilder().name("Title").text("Studio").build(), 0, 0, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder().name("Dates").text("2024")
                        .align(com.demcha.compose.document.node.TextAlign.RIGHT).build(),
                        0, 0, LayerAlign.CENTER_RIGHT)
                .build();
        DocumentNode sidebar = new ShapeContainerBuilder().name("Sidebar")
                .rectangle(100, STUB).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .margin(new DocumentInsets(-40, 0, 40 - STUB, -40))
                .position(new ShapeBuilder().name("BrandBlock").size(100, STUB)
                        .fillColor(DocumentColor.rgb(160, 80, 50)).build(), 0, 0, LayerAlign.TOP_LEFT, 0)
                .position(pair, 5, 10, LayerAlign.TOP_LEFT, 1)
                .build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page
                .add(sidebar)
                .addParagraph("Masthead"))) {
            assertThat(document.getParagraphs()).extracting(DocxOverTheFlowTest::ownText)
                    .as("neither is written in the flow").noneMatch(text -> text.contains("Studio"));
            String body = document.getDocument().getBody().xmlText();
            assertThat(body.split("<w:txbxContent>", -1)).as("two text boxes").hasSize(3);
            assertThat(body).contains(">Studio<").contains(">2024<");
        }
    }

    @Test
    void columnsInsideItAreSetInTextBoxesNotAsATableInTheFlow() throws Exception {
        // Two sections side by side in a stack are written as a one-row table wherever else
        // they stand: laid over the flow, that table would stand in it, empty, and push it down.
        DocumentNode columns = new com.demcha.compose.document.dsl.LayerStackBuilder().name("Columns")
                .layer(new com.demcha.compose.document.dsl.SectionBuilder().name("Left").spacing(0)
                        .padding(new DocumentInsets(0, 50, 0, 0))
                        .addSection("LeftContent", section -> section.spacing(0).addParagraph("West"))
                        .build(), LayerAlign.TOP_LEFT)
                .layer(new com.demcha.compose.document.dsl.SectionBuilder().name("Right").spacing(0)
                        .padding(new DocumentInsets(0, 0, 0, 50))
                        .addSection("RightContent", section -> section.spacing(0).addParagraph("East"))
                        .build(), LayerAlign.TOP_LEFT)
                .build();
        DocumentNode sidebar = new ShapeContainerBuilder().name("Sidebar")
                .rectangle(100, STUB).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .margin(new DocumentInsets(-40, 0, 40 - STUB, -40))
                .position(columns, 0, 10, LayerAlign.TOP_LEFT)
                .build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page
                .add(sidebar)
                .addParagraph("Masthead"))) {
            assertThat(document.getTables()).as("no table in the flow").isEmpty();
            assertThat(document.getParagraphs()).extracting(DocxOverTheFlowTest::ownText)
                    .as("neither in the flow").noneMatch(text -> text.contains("West") || text.contains("East"));
            assertThat(document.getDocument().getBody().xmlText()).contains(">West<").contains(">East<");
        }
    }

    @Test
    void aContainerThePageGivesRoomIsWrittenInTheFlowAsBefore() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page
                .add(sidebar(DocumentInsets.zero()))
                .addParagraph("Masthead"))) {
            assertThat(document.getParagraphs()).extracting(DocxOverTheFlowTest::ownText)
                    .as("its lines stand in the flow, which gives it room").contains("&Co.");
        }
    }

    /** A brand block with two lines of lockup over it, in a container with the given margins. */
    private static DocumentNode sidebar(DocumentInsets margin) {
        return new ShapeContainerBuilder().name("Sidebar")
                .rectangle(100, STUB).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .margin(margin)
                .position(new ShapeBuilder().name("BrandBlock").size(100, STUB)
                        .fillColor(DocumentColor.rgb(160, 80, 50)).build(), 0, 0, LayerAlign.TOP_LEFT, 0)
                .position(new ParagraphBuilder().name("MonogramTop").text("L").build(),
                        10, 10, LayerAlign.TOP_LEFT, 1)
                .position(new ParagraphBuilder().name("MonogramBottom").text("&Co.").build(),
                        10, 40, LayerAlign.TOP_LEFT, 2)
                // A container of its own, whose edges a container written in the flow carries
                // as space: none of it may reach the flow after the sidebar.
                .position(new ShapeContainerBuilder().name("Tagline")
                        .rectangle(80, 20).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .margin(new DocumentInsets(30, 0, 12, 0)).padding(new DocumentInsets(4, 0, 4, 0))
                        .position(new ParagraphBuilder().name("TaglineText").text("Design.").build(),
                                0, 0, LayerAlign.TOP_LEFT)
                        .build(), 10, 60, LayerAlign.TOP_LEFT, 3)
                .build();
    }

    private static XWPFParagraph masthead(XWPFDocument document) {
        return document.getParagraphs().stream()
                .filter(paragraph -> ownText(paragraph).equals("Masthead")).findFirst().orElseThrow();
    }

    /** The text of a paragraph's own runs, not of the text boxes anchored in it. */
    private static String ownText(XWPFParagraph paragraph) {
        StringBuilder text = new StringBuilder();
        for (var run : paragraph.getCTP().getRList()) {
            for (var part : run.getTList()) {
                text.append(part.getStringValue());
            }
        }
        return text.toString();
    }

    private static long before(XWPFParagraph paragraph) {
        var properties = paragraph.getCTP().getPPr();
        return properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetBefore()
                ? 0 : DocxTwips.of(properties.getSpacing().getBefore());
    }
}
