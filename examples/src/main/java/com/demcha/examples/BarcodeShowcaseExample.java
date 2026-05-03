package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.BarcodeBuilder;
import com.demcha.compose.document.node.BarcodeNode;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for every supported barcode type on the canonical
 * {@link BarcodeBuilder}: QR, Code 128, Code 39, EAN-13, EAN-8.
 *
 * <p>Each barcode is wrapped in a {@code ShapeContainerNode} sized to
 * the inner card width with the barcode anchored at {@code CENTER}, so
 * the rendered PDF shows every code visually centred inside its card
 * regardless of the code's natural aspect ratio.</p>
 */
public final class BarcodeShowcaseExample {
    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(112, 116, 128);
    private static final DocumentColor BRAND = DocumentColor.rgb(20, 80, 95);
    private static final DocumentColor ACCENT = DocumentColor.rgb(196, 153, 76);
    private static final DocumentColor SOFT_TEAL = DocumentColor.rgb(232, 244, 245);

    /**
     * A4 portrait inner width is 527pt (595 - 2 × 34pt margin). Two cards
     * per row with 14pt spacing → ~256.5pt per card. Card softPanel
     * padding is 14pt → ~228pt of content area. The barcode-centring
     * shape container uses this width so any barcode narrower than the
     * card slot ends up visually centred.
     */
    private static final double CARD_CONTENT_WIDTH = 228.0;

    private BarcodeShowcaseExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("barcode-showcase.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(THEME.pageBackground())
                .margin(34, 34, 34, 34)
                .create()) {

            document.pageFlow()
                    .name("BarcodeShowcase")
                    .spacing(14)
                    .addSection("Hero", section -> section
                            .softPanel(THEME.palette().surfaceMuted(), 10, 16)
                            .accentLeft(ACCENT, 4)
                            .spacing(6)
                            .addParagraph(p -> p
                                    .text("Barcode showcase")
                                    .textStyle(THEME.text().h1())
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Five encodings, one fluent ")
                                    .bold("BarcodeBuilder")
                                    .plain(": each format is a single ")
                                    .accent("type()", ACCENT)
                                    .plain(" call away.")))

                    // Top row: QR + Code 128
                    .addRow("Row1", row -> row
                            .spacing(14)
                            .weights(1, 1)
                            .addSection("QrCard", section -> barcodeCard(section,
                                    "QR Code",
                                    "qrCode()",
                                    "https://github.com/DemchaAV/GraphCompose",
                                    new BarcodeBuilder()
                                            .qrCode()
                                            .data("https://github.com/DemchaAV/GraphCompose")
                                            .size(150, 150)
                                            .build()))
                            .addSection("Code128Card", section -> barcodeCard(section,
                                    "Code 128",
                                    "code128()",
                                    "GC-INVOICE-2026-04-001",
                                    new BarcodeBuilder()
                                            .code128()
                                            .data("GC-INVOICE-2026-04-001")
                                            .size(220, 60)
                                            .build())))

                    // Middle row: Code 39 + EAN-13
                    .addRow("Row2", row -> row
                            .spacing(14)
                            .weights(1, 1)
                            .addSection("Code39Card", section -> barcodeCard(section,
                                    "Code 39",
                                    "code39()",
                                    "PROJECT-42",
                                    new BarcodeBuilder()
                                            .code39()
                                            .data("PROJECT-42")
                                            .size(220, 60)
                                            .build()))
                            .addSection("Ean13Card", section -> barcodeCard(section,
                                    "EAN-13",
                                    "ean13()",
                                    "5901234123457",
                                    new BarcodeBuilder()
                                            .ean13()
                                            .data("5901234123457")
                                            .size(190, 90)
                                            .build())))

                    // Bottom row: EAN-8 + branded QR
                    .addRow("Row3", row -> row
                            .spacing(14)
                            .weights(1, 1)
                            .addSection("Ean8Card", section -> barcodeCard(section,
                                    "EAN-8",
                                    "ean8()",
                                    "73513537",
                                    new BarcodeBuilder()
                                            .ean8()
                                            .data("73513537")
                                            .size(160, 90)
                                            .build()))
                            .addSection("BrandedQrCard", section -> barcodeCard(section,
                                    "Branded QR",
                                    "qrCode() + foreground/background",
                                    "Tinted with theme colours",
                                    new BarcodeBuilder()
                                            .qrCode()
                                            .data("https://demcha.io/graphcompose")
                                            .foreground(BRAND)
                                            .background(SOFT_TEAL)
                                            .quietZone(2)
                                            .size(150, 150)
                                            .build())))

                    .addSection("Footer", section -> section
                            .accentTop(THEME.palette().rule(), 0.6)
                            .padding(new DocumentInsets(8, 0, 0, 0))
                            .addParagraph(p -> p
                                    .text("Every barcode is a canonical DocumentNode — combine with rows, sections, layer stacks, and tables freely.")
                                    .textStyle(caption())
                                    .lineSpacing(1.4)
                                    .margin(DocumentInsets.zero())))
                    .build();

            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }

    private static void barcodeCard(com.demcha.compose.document.dsl.SectionBuilder section,
                                    String title,
                                    String factoryCall,
                                    String dataLabel,
                                    BarcodeNode barcode) {
        section
                .softPanel(DocumentColor.WHITE, 8, 14)
                .stroke(DocumentStroke.of(THEME.palette().rule(), 0.5))
                .spacing(8)
                .addParagraph(p -> p
                        .text(title)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA_BOLD)
                                .size(11)
                                .color(INK)
                                .build())
                        .margin(DocumentInsets.zero()))
                // Wrap in a shape container sized to the card content area —
                // OVERFLOW_VISIBLE skips clipping and CENTER alignment puts
                // the barcode at the visual midline regardless of its
                // natural aspect ratio.
                .addContainer(card -> card
                        .name(title.replace(' ', '_') + "_Center")
                        .rectangle(CARD_CONTENT_WIDTH, barcode.height())
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .center(barcode))
                .addParagraph(p -> p
                        .text(factoryCall)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.COURIER)
                                .size(8.5)
                                .color(BRAND)
                                .build())
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p
                        .text("Data: " + dataLabel)
                        .textStyle(caption())
                        .margin(DocumentInsets.zero()));
    }

    private static DocumentTextStyle caption() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(8.6)
                .color(MUTED)
                .build();
    }
}
