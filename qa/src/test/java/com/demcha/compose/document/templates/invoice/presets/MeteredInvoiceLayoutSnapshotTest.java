package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MeteredInvoiceLayoutSnapshotTest {

    @Test
    void theInvoiceMatchesItsLayoutSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            MeteredInvoice.create().compose(session, MeteredInvoiceFixtures.invoice());
            assertThat(session.layoutSnapshot().totalPages()).as("total pages").isEqualTo(1);
            TemplateTestSupport.assertCanonicalSnapshot(
                    session, "metered_invoice_layout", "invoice");
        }
    }
}
