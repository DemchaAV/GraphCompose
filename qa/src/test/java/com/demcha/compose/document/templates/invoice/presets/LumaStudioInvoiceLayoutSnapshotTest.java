package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exact layout snapshot gate for {@link LumaStudioInvoice} — freezes the
 * resolved geometry the pixel budget cannot see (a small column or spacing
 * shift stays under the visual-diff budget but changes the snapshot), and
 * the pagination contract of the overflow invoice: the service lines flow
 * onto further pages while the totals stack and each closing block stay
 * whole.
 *
 * <p>Refresh with {@code -Dgraphcompose.updateSnapshots=true} after a
 * deliberate layout change, and commit the JSON with the change.</p>
 */
class LumaStudioInvoiceLayoutSnapshotTest {

    @Test
    void canonicalInvoiceMatchesLayoutSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            LumaStudioInvoice.create().compose(
                    session, LumaStudioInvoiceFixtures.canonicalInvoice());
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(1);
            TemplateTestSupport.assertCanonicalSnapshot(
                    session, "luma_studio_invoice_layout", "invoice");
        }
    }

    @Test
    void overflowInvoicePaginatesOverThreePages() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            LumaStudioInvoice.create().compose(
                    session, LumaStudioInvoiceFixtures.overflowInvoice());
            // Three pages exactly: the table spans the first two, and the
            // closing blocks move whole onto the third rather than splitting.
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(3);
            TemplateTestSupport.assertCanonicalSnapshot(
                    session, "luma_studio_invoice_overflow_layout", "invoice");
        }
    }
}
