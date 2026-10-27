package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.svg.SvgIcon;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.xmlbeans.XmlObject;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.STRelFromV;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.style.DocumentRowColumn.fixed;
import static com.demcha.compose.document.style.DocumentRowColumn.weight;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A drawing a node paints is anchored in a paragraph of the block it belongs to, so Word moves it
 * with that block when the text above is edited.
 *
 * <p>Chosen by where it stood, an icon laid over a card's corner was left to the page — the card's
 * text starts its padding below the icon's top, more than a shape was let rise above a paragraph
 * in a cell — and Word's editing protocol moved the card 36pt and the icon not at all. So was a
 * skill's bar beside its label in a row: in a cell, a shape was let rise above its paragraph by a
 * stroke's width only.</p>
 */
class DocxDrawingHolderTest {

    private static final SvgIcon ICON = SvgIcon.parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                                                      + "<circle cx='5' cy='5' r='5' fill='#1a5694'/></svg>");

    @Test
    void anIconLaidOverACardsCornerIsAnchoredInTheCardsText() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 500, 20, page -> page
                .addParagraph(p -> p.text("Before the card."))
                .addLayerStack(stack -> stack
                        .layer(new SectionBuilder().softPanel(DocumentColor.rgb(232, 240, 250), 0, 14)
                                .addParagraph(p -> p.text("The card's text.")).build())
                        .layer(ICON.node(18), LayerAlign.TOP_RIGHT))
                .addParagraph(p -> p.text("After the card.")))) {
            XWPFParagraph card = cellParagraphHolding(document, "The card's text.");
            List<CTAnchor> held = anchorsIn(card.getCTP());

            assertThat(held).as("the icon, in the card's paragraph").singleElement()
                    .satisfies(icon -> {
                        assertThat(icon.getLayoutInCell()).isTrue();
                        assertThat(icon.getPositionV().getRelativeFrom()).isEqualTo(STRelFromV.PARAGRAPH);
                        assertThat(points(icon.getPositionV().getPosOffset()))
                                .as("at the card's top, its padding above the text").isCloseTo(-14, within(1.0));
                    });
            assertThat(onThePage(document)).as("nothing left to the page").isZero();
        }
    }

    @Test
    void aMeterBesideItsLabelInARowIsAnchoredInTheRow() throws Exception {
        // A skill's meter: a track and its fill, drawn as lines in a stack of their own.
        DocumentNode meter = new LayerStackBuilder().name("Meter")
                .layer(new LineBuilder().horizontal(80).stroke(DocumentStroke.of(DocumentColor.rgb(220, 226, 236), 4)).build())
                .layer(new LineBuilder().horizontal(50).stroke(DocumentStroke.of(DocumentColor.rgb(26, 86, 148), 4)).build(),
                        LayerAlign.TOP_LEFT)
                .build();
        try (XWPFDocument document = DocxExports.withLayout(400, 500, 20, page -> page
                .addParagraph(p -> p.text("Skills"))
                .addRow(row -> row.columns(fixed(120), weight(1))
                        .addParagraph(p -> p.text("Negotiation"))
                        .add(meter)))) {
            assertThat(onThePage(document)).as("nothing left to the page").isZero();
            XWPFParagraph label = cellParagraphHolding(document, "Negotiation");
            List<CTAnchor> inTheRow = new ArrayList<>(anchorsIn(label.getCTP()));
            XWPFTableCell barCell = ((XWPFTableCell) label.getBody()).getTableRow().getCell(1);
            inTheRow.addAll(anchorsIn(barCell.getCTTc()));
            assertThat(inTheRow).as("the track and its fill, in their row").hasSize(2)
                    .allSatisfy(anchor -> assertThat(anchor.getLayoutInCell()).isTrue());
        }
    }

    @Test
    void theHoldersOfANodeAreItAndTheBlocksAboveItButNotThePagesFlow() throws Exception {
        DocumentNode icon = ICON.node(18);
        try (DocumentSession session = GraphCompose.document().pageSize(400, 500).margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(page -> page.addLayerStack(stack -> stack
                    .layer(new ParagraphBuilder().text("Text").build())
                    .layer(icon, LayerAlign.TOP_RIGHT)));
            DocxLayoutMetrics layout = DocxLayoutMetrics.of(session.documentGraph(), session.layoutGraph());
            String path = layout.pathOf(icon);

            assertThat(layout.holdersOf(path)).containsExactly(path, path.substring(0, path.lastIndexOf('/')));
            assertThat(layout.holdersOf("@timeline-rail")).as("no node's").isEmpty();
        }
    }

    @Test
    void aBlockTheTreeStartsAtAndAContainerFurtherInAreHolders() {
        // Added at the root, a section is a block, not the page's flow.
        DocumentNode icon = ICON.node(18);
        DocumentNode section = new SectionBuilder().name("Root")
                .add(new LayerStackBuilder().name("Stack").layer(icon).build()).build();
        DocxLayoutMetrics atTheRoot = DocxLayoutMetrics.of(
                new com.demcha.compose.document.layout.DocumentGraph(List.of(section)), null);
        assertThat(atTheRoot.holdersOf(atTheRoot.pathOf(icon)))
                .containsExactly(atTheRoot.pathOf(icon), "Root[0]/Stack[0]", "Root[0]");

        // A container the flow holds is a block; only the flow the tree starts at is left out.
        DocumentNode mark = ICON.node(18);
        DocumentNode inner = new com.demcha.compose.document.node.ContainerNode("Inner", List.of(mark), 0,
                DocumentInsets.zero(), DocumentInsets.zero(), null, null);
        DocumentNode flow = new com.demcha.compose.document.node.ContainerNode("Flow", List.of(inner), 0,
                DocumentInsets.zero(), DocumentInsets.zero(), null, null);
        DocxLayoutMetrics inAFlow = DocxLayoutMetrics.of(
                new com.demcha.compose.document.layout.DocumentGraph(List.of(flow)), null);
        assertThat(inAFlow.holdersOf(inAFlow.pathOf(mark))).containsExactly(inAFlow.pathOf(mark), "Flow[0]/Inner[0]");
    }

    /** The paragraph of a table cell whose text starts so. */
    private static XWPFParagraph cellParagraphHolding(XWPFDocument document, String text) {
        for (XmlObject found : document.getDocument().selectPath("declare namespace w='"
                                                                 + "http://schemas.openxmlformats.org/wordprocessingml/2006/main'"
                                                                 + " .//w:tc//w:p")) {
            XWPFParagraph paragraph = document.getParagraph((org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP) found);
            if (paragraph != null && paragraph.getText().startsWith(text)) {
                return paragraph;
            }
        }
        return document.getTables().stream()
                .flatMap(table -> table.getRows().stream())
                .flatMap(row -> row.getTableCells().stream())
                .flatMap(cell -> paragraphsIn(cell).stream())
                .filter(paragraph -> paragraph.getText().startsWith(text))
                .findFirst().orElseThrow(() -> new AssertionError("no cell paragraph starts '" + text + "'"));
    }

    private static List<XWPFParagraph> paragraphsIn(XWPFTableCell cell) {
        List<XWPFParagraph> paragraphs = new ArrayList<>(cell.getParagraphs());
        cell.getTables().forEach(table -> table.getRows().forEach(row -> row.getTableCells()
                .forEach(inner -> paragraphs.addAll(paragraphsIn(inner)))));
        return paragraphs;
    }

    /** How many drawings the body places down from the page's top edge. */
    private static long onThePage(XWPFDocument document) {
        return anchorsIn(document.getDocument().getBody()).stream()
                .filter(anchor -> anchor.getPositionV().getRelativeFrom() == STRelFromV.PAGE)
                .count();
    }

    private static List<CTAnchor> anchorsIn(XmlObject root) {
        List<CTAnchor> anchors = new ArrayList<>();
        for (XmlObject found : root.selectPath("declare namespace wp='"
                                               + "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing'"
                                               + " .//wp:anchor")) {
            anchors.add((CTAnchor) found);
        }
        return anchors;
    }

    private static double points(Object emu) {
        return ((Number) emu).doubleValue() / 12700.0;
    }
}
