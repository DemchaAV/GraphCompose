package com.demcha.smoke;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.invoice.presets.ModernInvoice;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario 4 — a built-in template ({@code ModernInvoice}) from the opt-in
 * {@code graph-compose-templates} module composes over the canonical DSL and
 * renders through the PDF stack.
 */
class TemplatesRenderTest {

    @Test
    void modernInvoiceTemplateComposesAndRenders() throws Exception {
        InvoiceDocumentSpec spec = InvoiceDocumentSpec.builder()
                .title("Smoke Invoice")
                .invoiceNumber("SMOKE-001")
                .issueDate("2026-07-13")
                .dueDate("2026-08-13")
                .status("DUE")
                .fromParty(p -> p.name("Acme Studio"))
                .billToParty(p -> p.name("Client Ltd"))
                .lineItem("Consulting", "Release smoke test", "1", "$100.00", "$100.00")
                .totalRow("Total", "$100.00")
                .build();

        DocumentTemplate<InvoiceDocumentSpec> template = ModernInvoice.create();

        Path out = Files.createTempFile("gc-smoke-templates", ".pdf");
        try (DocumentSession document = GraphCompose.document(out)
                .pageSize(DocumentPageSize.A4)
                .margin(36f, 36f, 36f, 36f)
                .create()) {
            template.compose(document, spec);
            document.buildPdf();
        }

        assertThat(Files.size(out)).isGreaterThan(0L);
        byte[] head = Arrays.copyOf(Files.readAllBytes(out), 5);
        assertThat(new String(head)).isEqualTo("%PDF-");
    }
}
