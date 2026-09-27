package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A strip drawn in two layers for its reading order — its fill and the name in one, with a
 * stand-in where the subtitle goes, the subtitle in a later one — is written as one strip.
 *
 * @author Artem Demchyshyn
 */
class DocxStandInMoveTest {

    private static final DocumentTextStyle TEXT = DocumentTextStyle.DEFAULT.withSize(12);
    // Wider than the subtitle with room to spare, so the strip, as wide as its content, is too.
    private static final String NAME = "Jordan Alexander Rivera";

    @Test
    void theSubtitleALaterLayerLaysInThePanelIsWrittenInThePanel() throws Exception {
        Export export = export(false);
        try (XWPFDocument document = export.document()) {
            XWPFTableCell panel = cellHolding(document, NAME);

            assertThat(panel.getText()).as("the strip holds the name and the subtitle over it")
                    .contains(NAME).contains("Subtitle");
            assertThat(allText(document).split("Subtitle", -1)).as("written once").hasSize(2);
            assertThat(panel.getText()).doesNotContain("Body");
        }
    }

    @Test
    void theBodyResumesFromTheStripsFootNotFromInsideIt() throws Exception {
        // The strip's bottom padding is the panel cell's margin; counted again in the gap, the
        // column under the strip stood that much too low.
        Export export = export(false);
        try (XWPFDocument document = export.document()) {
            XWPFParagraph body = paragraph(cellHolding(document, "Body"), "Body");
            PlacedNode bodyBox = export.node("Body");
            double gap = export.node("Strip").placementY() - (bodyBox.placementY() + bodyBox.placementHeight());
            double before = body.getCTP().getPPr() == null || !body.getCTP().getPPr().isSetSpacing()
                            || body.getCTP().getPPr().getSpacing().getBefore() == null
                    ? 0 : DocxTwips.of(body.getCTP().getPPr().getSpacing().getBefore()) / 20.0;

            assertThat(before).isCloseTo(Math.max(0, gap), within(0.1));
        }
    }

    @Test
    void aStandInTallerThanWhatMovesIntoItKeepsTheRestOfItsHeight() throws Exception {
        Export export = export(false, 30, 10);
        try (XWPFDocument document = export.document()) {
            XWPFParagraph subtitle = paragraph(cellHolding(document, NAME), "Subtitle");
            double rest = export.node("Subtitle").placementY() - export.node("SubtitlePlace").placementY();

            assertThat(rest).isGreaterThan(15);
            assertThat(DocxTwips.of(subtitle.getCTP().getPPr().getSpacing().getAfter()) / 20.0)
                    .isCloseTo(rest, within(0.1));
        }
    }

    @Test
    void aMovedBlockStandsAcrossWhereItsOwnLayerPutIt() throws Exception {
        // The strip is padded 10pt at the sides, the layer the subtitle comes from 20pt: in the
        // strip's cell the subtitle is held in the 10pt more its own layer gives it.
        Export export = export(false, 0, 20);
        try (XWPFDocument document = export.document()) {
            XWPFParagraph subtitle = paragraph(cellHolding(document, NAME), "Subtitle");
            double placed = export.node("Subtitle").placementX() - export.node("SubtitlePlace").placementX();

            assertThat(placed).isCloseTo(10, within(0.1));
            assertThat(DocxTwips.of(subtitle.getCTP().getPPr().getInd().getLeft()) / 20.0)
                    .isCloseTo(placed, within(0.1));
        }
    }

    @Test
    void aBlockReachingPastTheStripAcrossStaysInItsOwnLayer() throws Exception {
        // A strip as narrow as a short name, and a subtitle its own layer sets 10pt further in:
        // the subtitle stands out of the strip on the page, and is not moved into it.
        try (XWPFDocument document = export(false, 0, 20, "Name", false).document()) {
            assertThat(cellHolding(document, "Name").getText()).doesNotContain("Subtitle");
            assertThat(allText(document).split("Subtitle", -1)).as("written once").hasSize(2);
        }
    }

    @Test
    void twoBlocksInOneStandInAreWrittenInTheirOrderOnThePage() throws Exception {
        try (XWPFDocument document = export(false, 30, 10, NAME, true).document()) {
            assertThat(cellHolding(document, NAME).getParagraphs())
                    .extracting(XWPFParagraph::getText)
                    .filteredOn(text -> !text.isBlank())
                    .containsExactly(NAME, "Subtitle", "Tagline");
        }
    }

    @Test
    void aSubtitleSetInAChipMovesWithItsChip() throws Exception {
        try (XWPFDocument document = export(true).document()) {
            XWPFTableCell panel = cellHolding(document, NAME);

            assertThat(panel.getTables()).as("the chip, a panel of its own, inside the strip").hasSize(1);
            assertThat(panel.getTables().get(0).getText()).contains("Subtitle");
        }
    }

    private record Export(XWPFDocument document, List<PlacedNode> nodes) {
        PlacedNode node(String name) {
            return nodes.stream().filter(node -> name.equals(node.semanticName())).findFirst().orElseThrow();
        }
    }

