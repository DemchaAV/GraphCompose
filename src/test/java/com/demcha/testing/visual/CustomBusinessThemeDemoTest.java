package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.templates.builtins.InvoiceTemplateV2;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.document.theme.DocumentPalette;
import com.demcha.compose.document.theme.SpacingScale;
import com.demcha.compose.document.theme.TablePreset;
import com.demcha.compose.document.theme.TextScale;
import com.demcha.compose.font.FontName;
import com.demcha.testing.VisualTestOutputs;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase E.4 — renders the same {@link InvoiceDocumentSpec} once with the
 * built-in {@link BusinessTheme#modern()} and once with a hand-built
 * "Studio Emerald" {@code BusinessTheme}. The output lives under
 * {@code target/visual-tests/custom-business-theme/} so a reviewer can
 * open the two PDFs side-by-side and confirm the custom theme really
 * does swap the visual identity (emerald primary + copper accent on
 * ivory paper) without touching the template code.
 *
 * <p>The {@link #studioEmeraldTheme()} method mirrors the body of
 * {@code examples/.../CustomBusinessThemeExample.studioEmeraldTheme()};
 * the example exists to be read, this test exists to keep that
 * authoring path honest.</p>
 *
 * @author Artem Demchyshyn
 */
class CustomBusinessThemeDemoTest {

    @Test
    void modernReferenceRendersToValidPdf() throws Exception {
        renderWithTheme("invoice-modern-reference", BusinessTheme.modern());
    }

    @Test
    void studioEmeraldRendersToValidPdf() throws Exception {
        renderWithTheme("invoice-studio-emerald", studioEmeraldTheme());
    }

    @Test
    void customThemeIsAcceptedByInvoiceTemplateV2() {
        BusinessTheme custom = studioEmeraldTheme();
        // The hand-built theme must satisfy InvoiceTemplateV2's contract
        // (i.e. all token slots populated) — constructing the template
        // without an exception proves it.
        InvoiceTemplateV2 template = new InvoiceTemplateV2(custom);
        assertThat(template).isNotNull();
        assertThat(custom.name()).isEqualTo("studio-emerald");
        assertThat(custom.pageBackground()).isNotNull();
    }

    private static void renderWithTheme(String stem, BusinessTheme theme) throws Exception {
        Path output = VisualTestOutputs.preparePdf(stem, "custom-business-theme");
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
        assertThat(new String(bytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    private static BusinessTheme studioEmeraldTheme() {
        DocumentPalette palette = DocumentPalette.of(
                new Color(20, 80, 60),
                new Color(176, 116, 56),
                new Color(252, 248, 240),
                new Color(238, 232, 218),
                new Color(34, 38, 44),
                new Color(110, 116, 124),
                new Color(210, 200, 180));
        SpacingScale spacing = new SpacingScale(4.0, 7.0, 11.0, 18.0, 30.0);
        TextScale text = new TextScale(
                style(FontName.TIMES_BOLD,     28, DocumentTextDecoration.BOLD, palette.primary()),
                style(FontName.TIMES_BOLD,     17, DocumentTextDecoration.BOLD, palette.primary()),
                style(FontName.HELVETICA_BOLD, 12, DocumentTextDecoration.BOLD, palette.textPrimary()),
                style(FontName.HELVETICA,      10, DocumentTextDecoration.DEFAULT, palette.textPrimary()),
                style(FontName.HELVETICA,       9, DocumentTextDecoration.DEFAULT, palette.textMuted()),
                style(FontName.HELVETICA_BOLD, 10, DocumentTextDecoration.BOLD, palette.textPrimary()),
                style(FontName.HELVETICA_BOLD, 10, DocumentTextDecoration.BOLD, palette.accent()));
        DocumentInsets cellPadding = DocumentInsets.symmetric(spacing.xs(), spacing.sm());
        DocumentStroke rule = DocumentStroke.of(palette.rule(), 0.5);
        TablePreset table = new TablePreset(
                DocumentTableStyle.builder()
                        .padding(cellPadding).fillColor(palette.surface()).stroke(rule).build(),
                DocumentTableStyle.builder()
                        .padding(cellPadding).fillColor(palette.surfaceMuted()).stroke(rule).build(),
                DocumentTableStyle.builder()
                        .padding(cellPadding).fillColor(palette.surfaceMuted())
                        .stroke(DocumentStroke.of(palette.accent(), 0.8)).build(),
                DocumentTableStyle.builder()
                        .padding(cellPadding).fillColor(palette.surfaceMuted()).stroke(rule).build());
        return new BusinessTheme("studio-emerald", palette, spacing, text, table, palette.surface());
    }

    private static DocumentTextStyle style(FontName font,
                                           double size,
                                           DocumentTextDecoration decoration,
                                           DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(font).size(size).decoration(decoration).color(color).build();
    }

    private static InvoiceDocumentSpec sampleInvoice() {
        return InvoiceDocumentSpec.builder()
                .title("Invoice")
                .invoiceNumber("GC-2026-041")
                .issueDate("02 Apr 2026")
                .dueDate("16 Apr 2026")
                .reference("Studio Emerald — Custom Theme")
                .status("Pending")
                .fromParty(party -> party
                        .name("GraphCompose Studio")
                        .addressLines("18 Layout Street", "London, UK", "EC1A 4GC")
                        .email("billing@graphcompose.dev")
                        .phone("+44 20 5555 1000"))
                .billToParty(party -> party
                        .name("Northwind Systems")
                        .addressLines("Attn: Finance Team", "410 Market Avenue", "Manchester, UK")
                        .email("ap@northwind.example"))
                .lineItem("Discovery workshop", "Stakeholder interviews", "1", "GBP 1,450", "GBP 1,450")
                .lineItem("Template architecture", "Reusable flows", "2", "GBP 980", "GBP 1,960")
                .lineItem("Render QA", "Visual validation passes", "3", "GBP 320", "GBP 960")
                .summaryRow("Subtotal", "GBP 4,370")
                .summaryRow("VAT (20%)", "GBP 874")
                .totalRow("Total", "GBP 5,244")
                .note("Brand-coloured invoice using Studio Emerald BusinessTheme.")
                .paymentTerm("Payment due within 14 calendar days.")
                .footerNote("Thank you for choosing GraphCompose for production document rendering.")
                .build();
    }
}
