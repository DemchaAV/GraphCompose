package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A layer stack whose layers are side-by-side columns is written as the columns of one row.
 *
 * <p>A two-column CV can lay its columns out as the layers of one stack, each inset to its
 * band, so the name can be drawn before the sidebar. The export wrote the layers one after
 * the other, so the main column started below the whole sidebar and a one-page CV ran to
 * three pages in LibreOffice.</p>
 */
class DocxLayerColumnsTest {

    private static final double PAGE_WIDTH = 400;
    private static final double MARGIN = 20;
    private static final double SIDEBAR = 120;
    private static final double MAIN = PAGE_WIDTH - 2 * MARGIN - SIDEBAR;
    /** What a line of the default text in a column is raised into the space above it. */
    private static final long RAISE = DocxExports.DEFAULT_LINE_RAISE;
    /** A paragraph long enough to fill its column, so the stack is as wide as the page's. */
    private static final String LONG = "Led the delivery of a document platform across three teams, "
            + "from the first prototype to the release that replaced the old reporting stack.";

    @Test
    void sideBySideLayersAreTheCellsOfOneRow() throws Exception {
        try (Export export = export(stack -> stack
                .layer(column("Sidebar", 0, MAIN, side -> side.addParagraph("Contact")), LayerAlign.TOP_LEFT)
                .layer(column("Main", SIDEBAR, 0, main -> main.addParagraph("Experience")
                        .addParagraph(LONG)), LayerAlign.TOP_LEFT))) {
            XWPFDocument document = export.document();

            assertThat(document.getTables()).hasSize(1);
            XWPFTable table = document.getTables().get(0);
            assertThat(table.getRows()).hasSize(1);
            assertThat(table.getRow(0).getCell(0).getText()).isEqualTo("Contact");
            assertThat(table.getRow(0).getCell(1).getText()).startsWith("Experience").contains(LONG);
            // The sidebar's band ends where the main column's inset starts on the right: the
            // stack's width less the main column's width. The stack is as wide as its widest
            // layer, the main column with its longest line.
            double stack = export.placed("Columns").placementWidth();
            assertThat(table.getCTTbl().getTblGrid().getGridColList())
                    .extracting(column -> DocxTwips.of(column.getW()))
                    .containsExactly(Math.round((stack - MAIN) * 20), Math.round(MAIN * 20));
            assertThat(document.getParagraphs()).as("nothing of either column left in the body")
                    .extracting(XWPFParagraph::getText).doesNotContain("Contact", "Experience");
        }
    }

    @Test
    void aLaterLayerStandsBelowTheLineAboveItAsFarAsThatLineWasRaised() throws Exception {
        // The first layer's line, Spectral under 20pt of space, is moved up into that space and
        // owes as much below it. The later layer measures the gap to itself from the page, where
        // that line stood lower, so it takes the raise back with the gap.
        com.demcha.compose.document.style.DocumentTextStyle spectral = com.demcha.compose.document.style
                .DocumentTextStyle.builder().fontName(com.demcha.compose.font.FontName.SPECTRAL).size(30).build();
        com.demcha.compose.document.style.DocumentTextStyle lato = com.demcha.compose.document.style
                .DocumentTextStyle.builder().fontName(com.demcha.compose.font.FontName.LATO).size(10).build();
        try (Export export = export(stack -> stack
                .layer(column("TitleLayer", SIDEBAR, 0, top -> top.addParagraph(p -> p.name("Title").text("Title")
                        .textStyle(spectral).margin(DocumentInsets.top(20)))), LayerAlign.TOP_LEFT)
                .layer(column("Sidebar", 0, MAIN, side -> side.addParagraph("Contact")), LayerAlign.TOP_LEFT)
                .layer(column("MainLayer", SIDEBAR, 0, main -> main
                        .addSpacer(spacer -> spacer.name("TitlePlace").width(100).height(80))
                        .addParagraph(p -> p.name("Role").text("Engineer").textStyle(lato))), LayerAlign.TOP_LEFT))) {
            XWPFTableCell main = export.document().getTables().get(0).getRow(0).getCell(1);
            XWPFParagraph title = main.getParagraphs().get(0);
            double line = org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.EXACT
                    .equals(title.getCTP().getPPr().getSpacing().getLineRule())
                    ? DocxTwips.of(title.getCTP().getPPr().getSpacing().getLine()) / 20.0 : Double.NaN;
            double raise = 0.8 * line - 1.059 * 30;
            PlacedNode placedTitle = export.placed("Title");
            PlacedNode role = export.placed("Role");
            double gap = placedTitle.placementY() - (role.placementY() + role.placementHeight());

            assertThat(main.getParagraphs()).extracting(XWPFParagraph::getText).containsExactly("Title", "Engineer");
            assertThat(spacingBefore(title)).as("the premise: the title raised into the space above it")
                    .isCloseTo(Math.round((20 - raise) * 20), org.assertj.core.data.Offset.offset(1L));
            assertThat(spacingBefore(main.getParagraphs().get(1)))
                    .isCloseTo(Math.round((gap + raise) * 20), org.assertj.core.data.Offset.offset(2L));
        }
    }

