package com.demcha.compose.document.templates.receipt.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.templates.TemplateTestSupport;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.receipt.ReceiptDocumentSpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exact layout snapshot gate for {@link ModernReceipt} — freezes the resolved geometry of
 * the canonical receipt.
 *
 * <p>The receipt family shipped with a pixel gate only, and a pixel budget cannot say where
 * a box went: it absorbs a small shift and reports the same number for a moved hairline as
 * for a recoloured one. The receipt is built from measured text — a status chip sized to its
 * label, spaced-caps group titles, a hero amount, and the rail of the status trail — so the
 * geometry is the part that has to hold.</p>
 *
 * <p>Composed onto the same page size, margin and branded factory as
 * {@link ReceiptVisualParityTest}, from the same {@link ReceiptFixtures} spec, so the two
 * gates describe one document rather than two that may drift apart.</p>
 *
 * <p>Refresh with {@code -Dgraphcompose.updateSnapshots=true} after a deliberate layout
 * change, and commit the JSON with the change.</p>
 */
class ModernReceiptLayoutSnapshotTest {

    @Test
    void canonicalReceiptMatchesLayoutSnapshot() throws Exception {
        float margin = (float) ModernReceipt.RECOMMENDED_MARGIN;
        DocumentTemplate<ReceiptDocumentSpec> template = ModernReceipt.create(
                BrandTheme.receiptModern(),
                ModernReceipt.Options.branded(null, DocumentColor.rgb(23, 92, 211)));
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(margin, margin, margin, margin)
                .create()) {
            template.compose(session, ReceiptFixtures.canonicalReceipt());
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(1);
            TemplateTestSupport.assertCanonicalSnapshot(
                    session, "modern_receipt_layout", "receipt");
        }
    }
}
