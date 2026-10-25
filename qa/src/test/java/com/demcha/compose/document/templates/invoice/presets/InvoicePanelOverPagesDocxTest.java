package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.templates.data.invoice.InvoiceServiceLines;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceData;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An invoice whose line items the layout sets over pages inside a card writes the card as a table a
 * page, each opening with the line items' header, as the page repeats it.
 *
 * <p>Measured in Word 16: written as one table, {@code ObsidianInvoice}'s and
 * {@code SubscriptionInvoice}'s line items stood 24 to 30pt high on the second page, their header
 * missing, and over three pages one row stood a page early, in LibreOffice 24.2 too.</p>
 */
class InvoicePanelOverPagesDocxTest {

    @Test
    void obsidiansLineItemsCardIsATableAPage() throws Exception {
        assertCutAtItsPages(ObsidianInvoiceFixtures.invoice(), 3, (s, d) -> ObsidianInvoice.create().compose(s, d),
                "LineItemsCard", "Description");
        assertCutAtItsPages(ObsidianInvoiceFixtures.invoice(), 6, (s, d) -> ObsidianInvoice.create().compose(s, d),
                "LineItemsCard", "Description");
    }

    @Test
    void subscriptionsRuledLineItemsAreATableAPage() throws Exception {
        assertCutAtItsPages(SubscriptionInvoiceFixtures.invoice(), 6,
                (s, d) -> SubscriptionInvoice.create().compose(s, d), "LineItemsRuled", "DESCRIPTION");
    }

    /**
     * Exports the invoice with its line items repeated {@code times} and checks that the card the
     * layout breaks over pages is as many tables as it has pages, each after the first set apart by
     * a paragraph that breaks the page before itself and opening with the line items' header.
     */
    private static void assertCutAtItsPages(StructuredInvoiceData data, int times,
                                            BiConsumer<DocumentSession, StructuredInvoiceData> preset,
                                            String card, String header) throws Exception {
        List<InvoiceServiceLines.Line> lines = new ArrayList<>();
        for (int time = 0; time < times; time++) {
            lines.addAll(data.serviceLines().lines());
        }
        StructuredInvoiceData longer = new StructuredInvoiceData(data.brand(), data.supplier(), data.masthead(),
                data.billTo(), data.shipTo(), data.summary(),
                new InvoiceServiceLines(data.serviceLines().columns(), lines), data.totals(), data.payment(),
                data.notes(), data.currencyCode());
        int pages;
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().create()) {
            preset.accept(session, longer);
            PlacedNode placed = session.layoutGraph().nodes().stream()
                    .filter(node -> card.equals(node.semanticName())).findFirst().orElseThrow();
            pages = placed.endPage() - placed.startPage() + 1;
            docx = session.export(new DocxSemanticBackend());
        }
        assertThat(pages).as("the premise: the layout sets %s over pages", card).isGreaterThan(1);
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            List<IBodyElement> body = document.getBodyElements();
            List<XWPFTable> parts = new ArrayList<>();
            for (int index = 0; index < body.size(); index++) {
                if (body.get(index) instanceof XWPFParagraph between && between.isPageBreak()) {
                    parts.add((XWPFTable) body.get(index + 1));
                }
            }
            assertThat(parts).as("%s's parts after the first, %d lines", card, lines.size()).hasSize(pages - 1);
            for (XWPFTable part : parts) {
                XWPFTable items = part.getRow(0).getCell(0).getTables().get(0);
                // The header's cells may hold tables of their own, which a cell's text leaves out.
                String first;
                try (org.apache.xmlbeans.XmlCursor row = items.getRow(0).getCtRow().newCursor()) {
                    first = row.getTextValue();
                }
                assertThat(first).as("its header first").contains(header);
            }
        }
    }
}