    @Test
    void aLaterLayerThatWritesNothingLeavesTheRaiseOwedInTheCell() throws Exception {
        // The later layer only holds the title's place: written of it is nothing, and the raised
        // title still owes below it what it was raised by, at the cell's end.
        com.demcha.compose.document.style.DocumentTextStyle spectral = com.demcha.compose.document.style
                .DocumentTextStyle.builder().fontName(com.demcha.compose.font.FontName.SPECTRAL).size(30).build();
        try (Export export = export(stack -> stack
                .layer(column("TitleLayer", SIDEBAR, 0, top -> top.addParagraph(p -> p.name("Title").text("Title")
                        .textStyle(spectral).margin(DocumentInsets.top(20)))), LayerAlign.TOP_LEFT)
                .layer(column("Sidebar", 0, MAIN, side -> side.addParagraph("Contact")), LayerAlign.TOP_LEFT)
                .layer(column("MainLayer", SIDEBAR, 0, main -> main
                        .addSpacer(spacer -> spacer.name("TitlePlace").width(100).height(80))), LayerAlign.TOP_LEFT),
                true)) {
            XWPFTableCell main = export.document().getTables().get(0).getRow(0).getCell(1);
            XWPFParagraph title = main.getParagraphs().get(main.getParagraphs().size() - 1);
            long raised = 20 * 20L - spacingBefore(title);
            var spacing = title.getCTP().getPPr().getSpacing();

            assertThat(title.getText()).as("the premise: the title ends the cell").isEqualTo("Title");
            assertThat(raised).as("the premise: the title raised into the space above it").isGreaterThan(20);
            assertThat(spacing.isSetAfter() ? DocxTwips.of(spacing.getAfter()) : 0).isEqualTo(raised);
        }
    }

    @Test
    void aLaterLayerUnderAPanelTakesNoRaiseThePanelAlreadyOwedInside() throws Exception {
        // The first layer ends in a painted card whose line is raised: the card's cell owes the
        // raise below that line, inside the card, and the later layer's gap from the card's foot
        // is the page's alone.
        com.demcha.compose.document.style.DocumentTextStyle spectral = com.demcha.compose.document.style
                .DocumentTextStyle.builder().fontName(com.demcha.compose.font.FontName.SPECTRAL).size(30).build();
        com.demcha.compose.document.style.DocumentTextStyle lato = com.demcha.compose.document.style
                .DocumentTextStyle.builder().fontName(com.demcha.compose.font.FontName.LATO).size(10).build();
        try (Export export = export(stack -> stack
                .layer(column("CardLayer", SIDEBAR, 0, top -> top.addSection("Card", card -> card
                        .fillColor(DocumentColor.rgb(238, 243, 249)).padding(DocumentInsets.of(4))
                        .addParagraph(p -> p.name("Title").text("Title").textStyle(spectral)
                                .margin(DocumentInsets.top(20))))), LayerAlign.TOP_LEFT)
                .layer(column("Sidebar", 0, MAIN, side -> side.addParagraph("Contact")), LayerAlign.TOP_LEFT)
                .layer(column("MainLayer", SIDEBAR, 0, main -> main
                        .addSpacer(spacer -> spacer.name("CardPlace").width(100).height(100))
                        .addParagraph(p -> p.name("Role").text("Engineer").textStyle(lato))), LayerAlign.TOP_LEFT))) {
            XWPFTableCell main = export.document().getTables().get(0).getRow(0).getCell(1);
            XWPFParagraph role = main.getParagraphs().stream()
                    .filter(paragraph -> "Engineer".equals(paragraph.getText())).findFirst().orElseThrow();
            XWPFParagraph title = main.getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                    .filter(paragraph -> "Title".equals(paragraph.getText())).findFirst().orElseThrow();
            PlacedNode card = export.placed("Card");
            PlacedNode placedRole = export.placed("Role");
            double gap = card.placementY() - (placedRole.placementY() + placedRole.placementHeight());

            assertThat(spacingBefore(title)).as("the premise: the title raised into the space above it")
                    .isLessThan(20 * 20L);
            assertThat(spacingBefore(role)).isCloseTo(Math.round(gap * 20), org.assertj.core.data.Offset.offset(2L));
        }
    }

