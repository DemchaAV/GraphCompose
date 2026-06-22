package com.demcha.examples.features.text;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.RichText;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Runnable showcase for inline highlight "chips" ({@code @since 1.9.0}).
 *
 * <p>Styled text drawn on a rounded, padded background fill, on the text
 * baseline, flowing inside a paragraph — the GitHub inline {@code code} look and
 * inline status badges. {@code RichText.code(text)} ships engine defaults
 * (monospace + a light chip); {@code chip(text, fg, bg)} colours a badge; and
 * {@code highlight(text, style, bg, radius, padding)} is the full primitive.
 * On {@code ParagraphBuilder} the calls are {@code inlineCode} / {@code inlineChip}
 * / {@code inlineHighlight}.</p>
 */
public final class InlineHighlightExample {
    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final DocumentColor MUTED = DocumentColor.rgb(112, 116, 128);
    private static final DocumentColor BRAND = DocumentColor.rgb(20, 80, 95);
    private static final DocumentColor PANEL = DocumentColor.rgb(248, 244, 234);

    // Status-badge palette: ink + a soft tinted fill.
    private static final DocumentColor PAID_FG = DocumentColor.rgb(22, 101, 52);
    private static final DocumentColor PAID_BG = DocumentColor.rgb(220, 252, 231);
    private static final DocumentColor DUE_FG = DocumentColor.rgb(153, 27, 27);
    private static final DocumentColor DUE_BG = DocumentColor.rgb(254, 226, 226);
    private static final DocumentColor HOLD_FG = DocumentColor.rgb(146, 64, 14);
    private static final DocumentColor HOLD_BG = DocumentColor.rgb(254, 243, 199);

    private InlineHighlightExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/text", "inline-highlight-chips.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(THEME.pageBackground())
                .margin(34, 34, 34, 34)
                .create()) {

            document.pageFlow()
                    .name("InlineHighlightShowcase")
                    .spacing(14)
                    .addSection("Hero", section -> section
                            .softPanel(THEME.palette().surfaceMuted(), 10, 16)
                            .accentLeft(DocumentColor.rgb(97, 40, 217), 4)
                            .spacing(6)
                            .addParagraph(p -> p
                                    .text("Inline highlight chips")
                                    .textStyle(THEME.text().h1())
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Text on a rounded background, on the baseline ")
                                    .accent("— inline code and status badges", BRAND)
                                    .plain(". Add the ")
                                    .code("graph-compose")
                                    .plain(" dependency from ")
                                    .code("io.github.demchaav")
                                    .plain(".")))
                    .addSection("Code", section -> labelledRow(section,
                            "code(text) — monospace on a light chip, engine defaults",
                            rich -> rich
                                    .plain("Run ").code("./mvnw verify").plain(" then tag ")
                                    .code("v1.9.0").plain(" to publish ").code("graph-compose-emoji")))
                    .addSection("Badges", section -> labelledRow(section,
                            "chip(text, fg, bg) — a coloured status badge between words",
                            rich -> rich
                                    .chip(" Paid ", PAID_FG, PAID_BG).plain("     ")
                                    .chip(" Overdue ", DUE_FG, DUE_BG).plain("     ")
                                    .chip(" On hold ", HOLD_FG, HOLD_BG)))
                    .addSection("Custom", section -> labelledRow(section,
                            "highlight(text, style, bg, radius, padding) — the full primitive",
                            rich -> rich
                                    .plain("Pill ")
                                    .highlight("rounded", chipText(), DocumentColor.rgb(224, 231, 255),
                                            8.0, DocumentInsets.symmetric(2, 8))
                                    .plain("   square ")
                                    .highlight("sharp", chipText(), DocumentColor.rgb(255, 228, 230),
                                            0.0, DocumentInsets.symmetric(2, 6))))
                    .addSection("Wrapping", section -> labelledRow(section,
                            "a multi-word highlight wraps across lines — one continuous fill per fragment",
                            rich -> rich
                                    .plain("Reviewer note: ")
                                    .highlight("this longer highlighted phrase is intentionally verbose so that it "
                                                    + "spans more than one visual line, and the engine still paints a "
                                                    + "continuous rounded chip on each line fragment rather than one "
                                                    + "box per word",
                                            chipText(), DocumentColor.rgb(254, 249, 195),
                                            4.0, DocumentInsets.symmetric(2, 6))
                                    .plain(" — done.")))
                    .addSection("Footer", section -> section
                            .accentTop(THEME.palette().rule(), 0.6)
                            .padding(new DocumentInsets(8, 0, 0, 0))
                            .addRich(rich -> rich
                                    .plain("Source: ")
                                    .style("examples/.../InlineHighlightExample.java",
                                            DocumentTextStyle.builder()
                                                    .fontName(FontName.COURIER)
                                                    .size(8)
                                                    .color(MUTED)
                                                    .build())))
                    .build();

            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }

    private static void labelledRow(SectionBuilder section, String label, Consumer<RichText> body) {
        section
                .softPanel(PANEL, 6, 12)
                .spacing(4)
                .addParagraph(p -> p
                        .text(label)
                        .textStyle(caption())
                        .margin(DocumentInsets.zero()))
                .addRich(body::accept);
    }

    private static DocumentTextStyle chipText() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(9)
                .color(DocumentColor.rgb(55, 48, 163))
                .build();
    }

    private static DocumentTextStyle caption() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(8.5)
                .color(MUTED)
                .build();
    }
}
