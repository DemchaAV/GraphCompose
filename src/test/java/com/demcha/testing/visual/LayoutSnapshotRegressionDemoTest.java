package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.snapshot.LayoutSnapshot;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.builtins.InvoiceTemplateV2;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.testing.layout.LayoutSnapshotJson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase E.4 — pins the deterministic-snapshot contract that
 * {@code examples/.../LayoutSnapshotRegressionExample} demonstrates.
 *
 * <p>The example walks adopters through the
 * {@link DocumentSession#layoutSnapshot()} → JSON → baseline workflow.
 * This test guards the underlying invariants so the example keeps
 * working as the layout pipeline evolves:</p>
 *
 * <ul>
 *   <li>Two renders of the same invoice produce byte-identical
 *       snapshot JSON. The renderer's PDFBox `/ID` randomness does
 *       <strong>not</strong> leak into the layout snapshot.</li>
 *   <li>Renders of different invoices produce different snapshots —
 *       the snapshot reflects content, not just structure.</li>
 *   <li>The snapshot reports a positive page count and a non-empty
 *       node list, proving extraction actually inspected the layout
 *       graph.</li>
 * </ul>
 *
 * @author Artem Demchyshyn
 */
class LayoutSnapshotRegressionDemoTest {

    @Test
    void identicalInvoicesProduceIdenticalSnapshotJson() throws Exception {
        String firstJson;
        String secondJson;
        try (DocumentSession a = newSession()) {
            new InvoiceTemplateV2(BusinessTheme.modern()).compose(a, sampleInvoice(1));
            firstJson = LayoutSnapshotJson.toJson(a.layoutSnapshot());
        }
        try (DocumentSession b = newSession()) {
            new InvoiceTemplateV2(BusinessTheme.modern()).compose(b, sampleInvoice(1));
            secondJson = LayoutSnapshotJson.toJson(b.layoutSnapshot());
        }
        assertThat(secondJson)
                .as("Two renders of the same input must produce byte-identical snapshot JSON — that is the whole point of layout snapshots")
                .isEqualTo(firstJson);
    }

    @Test
    void structurallyDifferentInvoicesProduceDifferentSnapshotJson() throws Exception {
        // Layout snapshots capture node structure, bounds, page count,
        // and margins — not text content. Differences therefore have
        // to come from something that changes the layout shape (extra
        // rows here changes both the node list and the page count).
        String oneRow;
        String tenRows;
        try (DocumentSession a = newSession()) {
            new InvoiceTemplateV2(BusinessTheme.modern()).compose(a, sampleInvoice(1));
            oneRow = LayoutSnapshotJson.toJson(a.layoutSnapshot());
        }
        try (DocumentSession b = newSession()) {
            new InvoiceTemplateV2(BusinessTheme.modern()).compose(b, sampleInvoice(20));
            tenRows = LayoutSnapshotJson.toJson(b.layoutSnapshot());
        }
        assertThat(tenRows)
                .as("Adding line items must change the snapshot — that's the regression signal adopters rely on")
                .isNotEqualTo(oneRow);
    }

    @Test
    void snapshotReportsPagesAndNodes() throws Exception {
        try (DocumentSession document = newSession()) {
            new InvoiceTemplateV2(BusinessTheme.modern()).compose(document, sampleInvoice(1));
            LayoutSnapshot snapshot = document.layoutSnapshot();
            assertThat(snapshot.totalPages()).isPositive();
            assertThat(snapshot.nodes()).isNotEmpty();
            assertThat(snapshot.canvas()).isNotNull();
        }
    }

    private static DocumentSession newSession() {
        BusinessTheme theme = BusinessTheme.modern();
        return GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .pageBackground(theme.pageBackground())
                .margin(DocumentInsets.of(28))
                .create();
    }

    private static InvoiceDocumentSpec sampleInvoice(int lineItemCount) {
        InvoiceDocumentSpec.Builder builder = InvoiceDocumentSpec.builder()
                .title("Invoice")
                .invoiceNumber("GC-2026-041")
                .issueDate("02 Apr 2026")
                .dueDate("16 Apr 2026")
                .reference("Snapshot demo")
                .status("Pending")
                .fromParty(party -> party
                        .name("GraphCompose Studio")
                        .addressLines("18 Layout Street", "London, UK", "EC1A 4GC")
                        .email("billing@graphcompose.dev"))
                .billToParty(party -> party
                        .name("Northwind Systems")
                        .addressLines("Attn: Finance Team", "Manchester, UK")
                        .email("ap@northwind.example"));
        for (int i = 0; i < lineItemCount; i++) {
            builder.lineItem("Line " + (i + 1),
                    "Stakeholder interviews",
                    "1",
                    "GBP 1,450",
                    "GBP 1,450");
        }
        return builder
                .summaryRow("Subtotal", "GBP " + (1450L * lineItemCount))
                .totalRow("Total", "GBP " + (1740L * lineItemCount))
                .footerNote("Snapshot regression demo.")
                .build();
    }
}