    private static Export export(boolean chip) throws Exception {
        return export(chip, 0, 10);
    }

    private static Export export(boolean chip, double extra, double side) throws Exception {
        return export(chip, extra, side, NAME, false);
    }

    /**
     * Two columns: a sidebar, and a main column whose strip is drawn by two layers, the way
     * {@code ReadingOrderColumns} builds one. The first paints the strip with the name and a
     * stand-in under it, as tall as the subtitle plus {@code extra}; the second holds the name's
     * place with a spacer as tall as the name, then writes the subtitle, which lies inside the
     * stand-in, and after the strip, the body. The heights come from laying the strip out once.
     * With {@code tagline}, a second line follows the subtitle, in the stand-in's extra height.
     */
    private static Export export(boolean chip, double extra, double side, String nameText, boolean tagline)
            throws Exception {
        double[] name = new double[2];
        double[] subtitle = new double[2];
        try (DocumentSession probe = session()) {
            probe.pageFlow(page -> page.addSection("Probe", strip -> strip.padding(DocumentInsets.of(10))
                    .addParagraph(p -> p.name("Name").text(nameText).textStyle(TEXT))
                    .addParagraph(p -> p.name("Subtitle").text("Subtitle").textStyle(TEXT))));
            for (PlacedNode node : probe.layoutGraph().nodes()) {
                double[] box = "Name".equals(node.semanticName()) ? name
                        : "Subtitle".equals(node.semanticName()) ? subtitle : null;
                if (box != null) {
                    box[0] = node.placementWidth();
                    box[1] = node.placementHeight();
                }
            }
        }
        try (DocumentSession session = session()) {
            session.pageFlow(page -> page.addLayerStack(stack -> stack
                    .layer(column("Sidebar", 0, 260, sidebar -> sidebar
                            .addParagraph(p -> p.text("Contact").textStyle(TEXT))), LayerAlign.TOP_LEFT)
                    .layer(column("StripLayer", 100, 0, main -> main.addSection("Strip", strip -> strip
                            .fillColor(DocumentColor.rgb(240, 240, 236))
                            .padding(DocumentInsets.of(10))
                            .addParagraph(p -> p.name("Name").text(nameText).textStyle(TEXT))
                            .addSpacer(spacer -> spacer.name("SubtitlePlace")
                                    .width(subtitle[0]).height(subtitle[1] + extra)))),
                            LayerAlign.TOP_LEFT)
                    .layer(column("MainLayer", 100, 0, main -> {
                        main.addSection("Hero", hero -> {
                            hero.padding(new DocumentInsets(10, side, 10 + extra, side))
                                    .addSpacer(spacer -> spacer.name("NamePlace").width(name[0]).height(name[1]));
                            if (chip) {
                                hero.addSection("Chip", pill -> pill
                                        .fillColor(DocumentColor.rgb(26, 86, 148))
                                        .addParagraph(p -> p.name("Subtitle").text("Subtitle").textStyle(TEXT)));
                            } else {
                                hero.addParagraph(p -> p.name("Subtitle").text("Subtitle").textStyle(TEXT));
                            }
                            if (tagline) {
                                hero.addParagraph(p -> p.name("Tagline").text("Tagline").textStyle(TEXT));
                            }
                        });
                        main.addParagraph(p -> p.name("Body").text("Body").textStyle(TEXT));
                    }), LayerAlign.TOP_LEFT)));
            XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(
                    session.export(new DocxSemanticBackend())));
            return new Export(document, session.layoutGraph().nodes());
        }
    }

    private static DocumentSession session() {
        return GraphCompose.document().pageSize(400, 400).margin(DocumentInsets.of(20)).create();
    }

    private static DocumentNode column(String name, double left, double right, Consumer<SectionBuilder> content) {
        SectionBuilder layer = new SectionBuilder();
        layer.name(name).spacing(0).padding(new DocumentInsets(0, right, 0, left));
        layer.addSection(name + "Content", content);
        return layer.build();
    }

    private static XWPFParagraph paragraph(XWPFTableCell cell, String text) {
        return cell.getParagraphs().stream().filter(p -> p.getText().equals(text)).findFirst().orElseThrow();
    }

    /** The innermost cell holding a paragraph of the given text. */
    private static XWPFTableCell cellHolding(XWPFDocument document, String text) {
        List<XWPFTable> tables = new ArrayList<>(document.getTables());
        XWPFTableCell found = null;
        while (!tables.isEmpty()) {
            XWPFTable table = tables.remove(0);
            for (var row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    if (cell.getParagraphs().stream().anyMatch(p -> p.getText().equals(text))) {
                        found = cell;
                    }
                    tables.addAll(cell.getTables());
                }
            }
        }
        assertThat(found).as("a cell holding " + text).isNotNull();
        return found;
    }

    private static String allText(XWPFDocument document) {
        try (var extractor = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(document)) {
            return extractor.getText();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
