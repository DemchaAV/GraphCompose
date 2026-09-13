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
 * Structural layout-snapshot gate for the {@link InvoiceDocumentSpec} invoice
 * presets without a snapshot suite of their own — the sibling of
 * {@link InvoiceV2VisualParityTest}, on the same
 * {@link InvoicePresetFixtures#canonicalInvoice()}.
 *
 * <p>Each preset composes the canonical invoice on A4 at its
 * {@code RECOMMENDED_MARGIN}, exactly as the pixel gate does, and the laid-out
 * node tree is compared against
 * {@code layout-snapshots/canonical-templates/invoice/<slug>_layout.json}. The
 * comparison is exact: node identity, nesting, page ownership and geometry.</p>
 *
 * <p>After a deliberate layout change, re-run with
 * {@code -Dgraphcompose.updateSnapshots=true} and commit the JSON with the
 * change. A mismatch writes the actual snapshot under
 * {@code target/visual-tests/layout-snapshots/} for diffing.</p>
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
            TemplateTestSupport.assertCanonicalSnapshot(document, slug + "_layout", "invoice");
        }
    }

    private static Stream<Arguments> presets() {
        return InvoicePresetFixtures.layoutSnapshotPresets();
    }
}
