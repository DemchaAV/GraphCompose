package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exact layout snapshot gate for {@link ConsultingInvoice} — freezes the
 * resolved geometry the pixel budget cannot see (a small column or spacing
 * shift stays under the visual-diff budget but changes the snapshot), and
 * the pagination contract of the overflow invoice: the service lines flow
 * onto a second page and the totals stack stays whole.
 *
 * <p>Refresh with {@code -Dgraphcompose.updateSnapshots=true} after a
 * deliberate layout change, and commit the JSON with the change.</p>
 */
class ConsultingInvoiceLayoutSnapshotTest {

    @Test
    void canonicalInvoiceMatchesLayoutSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            ConsultingInvoice.create().compose(
                    session, ConsultingInvoiceFixtures.canonicalInvoice());
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(1);
            TemplateTestSupport.assertCanonicalSnapshot(
                    session, "consulting_invoice_layout", "invoice");
        }
    }

    @Test
    void overflowInvoicePaginatesOntoASecondPage() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            ConsultingInvoice.create().compose(
                    session, ConsultingInvoiceFixtures.overflowInvoice());
            assertThat(session.layoutSnapshot().totalPages()).isGreaterThan(1);
            TemplateTestSupport.assertCanonicalSnapshot(
                    session, "consulting_invoice_overflow_layout", "invoice");
        }
    }
}
