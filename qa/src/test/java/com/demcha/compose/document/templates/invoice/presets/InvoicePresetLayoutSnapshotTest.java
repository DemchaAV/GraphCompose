package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Structural layout-snapshot gate for the layered invoice presets —
 * the sibling of {@link InvoiceV2VisualParityTest}.
 *
 * <p>Each preset composes the canonical {@link InvoiceDocumentSpec}
 * from {@link InvoicePresetFixtures} on A4 at the preset's
 * {@code RECOMMENDED_MARGIN}, and the resulting post-layout node tree
 * is compared against a checked-in baseline JSON under
 * {@code src/test/resources/layout-snapshots/canonical-templates/invoice/}.
 * The comparison is exact, so it pins node identity, parent/child
 * nesting, page assignment, and computed geometry — the drift the
 * loose pixel budget cannot see.</p>
 *
 * <p><strong>Re-blessing baselines</strong> — after a deliberate
 * layout change, re-run with
 * {@code -Dgraphcompose.updateSnapshots=true} and commit the updated
 * JSON as part of the same change.</p>
 */
class InvoicePresetLayoutSnapshotTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void matchesLayoutSnapshot(String slug,
                               double margin,
                               Supplier<DocumentTemplate<InvoiceDocumentSpec>> factory)
            throws Exception {
        DocumentTemplate<InvoiceDocumentSpec> template = factory.get();
        float m = (float) margin;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(m, m, m, m)
                .create()) {
            template.compose(document, InvoicePresetFixtures.canonicalInvoice());
            TemplateTestSupport.assertCanonicalSnapshot(document, slug, "invoice");
        }
    }

    private static Stream<Arguments> presets() {
        return InvoicePresetFixtures.presets();
    }
}