    @Test
    void layersSharingABandFollowOneAnotherAndTheStandInIsLeftOut() throws Exception {
        // The name is drawn first, in a layer of its own; the main column holds its place with
        // a spacer and goes on with the role. In the cell the name comes first, the stand-in is
        // not written, and the role sits the page's distance below the name rather than below
        // the main column's padding and the spacer as well.
        try (Export export = export(stack -> stack
                .layer(column("NameLayer", SIDEBAR, 0,
                        name -> name.addParagraph(p -> p.name("Name").text("Ada Lovelace"))), LayerAlign.TOP_LEFT)
                .layer(column("Sidebar", 0, MAIN, side -> side.addParagraph("Contact")), LayerAlign.TOP_LEFT)
                .layer(column("MainLayer", SIDEBAR, 0, main -> main
                        .padding(DocumentInsets.top(10))
                        .addSpacer(spacer -> spacer.name("NamePlace").width(100).height(30))
                        .addParagraph(p -> p.name("Role").text("Engineer"))
                        .addParagraph(LONG)), LayerAlign.TOP_LEFT))) {
            XWPFTableCell main = export.document().getTables().get(0).getRow(0).getCell(1);

            assertThat(main.getParagraphs()).extracting(XWPFParagraph::getText)
                    .containsExactly("Ada Lovelace", "Engineer", LONG);
            PlacedNode name = export.placed("Name");
            PlacedNode role = export.placed("Role");
            double gap = name.placementY() - (role.placementY() + role.placementHeight());
            assertThat(gap).as("the page puts the role below the name").isGreaterThan(10);
            assertThat(spacingBefore(main.getParagraphs().get(1)))
                    .isCloseTo(Math.round(gap * 20) - RAISE, org.assertj.core.data.Offset.offset(1L));
        }
    }

    @Test
    void aDrawingThatOpensALaterLayerKeepsItsRoomBelowTheResume() throws Exception {
        // The main layer goes on below the name with a portrait drawn as a path. The path is
        // not written; the role under it is still the page's distance below the name.
        try (Export export = export(stack -> stack
                .layer(column("NameLayer", SIDEBAR, 0,
                        name -> name.addParagraph(p -> p.name("Name").text("Ada Lovelace"))), LayerAlign.TOP_LEFT)
                .layer(column("Sidebar", 0, MAIN, side -> side.addParagraph("Contact")), LayerAlign.TOP_LEFT)
                .layer(column("MainLayer", SIDEBAR, 0, main -> main
                        .padding(DocumentInsets.top(10))
                        .addSpacer(spacer -> spacer.name("NamePlace").width(100).height(30))
                        .add(new com.demcha.compose.document.dsl.PathBuilder().name("Portrait").size(60, 30)
                                .moveTo(0, 0).lineTo(1, 0).lineTo(0.5, 1).closePath().build())
                        .addParagraph(p -> p.name("Role").text("Engineer"))), LayerAlign.TOP_LEFT))) {
            XWPFTableCell main = export.document().getTables().get(0).getRow(0).getCell(1);
            XWPFParagraph role = main.getParagraphs().stream()
                    .filter(paragraph -> "Engineer".equals(paragraph.getText())).findFirst().orElseThrow();
            PlacedNode name = export.placed("Name");
            PlacedNode placedRole = export.placed("Role");
            double gap = name.placementY() - (placedRole.placementY() + placedRole.placementHeight());

            assertThat(gap).as("the portrait stands between them on the page").isGreaterThan(30);
            assertThat(spacingBefore(role)).isCloseTo(Math.round(gap * 20) - RAISE, org.assertj.core.data.Offset.offset(2L));
        }
    }

