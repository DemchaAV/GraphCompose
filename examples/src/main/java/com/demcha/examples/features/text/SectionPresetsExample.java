package com.demcha.examples.features.text;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for the v1.4 / v1.5 section preset shortcuts.
 *
 * <p>Demonstrates {@code pageBackground}, {@code band(color)},
 * {@code softPanel(...)}, {@code headingBar(...)}, {@code accentLeft /
 * accentRight / accentTop / accentBottom}, and per-corner
 * {@code DocumentCornerRadius} on a single
 * designed A4 page so a reader can see the visual difference between
 * each preset side-by-side.</p>
 */
public final class SectionPresetsExample {
    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(112, 116, 128);
    private static final DocumentColor BRAND = DocumentColor.rgb(20, 80, 95);
    private static final DocumentColor ACCENT = DocumentColor.rgb(196, 153, 76);
    private static final DocumentColor PALE_PINK = DocumentColor.rgb(252, 235, 230);
    private static final DocumentColor SLATE = DocumentColor.rgb(64, 76, 92);

    private SectionPresetsExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/text", "section-presets.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(THEME.pageBackground())   // page background preset
                .margin(34, 34, 34, 34)
                .create()) {

            document.pageFlow()
                    .name("SectionPresetsShowcase")
                    .spacing(14)
                    // Hero — softPanel + accentLeft
                    .addSection("Hero", section -> section
                            .softPanel(THEME.palette().surfaceMuted(), 10, 16)
                            .accentLeft(ACCENT, 4)
                            .spacing(6)
                            .addParagraph(p -> p
                                    .text("Section presets")
                                    .textStyle(THEME.text().h1())
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Five fluent shortcuts on every flow / section / module: ")
                                    .bold("band")
                                    .plain(", ")
                                    .bold("softPanel")
                                    .plain(", ")
                                    .bold("accentLeft / Right / Top / Bottom")
                                    .plain(", plus the canvas-wide ")
                                    .bold("pageBackground")
                                    .plain(".")))
                    // band(color) — full-width strip
                    .addSection("Band", section -> section
                            .band(BRAND)
                            .padding(new DocumentInsets(10, 14, 10, 14))
                            .addParagraph(p -> p
                                    .text("band(BRAND)  — full-width section strip")
                                    .textStyle(headlineOnDark())
                                    .margin(DocumentInsets.zero())))
                    // softPanel default radius/padding
                    .addSection("Soft panel — default", section -> section
                            .softPanel(PALE_PINK)
                            .addParagraph(p -> p
                                    .text("softPanel(color)  — 8pt corner + 12pt padding")
                                    .textStyle(panelHeadline())
                                    .margin(DocumentInsets.zero())))
                    // softPanel custom radius/padding
                    .addSection("Soft panel — custom", section -> section
                            .softPanel(SLATE, 16, 20)
                            .addParagraph(p -> p
                                    .text("softPanel(color, radius=16, padding=20)")
                                    .textStyle(headlineOnDark())
                                    .margin(DocumentInsets.zero())))
                    // headingBar(text, bar -> ...) — filled title band above the body
                    .addSection("Heading bar", section -> section
                            .spacing(6)
                            .headingBar("EXPERIENCE", bar -> bar
                                    .fill(BRAND)
                                    .textStyle(headlineOnDark()))
                            .addParagraph(p -> p
                                    .text("headingBar(text, bar -> ...)  — a filled, rounded title band added above the body in one call")
                                    .textStyle(panelHeadline())
                                    .margin(DocumentInsets.zero())))
                    // accentLeft
                    .addSection("Accent left", section -> section
                            .softPanel(DocumentColor.WHITE, 6, 12)
                            .stroke(DocumentStroke.of(THEME.palette().rule(), 0.5))
                            .accentLeft(BRAND, 4)
                            .addParagraph(p -> p
                                    .text("accentLeft(BRAND, 4)  — vertical strip on the left edge")
                                    .textStyle(panelHeadline())
                                    .margin(DocumentInsets.zero())))
                    // accentRight
                    .addSection("Accent right", section -> section
                            .softPanel(DocumentColor.WHITE, 6, 12)
                            .stroke(DocumentStroke.of(THEME.palette().rule(), 0.5))
                            .accentRight(ACCENT, 4)
                            .addParagraph(p -> p
                                    .text("accentRight(ACCENT, 4)  — vertical strip on the right edge")
                                    .textStyle(panelHeadline())
                                    .margin(DocumentInsets.zero())))
                    // accentTop
                    .addSection("Accent top", section -> section
                            .softPanel(DocumentColor.WHITE, 6, 12)
                            .stroke(DocumentStroke.of(THEME.palette().rule(), 0.5))
                            .accentTop(BRAND, 3)
                            .addParagraph(p -> p
                                    .text("accentTop(BRAND, 3)  — horizontal rule above the panel content")
                                    .textStyle(panelHeadline())
                                    .margin(DocumentInsets.zero())))
                    // accentBottom
                    .addSection("Accent bottom", section -> section
                            .softPanel(DocumentColor.WHITE, 6, 12)
                            .stroke(DocumentStroke.of(THEME.palette().rule(), 0.5))
                            .accentBottom(ACCENT, 3)
                            .addParagraph(p -> p
                                    .text("accentBottom(ACCENT, 3)  — horizontal rule below the panel content")
                                    .textStyle(panelHeadline())
                                    .margin(DocumentInsets.zero())))
                    // Per-corner radius
                    .addSection("Per-corner radius", section -> section
                            .fillColor(DocumentColor.WHITE)
                            .stroke(DocumentStroke.of(THEME.palette().rule(), 0.5))
                            .cornerRadius(DocumentCornerRadius.of(0, 18, 18, 0))
                            .padding(new DocumentInsets(12, 14, 12, 14))
                            .addParagraph(p -> p
                                    .text("DocumentCornerRadius.of(0, 18, 18, 0)  — only right corners rounded")
                                    .textStyle(panelHeadline())
                                    .margin(DocumentInsets.zero())))
                    // Stacked accents
                    .addSection("Stacked", section -> section
                            .softPanel(DocumentColor.WHITE, 6, 14)
                            .stroke(DocumentStroke.of(THEME.palette().rule(), 0.5))
                            .accentLeft(BRAND, 3)
                            .accentBottom(ACCENT, 2)
                            .spacing(4)
                            .addParagraph(p -> p
                                    .text("Stacked accents")
                                    .textStyle(panelHeadline())
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("accentLeft + accentBottom compose without breaking the rounded corner — both edges paint independently of the soft panel.")))
                    // Footer note
                    .addSection("Footer", section -> section
                            .accentTop(THEME.palette().rule(), 0.6)
                            .padding(new DocumentInsets(8, 0, 0, 0))
                            .addParagraph(p -> p
                                    .text("Each preset is a thin alias over fillColor / cornerRadius / padding / DocumentBorders — nothing magic, just better-named.")
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

    private static DocumentTextStyle panelHeadline() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(11)
                .color(INK)
                .build();
    }

    private static DocumentTextStyle headlineOnDark() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(11)
                .color(DocumentColor.WHITE)
                .build();
    }

    private static DocumentTextStyle caption() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(8.6)
                .color(MUTED)
                .build();
    }
}
