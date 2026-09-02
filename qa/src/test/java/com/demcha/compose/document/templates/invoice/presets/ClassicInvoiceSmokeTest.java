package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.invoice.InvoiceData;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the layered invoice pipeline through
 * {@link ClassicInvoice} — proves the preset renders an
 * {@link InvoiceDocumentSpec} end-to-end on a {@link BrandTheme}, via
 * both factory variants, with any theme, and on an empty invoice.
 */
class ClassicInvoiceSmokeTest {

    /** An invoice with no line items, summaries, notes, footer, or status — the empty paths. */
    private static InvoiceDocumentSpec minimalSpec() {
        return InvoiceDocumentSpec.from(InvoiceData.builder()
                .invoiceNumber("GC-2026-002")
                .fromParty(from -> from.name("GraphCompose Studio"))
                .billToParty(to -> to.name("Northwind Systems"))
                .build());
    }

    private static void render(DocumentTemplate<InvoiceDocumentSpec> template,
                               InvoiceDocumentSpec spec) throws Exception {
        float m = (float) ClassicInvoice.RECOMMENDED_MARGIN;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(m, m, m, m)
                .create()) {
            template.compose(session, spec);
            assertThat(session.roots()).isNotEmpty();
            // Drive layout + render, not just composition — the zero-row
            // summary table of an empty invoice only exists at layout time.
            assertThat(session.toPdfBytes()).isNotEmpty();
        }
    }

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<InvoiceDocumentSpec> template = ClassicInvoice.create();
        assertThat(template.id()).isEqualTo(ClassicInvoice.ID);
        assertThat(template.displayName()).isEqualTo(ClassicInvoice.DISPLAY_NAME);
    }

    @Test
    void defaultFactoryRendersWithInvoiceTheme() throws Exception {
        // create() wires BrandTheme.invoiceModern() — the variant the example uses.
        render(ClassicInvoice.create(), InvoicePresetFixtures.canonicalInvoice());
    }

    @Test
    void rendersWithExplicitTheme() throws Exception {
        render(ClassicInvoice.create(BrandTheme.invoiceModern()),
                InvoicePresetFixtures.canonicalInvoice());
    }

    @Test
    void readsAnyTheme() throws Exception {
        // Renders under a non-invoice theme without crashing: the header,
        // hero, labels, and footer follow the theme; the line-item body
        // cells inherit the DSL default (as in ModernInvoice).
        render(ClassicInvoice.create(BrandTheme.boxedClassic()),
                InvoicePresetFixtures.canonicalInvoice());
    }

    @Test
    void rendersEmptyInvoice() throws Exception {
        // Exercises the empty-collection paths through full layout and
        // render: the skipped Summary section (the engine rejects a
        // zero-row table), the empty note / payment lists under their
        // always-rendered headings, the skipped footer-note paragraph,
        // and the em-dash status fallback.
        render(ClassicInvoice.create(), minimalSpec());
    }
}