    @Test
    void aSpacerThatHoldsRealSpaceInAShortLayerIsKept() throws Exception {
        // The name layer puts 12pt between the name and the title. The main layer beside it runs
        // the whole band, so its box is level with that spacer; its content is not.
        try (Export export = export(stack -> stack
                .layer(column("NameLayer", SIDEBAR, 0, name -> name
                        .addParagraph(p -> p.name("Name").text("Ada Lovelace"))
                        .addSpacer(spacer -> spacer.name("Gap").width(100).height(12))
                        .addParagraph("Analyst")), LayerAlign.TOP_LEFT)
                .layer(column("Sidebar", 0, MAIN, side -> side.addParagraph("Contact")), LayerAlign.TOP_LEFT)
                .layer(column("MainLayer", SIDEBAR, 0, main -> main
                        .addSpacer(spacer -> spacer.name("NamePlace").width(100).height(60))
                        .addParagraph("Engineer")
                        .addParagraph(LONG)), LayerAlign.TOP_LEFT))) {
            XWPFTableCell main = export.document().getTables().get(0).getRow(0).getCell(1);

            assertThat(main.getParagraphs()).extracting(XWPFParagraph::getText)
                    .containsExactly("Ada Lovelace", "", "Analyst", "Engineer", LONG);
        }
    }

    @Test
    void aStandInAtTheFootOfAnEarlierLayerIsNotWhereTheGapIsMeasuredFrom() throws Exception {
        // The name layer ends with a stand-in for the subtitle the main layer writes. The gap
        // above the subtitle is measured from the name, not from that stand-in's foot, which is
        // below the subtitle's top and left the subtitle no gap at all.
        try (Export export = export(stack -> stack
                .layer(column("NameLayer", SIDEBAR, 0, name -> name
                        .addParagraph(p -> p.name("Name").text("Ada Lovelace"))
                        .addSpacer(spacer -> spacer.name("SubtitlePlace").width(100).height(40))), LayerAlign.TOP_LEFT)
                .layer(column("Sidebar", 0, MAIN, side -> side.addParagraph("Contact")), LayerAlign.TOP_LEFT)
                .layer(column("MainLayer", SIDEBAR, 0, main -> main
                        .addSpacer(spacer -> spacer.name("NamePlace").width(100).height(30))
                        .addParagraph(p -> p.name("Subtitle").text("Engineer"))
                        .addParagraph(LONG)), LayerAlign.TOP_LEFT))) {
            XWPFTableCell main = export.document().getTables().get(0).getRow(0).getCell(1);
            PlacedNode name = export.placed("Name");
            PlacedNode subtitle = export.placed("Subtitle");
            double gap = name.placementY() - (subtitle.placementY() + subtitle.placementHeight());

            assertThat(main.getParagraphs()).extracting(XWPFParagraph::getText)
                    .containsExactly("Ada Lovelace", "Engineer", LONG);
            assertThat(gap).isGreaterThan(5);
            assertThat(spacingBefore(main.getParagraphs().get(1)))
                    .isCloseTo(Math.round(gap * 20) - RAISE, org.assertj.core.data.Offset.offset(1L));
        }
    }

