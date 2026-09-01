package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exact layout snapshot gate for {@link WorkspaceInvoice} — freezes the resolved
 * geometry of the canonical one-page invoice, which the pixel budget cannot
 * see: a small column or spacing shift stays under the visual-diff budget but
 * changes the snapshot.
 *
 * <p>The preset owns its page geometry, so the session starts unconfigured.</p>
 *
 * <p>Refresh with {@code -Dgraphcompose.updateSnapshots=true} after a
 * deliberate layout change, and commit the JSON with the change.</p>
 */
class WorkspaceInvoiceLayoutSnapshotTest {

    @Test
    void canonicalInvoiceMatchesLayoutSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            WorkspaceInvoice.create().compose(session,
                    WorkspaceInvoiceFixtures.canonicalInvoice());
            assertThat(session.layoutSnapshot().totalPages()).as("total pages").isEqualTo(1);
            TemplateTestSupport.assertCanonicalSnapshot(
                    session, "workspace_invoice_layout", "invoice");
        }
    }
}
