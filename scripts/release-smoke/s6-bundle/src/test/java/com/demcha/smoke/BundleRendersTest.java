package com.demcha.smoke;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.invoice.presets.ModernInvoice;
import com.demcha.compose.emoji.GraphComposeEmoji;
import com.demcha.compose.font.DefaultFonts;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario 6 — the batteries-included {@code graph-compose-bundle} aggregate.
 * A single dependency renders a templated document through the PDF stack,
 * exposes the bundled font catalogue, and makes the colour-emoji set
 * resolvable — proving the bundle pulled the documented aggregate set
 * (wrapper + templates + fonts + emoji) transitively.
 */
class BundleRendersTest {

    @Test
    void bundleRendersTemplateAndExposesFontsAndEmoji() throws Exception {
        InvoiceDocumentSpec spec = InvoiceDocumentSpec.builder()
                .title("Bundle Smoke Invoice")
                .invoiceNumber("SMOKE-BUNDLE-001")
                .issueDate("2026-07-13")
                .dueDate("2026-08-13")
                .status("DUE")
                .fromParty(p -> p.name("Acme Studio"))
                .billToParty(p -> p.name("Client Ltd"))
                .lineItem("Consulting", "Bundle smoke test", "1", "$100.00", "$100.00")
                .totalRow("Total", "$100.00")
                .build();

        DocumentTemplate<InvoiceDocumentSpec> template = ModernInvoice.create();

        Path out = Files.createTempFile("gc-smoke-bundle", ".pdf");
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

        // Bundled font catalogue is on the classpath (graph-compose-fonts).
        assertThat(DefaultFonts.bundledFontNames()).isNotEmpty();

        // Colour-emoji set is resolvable (graph-compose-emoji).
        assertThat(GraphComposeEmoji.isAvailable()).isTrue();
    }
}
