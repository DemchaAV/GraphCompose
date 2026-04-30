package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.builtins.InvoiceTemplateV2;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.testing.VisualTestOutputs;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders the same {@link InvoiceDocumentSpec} via {@code InvoiceTemplateV2}
 * with each of the three built-in {@link BusinessTheme} themes. The output
 * lives under {@code target/visual-tests/invoice-template-v2/} so a reviewer
 * can flip through the three PDFs side-by-side and see the theme switch
 * visually.
 *
 * @author Artem Demchyshyn
 */
class InvoiceTemplateV2DemoTest {

    @Test
    void modernThemeRendersToValidPdf() throws Exception {
        renderTheme("invoice-modern", BusinessTheme.modern());
    }

    @Test
    void classicThemeRendersToValidPdf() throws Exception {
        renderTheme("invoice-classic", BusinessTheme.classic());
    }

    @Test
    void executiveThemeRendersToValidPdf() throws Exception {
        renderTheme("invoice-executive", BusinessTheme.executive());
    }

    private static void renderTheme(String stem, BusinessTheme theme) throws Exception {
        Path output = VisualTestOutputs.preparePdf(stem, "invoice-template-v2");
        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .pageBackground(theme.pageBackground())
                .margin(DocumentInsets.of(28))
                .create()) {
            new InvoiceTemplateV2(theme).compose(document, sampleInvoice());
            Files.write(output, document.toPdfBytes());
        }
        byte[] bytes = Files.readAllBytes(output);
        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
                .isEqualTo("%PDF-");
    }

    private static InvoiceDocumentSpec sampleInvoice() {
        return InvoiceDocumentSpec.builder()
                .title("Invoice")
                .invoiceNumber("GC-2026-041")
                .issueDate("02 Apr 2026")
                .dueDate("16 Apr 2026")
                .reference("Platform Refresh Sprint")
                .status("Pending")
                .fromParty(party -> party
                        .name("GraphCompose Studio")
                        .addressLines("18 Layout Street", "London, UK", "EC1A 4GC")
                        .email("billing@graphcompose.dev")
                        .phone("+44 20 5555 1000")
                        .taxId("GB-99887766"))
                .billToParty(party -> party
                        .name("Northwind Systems")
                        .addressLines("Attn: Finance Team", "410 Market Avenue", "Manchester, UK")
                        .email("ap@northwind.example")
                        .phone("+44 161 555 2200"))
                .lineItem("Discovery workshop", "Stakeholder interviews", "1", "GBP 1,450", "GBP 1,450")
                .lineItem("Template architecture", "Reusable document flows", "2", "GBP 980", "GBP 1,960")
                .lineItem("Render QA", "Visual validation passes", "3", "GBP 320", "GBP 960")
                .lineItem("Developer enablement", "Examples module + onboarding notes", "1", "GBP 780", "GBP 780")
                .summaryRow("Subtotal", "GBP 5,150")
                .summaryRow("VAT (20%)", "GBP 1,030")
                .totalRow("Total", "GBP 6,180")
                .note("Please include the invoice number on your remittance advice.")
                .note("All work was delivered as agreed during the April implementation window.")
                .paymentTerm("Payment due within 14 calendar days.")
                .paymentTerm("Bank transfer preferred.")
                .footerNote("Thank you for choosing GraphCompose for production document rendering.")
                .build();
    }
}
