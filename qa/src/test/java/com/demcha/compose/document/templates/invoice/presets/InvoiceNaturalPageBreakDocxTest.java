package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * An invoice the layout breaks onto a new page by itself, after a card or a row, breaks there in
 * Word too: nothing in a card keeps it with what follows, and the block opening the next page
 * holds the space the layout leaves above it there.
 *
 * <p>Measured in Word 16 and LibreOffice 24.2. {@code ObsidianInvoice} with its line items repeated
 * twelve times sets its info cards at the foot of its fourth page and opens the fifth with its
 * footer divider, 13pt below the margin. Word set the cards on the fifth page with the divider,
 * 17 lines low, and both editors set the fifth page 13pt high. {@code MeteredInvoice} with its
 * line items repeated nine times opens its fourth page with its closing row 12.4pt below the
 * margin, the gap above it carried there; Word set that page 13.0pt high and LibreOffice 12.8pt.</p>
 */
class InvoiceNaturalPageBreakDocxTest {

    @Test
    void obsidiansInfoCardsKeepNothingWithTheDividerTheNextPageOpensWith() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            ObsidianInvoice.create().compose(session,
                    InvoiceDocxCorpus.withLinesRepeated(ObsidianInvoiceFixtures.invoice(), 12));
            LayoutGraph layout = session.layoutGraph();
            PlacedNode cards = named(layout, "InfoCardsRow");
            PlacedNode divider = named(layout, "FooterDivider");
            assertThat(divider.startPage()).as("the premise: the divider opens the page after the cards")
                    .isEqualTo(cards.endPage() + 1);
            double held = spaceAbove(layout, divider);
            assertThat(held).as("the premise: the divider's margin is on its page").isGreaterThan(12);

            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(
                    session.export(new DocxSemanticBackend())))) {
                List<IBodyElement> body = document.getBodyElements();
                XWPFTable row = tableReading(document, "Payment details");
                List<CTP> inside = paragraphsIn(row);
                assertThat(inside).as("the premise: the cards' paragraphs").hasSizeGreaterThan(10);
                // Word keeps a row with what follows its table where a paragraph a cell opens with
                // keeps with the next; one between two tables in a cell opens none, even after the
                // table a cell opens with (measured: a row at a page's foot stayed there either way).
                for (CTP paragraph : inside) {
                    if (paragraph.isSetPPr() && paragraph.getPPr().isSetKeepNext()) {
                        assertThat(followsATable(paragraph))
                                .as("only a paragraph after a table in a card keeps with the next").isTrue();
                    }
                }
                XWPFParagraph line = (XWPFParagraph) body.get(body.indexOf(row) + 1);
                XWPFParagraph rule = (XWPFParagraph) body.get(body.indexOf(row) + 2);
                assertThat(rule.getCTP().getPPr().isSetPBdr()).as("the divider, a paragraph's border").isTrue();
                assertHolds(line, held);
                assertThat(before(rule)).as("its edge is the line's").isZero();
            }
        }
    }

    @Test
    void meteredsClosingRowHoldsTheGapTheLayoutCarriesOntoItsPage() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            MeteredInvoice.create().compose(session,
                    InvoiceDocxCorpus.withLinesRepeated(MeteredInvoiceFixtures.invoice(), 9));
            LayoutGraph layout = session.layoutGraph();
            PlacedNode items = named(layout, "LineItems");
            PlacedNode closing = named(layout, "ClosingRow");
            assertThat(closing.startPage()).as("the premise: the closing row opens the page after the line items")
                    .isEqualTo(items.endPage() + 1);
            assertThat(closing.margin().top()).as("the premise: no edge of its own").isZero();
            double held = spaceAbove(layout, closing);
            assertThat(held).as("the premise: the gap above it carried onto its page").isGreaterThan(12);

            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(
                    session.export(new DocxSemanticBackend())))) {
                List<IBodyElement> body = document.getBodyElements();
                XWPFTable row = tableReading(document, "PAYMENT DETAILS");
                assertHolds((XWPFParagraph) body.get(body.indexOf(row) - 1), held);
            }
        }
    }

    private static void assertHolds(XWPFParagraph line, double points) {
        // The drawings the page anchors in its first paragraph may stand in it.
        assertThat(line.getRuns()).as("a line holding no text of its own")
                .allSatisfy(run -> assertThat(run.getCTR().sizeOfTArray()).isZero());
        CTSpacing spacing = line.getCTP().getPPr().getSpacing();
        assertThat(spacing.getLineRule()).isEqualTo(STLineSpacingRule.EXACT);
        assertThat(((BigInteger) spacing.getLine()).longValue()).as("the space above, a line's height")
                .isCloseTo(Math.round(points * 20), within(1L));
        assertThat(line.getCTP().getPPr().isSetKeepNext()).as("kept with what follows").isTrue();
    }

    private static long before(XWPFParagraph paragraph) {
        CTSpacing spacing = paragraph.getCTP().getPPr().getSpacing();
        return spacing == null || !spacing.isSetBefore() ? 0 : ((BigInteger) spacing.getBefore()).longValue();
    }

    /** How far below its page's top margin the layout starts a block, in points. */
    private static double spaceAbove(LayoutGraph layout, PlacedNode node) {
        double top = layout.pageGeometry().marginForPage(node.startPage()).top();
        return layout.canvas().height() - top - node.placementY() - node.placementHeight();
    }

    private static PlacedNode named(LayoutGraph layout, String name) {
        return layout.nodes().stream().filter(node -> name.equals(node.semanticName())).findFirst().orElseThrow();
    }

    /** The body table whose text reads {@code text}. */
    private static XWPFTable tableReading(XWPFDocument document, String text) {
        return document.getTables().stream().filter(table -> {
            try (org.apache.xmlbeans.XmlCursor cursor = table.getCTTbl().newCursor()) {
                return cursor.getTextValue().contains(text);
            }
        }).findFirst().orElseThrow();
    }

    /** Whether a paragraph comes right after a table in its cell: none of its cell's opens with it. */
    private static boolean followsATable(CTP paragraph) {
        try (org.apache.xmlbeans.XmlCursor cursor = paragraph.newCursor()) {
            return cursor.toPrevSibling() && cursor.getObject() instanceof
                    org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
        }
    }

    /** Every paragraph a table holds, at any depth. */
    private static List<CTP> paragraphsIn(XWPFTable table) {
        List<CTP> paragraphs = new ArrayList<>();
        for (org.apache.xmlbeans.XmlObject found : table.getCTTbl().selectPath(
                "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:p")) {
            paragraphs.add((CTP) found);
        }
        return paragraphs;
    }
}