    @Test
    void theFirstLayerKeepsItsTopEdgeAndTheStackItsLeftOne() throws Exception {
        // A layer's side padding is its band; its top padding is space above its content. The
        // bands are measured inside the stack's own padding, so the table starts there too.
        try (Export export = export(stack -> stack
                .padding(new DocumentInsets(0, 0, 0, 16))
                .layer(new SectionBuilder().name("Sidebar").spacing(0)
                        .padding(new DocumentInsets(25, MAIN - 16, 0, 0))
                        .addParagraph("Contact").build(), LayerAlign.TOP_LEFT)
                .layer(column("Main", SIDEBAR, 0, main -> main.addParagraph("Experience")
                        .addParagraph(LONG)), LayerAlign.TOP_LEFT))) {
            XWPFTable table = export.document().getTables().get(0);

            assertThat(spacingBefore(table.getRow(0).getCell(0).getParagraphs().get(0))).isEqualTo(25 * 20L - RAISE);
            assertThat(table.getCTTbl().getTblPr().isSetTblInd()).as("the table is indented").isTrue();
            assertThat(DocxTwips.of(table.getCTTbl().getTblPr().getTblInd().getW())).isEqualTo(16 * 20L);
        }
    }

    @Test
    void layersWhoseBandsOverlapAreStillWrittenOneAfterTheOther() throws Exception {
        try (Export export = export(stack -> stack
                .layer(column("Wide", 0, 0, wide -> wide.addParagraph("Behind")), LayerAlign.TOP_LEFT)
                .layer(column("Inset", SIDEBAR, 0, inset -> inset.addParagraph("Over")), LayerAlign.TOP_LEFT))) {
            assertThat(export.document().getTables()).isEmpty();
            assertThat(export.document().getParagraphs()).extracting(XWPFParagraph::getText)
                    .contains("Behind", "Over");
        }
    }

    @Test
    void aLayerThatPaintsIsNotAColumn() throws Exception {
        // A filled layer is a panel drawn over the others, not a band of text beside them.
        try (Export export = export(stack -> stack
                .layer(column("Sidebar", 0, MAIN, side -> side.addParagraph("Contact")), LayerAlign.TOP_LEFT)
                .layer(new SectionBuilder().name("Main").padding(new DocumentInsets(0, 0, 0, SIDEBAR))
                        .fillColor(DocumentColor.rgb(240, 240, 240))
                        .addParagraph("Experience").build(), LayerAlign.TOP_LEFT))) {
            assertThat(export.document().getTables())
                    .noneMatch(table -> table.getRow(0).getTableCells().size() == 2);
        }
    }

    @Test
    void aRuleInAColumnIsWrittenAsARule() throws Exception {
        // Beside one another the columns overlap nothing, so a rule in one is a rule, not a
        // stroke of a picture drawn over something else.
        try (Export export = export(stack -> stack
                .layer(column("Sidebar", 0, MAIN, side -> side.addParagraph("Contact")), LayerAlign.TOP_LEFT)
                .layer(column("Main", SIDEBAR, 0, main -> main
                        .addParagraph("Experience")
                        .addDivider(divider -> divider.width(200).thickness(1).color(DocumentColor.rgb(0, 0, 0)))
                        .addParagraph(LONG)), LayerAlign.TOP_LEFT))) {
            XWPFTableCell main = export.document().getTables().get(0).getRow(0).getCell(1);

            assertThat(main.getParagraphs())
                    .anyMatch(paragraph -> paragraph.getCTP().xmlText().contains("w:pBdr"));
        }
    }

    private static long spacingBefore(XWPFParagraph paragraph) {
        var properties = paragraph.getCTP().getPPr();
        return properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetBefore()
                ? 0
                : DocxTwips.of(properties.getSpacing().getBefore());
    }

