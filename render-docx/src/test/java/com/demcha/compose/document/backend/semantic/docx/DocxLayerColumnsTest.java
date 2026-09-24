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
                    .isCloseTo(Math.round(gap * 20), org.assertj.core.data.Offset.offset(1L));
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

    /** One column as a full-width layer, inset to its band, as a two-column CV lays it out. */
    private static DocumentNode column(String name, double insetLeft, double insetRight,
                                       Consumer<SectionBuilder> content) {
        SectionBuilder layer = new SectionBuilder();
        layer.name(name).spacing(0).padding(new DocumentInsets(0, insetRight, 0, insetLeft));
        layer.addSection(name + "Content", section -> content.accept(section.spacing(0)));
        return layer.build();
    }

    private static Export export(Consumer<LayerStackBuilder> layers) throws Exception {
        DocumentSession session = GraphCompose.document()
                .pageSize(PAGE_WIDTH, 600)
                .margin(DocumentInsets.of(MARGIN))
                .create();
        session.pageFlow(page -> page.addLayerStack(stack -> layers.accept(stack.name("Columns"))));
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
