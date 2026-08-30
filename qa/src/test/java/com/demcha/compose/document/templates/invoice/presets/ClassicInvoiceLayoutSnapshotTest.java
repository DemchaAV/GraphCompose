package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exact layout snapshot gate for {@link ClassicInvoice} — freezes the
 * resolved node geometry the pixel budget cannot see (a small column or
 * spacing shift stays under the visual-diff budget but changes the
 * snapshot), and the pagination contract of the overflow invoice: the
 * forty-line-item fixture flows onto a second page with the repeated
 * table header, and the summary + footer follow the table.
 *
 * <p>The multi-page contract is guarded here at snapshot level only —
 * full-page pixel baselines drift across platforms far more than the
 * geometry they would guard (see the budget note in
 * {@code InvoiceV2VisualParityTest}), while the snapshot is exact on
 * every platform.</p>
 *
 * <p>Refresh with {@code -Dgraphcompose.updateSnapshots=true} after a
 * deliberate layout change, and commit the JSON with the change.</p>
 */
class ClassicInvoiceLayoutSnapshotTest {

    private static DocumentSession open() {
        float m = (float) ClassicInvoice.RECOMMENDED_MARGIN;
        return GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(m, m, m, m)
                .create();
    }

    @Test
    void canonicalInvoiceMatchesLayoutSnapshot() throws Exception {
        try (DocumentSession session = open()) {
            ClassicInvoice.create().compose(session, InvoicePresetFixtures.canonicalInvoice());
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(1);
            TemplateTestSupport.assertCanonicalSnapshot(
                    session, "classic_invoice_layout", "invoice");
        }
    }

    @Test
    void overflowInvoicePaginatesOntoSecondPage() throws Exception {
        try (DocumentSession session = open()) {
            ClassicInvoice.create().compose(session, InvoicePresetFixtures.stressInvoice());
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(2);
            TemplateTestSupport.assertCanonicalSnapshot(
                    session, "classic_invoice_stress_layout", "invoice");
        }
    }
}