    @Test
    void aCardsMarkAndTheRuleBetweenCardsAreDrawnOverTheColumnsNotColumnsOfTheirOwn() throws Exception {
        // A card grid lays each card's mark over the edge of its text and a rule on the
        // boundary between two cards: neither is a column, and while they stood in the way the
        // grid was written one card under the other, each a column lower and further right.
        try (Export export = export(stack -> stack
                .layer(column("First", 0, MAIN, first -> first.addParagraph("AWS Certified")), LayerAlign.TOP_LEFT)
                .layer(column("Second", SIDEBAR, 0, second -> second.addParagraph("Docker Certified")), LayerAlign.TOP_LEFT)
                .layer(sleeve("Mark", SIDEBAR - 8, new com.demcha.compose.document.dsl.ImageBuilder().name("Medal")
                        .source(pngBytes()).size(16, 16).build()), LayerAlign.TOP_LEFT)
                .layer(sleeve("Rule", SIDEBAR - 4, new com.demcha.compose.document.dsl.LineBuilder().name("RuleLine")
                        .vertical(80).thickness(0.5).color(DocumentColor.rgb(200, 200, 200)).build()),
                        LayerAlign.TOP_LEFT))) {
            XWPFDocument document = export.document();
            String body = document.getDocument().xmlText();

            assertThat(document.getTables()).hasSize(1);
            XWPFTable table = document.getTables().get(0);
            assertThat(table.getRow(0).getTableCells()).extracting(XWPFTableCell::getText)
                    .containsExactly("AWS Certified", "Docker Certified");
            assertThat(body).as("the mark is drawn where the page puts it, not written in a cell")
                    .doesNotContain("<wp:inline").contains("<pic:pic");
            assertThat(body).as("and the rule across the boundary with it").contains("C8C8C8");
            // The rule, as tall as the band, is what made the band that tall: the row keeps it.
            var row = table.getRow(0).getCtRow().getTrPr();
            assertThat(row != null && row.sizeOfTrHeightArray() == 1).as("the row is held to a height").isTrue();
            assertThat(DocxTwips.of(row.getTrHeightArray(0).getVal())).isGreaterThanOrEqualTo(80L * 20 - 1);
        }
    }

    @Test
    void aPortraitInABandOfItsOwnStaysAColumn() throws Exception {
        // A picture alone in its sleeve is only left out where it stands across the columns: a
        // portrait with a band of its own beside the text is that band's cell, as before.
        try (Export export = export(stack -> stack
                .layer(column("Portrait", 0, MAIN, side -> side.add(new com.demcha.compose.document.dsl.ImageBuilder()
                        .name("Photo").source(pngBytes()).size(80, 80).build())), LayerAlign.TOP_LEFT)
                .layer(column("Main", SIDEBAR, 0, main -> main.addParagraph("Experience")), LayerAlign.TOP_LEFT))) {
            XWPFDocument document = export.document();

            assertThat(document.getTables()).hasSize(1);
            assertThat(document.getTables().get(0).getRow(0).getTableCells()).hasSize(2);
            assertThat(document.getDocument().xmlText()).as("the portrait written in its cell").contains("<wp:inline");
        }
    }

    private static DocumentNode sleeve(String name, double insetLeft, DocumentNode child) {
        SectionBuilder sleeve = new SectionBuilder();
        sleeve.name(name).spacing(0).padding(DocumentInsets.zero())
                .margin(new DocumentInsets(0, 0, 0, insetLeft));
        sleeve.add(child);
        return sleeve.build();
    }

    private static byte[] pngBytes() {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(16, 16,
                    java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    /** One column as a full-width layer, inset to its band, as a two-column CV lays it out. */
    private static DocumentNode column(String name, double insetLeft, double insetRight,
                                       Consumer<SectionBuilder> content) {
        SectionBuilder layer = new SectionBuilder();
        layer.name(name).spacing(0).padding(new DocumentInsets(0, insetRight, 0, insetLeft));
        layer.addSection(name + "Content", section -> content.accept(section.spacing(0)));
        return layer.build();
    }

    private static Export export(Consumer<LayerStackBuilder> layers) throws Exception {
        return export(layers, false);
    }

    /**
     * @param followed whether a paragraph follows the stack, so its table does not close the
     *                 document and its cells keep the space below their last lines
     */
    private static Export export(Consumer<LayerStackBuilder> layers, boolean followed) throws Exception {
        DocumentSession session = GraphCompose.document()
                .pageSize(PAGE_WIDTH, 600)
                .margin(DocumentInsets.of(MARGIN))
                .create();
        session.pageFlow(page -> {
            page.addLayerStack(stack -> layers.accept(stack.name("Columns")));
            if (followed) {
                page.addParagraph("Below");
            }
        });
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
