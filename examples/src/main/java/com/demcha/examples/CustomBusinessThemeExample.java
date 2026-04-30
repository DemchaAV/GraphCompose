package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.templates.builtins.InvoiceTemplateV2;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.document.theme.DocumentPalette;
import com.demcha.compose.document.theme.SpacingScale;
import com.demcha.compose.document.theme.TablePreset;
import com.demcha.compose.document.theme.TextScale;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.awt.Color;
import java.nio.file.Path;

/**
 * Phase E.4 — runnable showcase that hand-builds a {@link BusinessTheme}
 * from scratch (rather than picking one of the {@code classic / modern
 * / executive} presets) and pipes it through {@link InvoiceTemplateV2}.
 *
 * <p>The theme assembled below ("Studio Emerald") is a deliberately
 * distinctive identity — emerald primary + copper accent on a warm
 * ivory paper — so when you open the generated invoice next to
 * {@link InvoiceCinematicFileExample} the difference is unmistakable.
 * The point is not the colour scheme itself but the demonstration that
 * <strong>every visible token on the invoice is driven from the theme
 * record</strong>: change a colour or font here and the invoice updates
 * without touching the template code.</p>
 *
 * <p>Use this example as a starter when you need to brand GraphCompose
 * output for your own project: copy the body of
 * {@link #studioEmeraldTheme()}, tweak the seven palette colours and
 * the seven text-scale styles, and you have a custom theme that any
 * {@code *TemplateV2} class can consume.</p>
 *
 * @author Artem Demchyshyn
 */
public final class CustomBusinessThemeExample {

    private CustomBusinessThemeExample() {
    }

    /**
     * Builds a custom business theme called "Studio Emerald".
     *
     * <p>The theme overrides every token rather than deriving from
     * {@link BusinessTheme#modern()} so the example shows the full
     * surface area a brand-customising consumer needs to fill in.</p>
     *
     * @return custom business theme
     */
    public static BusinessTheme studioEmeraldTheme() {
        // 1. Palette — semantic colour slots, all routed by role.
        DocumentPalette palette = DocumentPalette.of(
                new Color(20, 80, 60),     // primary  — deep emerald (titles, accent strip)
                new Color(176, 116, 56),   // accent   — warm copper (totals row, badges)
                new Color(252, 248, 240),  // surface  — warm ivory paper
                new Color(238, 232, 218),  // surfaceMuted — soft sand for soft panels / table headers
                new Color(34, 38, 44),     // textPrimary — near-black body
                new Color(110, 116, 124),  // textMuted — captions, metadata
                new Color(210, 200, 180)); // rule — soft sand-on-sand divider

        // 2. Spacing — slightly tighter than the default so a single
        //    invoice page reads compact without feeling cramped.
        SpacingScale spacing = new SpacingScale(4.0, 7.0, 11.0, 18.0, 30.0);

        // 3. Text scale — Times Roman headings + Helvetica body for a
        //    "studio editorial" tone. Each style references palette
        //    colours so the type system stays consistent with the
        //    palette above.
        TextScale text = new TextScale(
                style(FontName.TIMES_BOLD,     28, DocumentTextDecoration.BOLD, palette.primary()),
                style(FontName.TIMES_BOLD,     17, DocumentTextDecoration.BOLD, palette.primary()),
                style(FontName.HELVETICA_BOLD, 12, DocumentTextDecoration.BOLD, palette.textPrimary()),
                style(FontName.HELVETICA,      10, DocumentTextDecoration.DEFAULT, palette.textPrimary()),
                style(FontName.HELVETICA,       9, DocumentTextDecoration.DEFAULT, palette.textMuted()),
                style(FontName.HELVETICA_BOLD, 10, DocumentTextDecoration.BOLD, palette.textPrimary()),
                style(FontName.HELVETICA_BOLD, 10, DocumentTextDecoration.BOLD, palette.accent()));

        // 4. Table preset — common cell padding pulled from the
        //    spacing scale so changing `spacing.sm()` ripples
        //    through both the table and the rest of the document.
        DocumentInsets cellPadding = DocumentInsets.symmetric(spacing.xs(), spacing.sm());
        DocumentStroke rule = DocumentStroke.of(palette.rule(), 0.5);
        DocumentTableStyle baseCell = DocumentTableStyle.builder()
                .padding(cellPadding)
                .fillColor(palette.surface())
                .stroke(rule)
                .build();
        DocumentTableStyle headerCell = DocumentTableStyle.builder()
                .padding(cellPadding)
                .fillColor(palette.surfaceMuted())
                .stroke(rule)
                .build();
        DocumentTableStyle totalCell = DocumentTableStyle.builder()
                .padding(cellPadding)
                .fillColor(palette.surfaceMuted())
                .stroke(DocumentStroke.of(palette.accent(), 0.8))
                .build();
        DocumentTableStyle zebraCell = DocumentTableStyle.builder()
                .padding(cellPadding)
                .fillColor(palette.surfaceMuted())
                .stroke(rule)
                .build();
        TablePreset table = new TablePreset(baseCell, headerCell, totalCell, zebraCell);

        // 5. Compose the theme. The page background is the ivory
        //    surface so the entire page picks up the studio paper.
        return new BusinessTheme(
                "studio-emerald",
                palette,
                spacing,
                text,
                table,
                palette.surface());
    }

    /**
     * Renders {@link ExampleDataFactory#sampleInvoice()} through
     * {@link InvoiceTemplateV2} using {@link #studioEmeraldTheme()}.
     *
     * @return path to the generated PDF file
     * @throws Exception if writing the PDF fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("invoice-custom-theme.pdf");
        BusinessTheme theme = studioEmeraldTheme();
        InvoiceTemplateV2 template = new InvoiceTemplateV2(theme);

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(theme.pageBackground())
                .margin(28, 28, 28, 28)
                .create()) {
            template.compose(document, ExampleDataFactory.sampleInvoice());
            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }

    private static DocumentTextStyle style(FontName font,
                                           double size,
                                           DocumentTextDecoration decoration,
                                           DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .size(size)
                .decoration(decoration)
                .color(color)
                .build();
    }
}
