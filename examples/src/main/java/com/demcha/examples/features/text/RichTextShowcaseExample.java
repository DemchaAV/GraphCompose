package com.demcha.examples.features.text;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for the canonical {@code RichText} DSL.
 *
 * <p>Walks through every fluent method on {@code RichText} —
 * {@code plain / bold / italic / boldItalic / underline / strikethrough /
 * color / accent / size / style / link / append / dot / ellipse / diamond /
 * star / shape} — laid out as labelled
 * "what does this look like" rows on a single A4 page so the rendered PDF
 * reads like a quick reference.</p>
 */
public final class RichTextShowcaseExample {
    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(112, 116, 128);
    private static final DocumentColor BRAND = DocumentColor.rgb(20, 80, 95);
    private static final DocumentColor ACCENT = DocumentColor.rgb(196, 153, 76);
    private static final DocumentColor PANEL = DocumentColor.rgb(248, 244, 234);

    private RichTextShowcaseExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/text", "rich-text-showcase.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(THEME.pageBackground())
                .margin(34, 34, 34, 34)
                .create()) {

            document.pageFlow()
                    .name("RichTextShowcase")
                    .spacing(14)
                    .addSection("Hero", section -> section
                            .softPanel(THEME.palette().surfaceMuted(), 10, 16)
                            .accentLeft(ACCENT, 4)
                            .spacing(6)
                            .addParagraph(p -> p
                                    .text("RichText showcase")
                                    .textStyle(THEME.text().h1())
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Every fluent ")
                                    .bold("RichText")
                                    .plain(" run available on the canonical surface, with the rendered ")
                                    .accent("output", BRAND)
                                    .plain(" right next to its source method.")))
                    .addSection("Plain runs", section -> labelledRow(section,
                            "plain / bold / italic",
                            rich -> rich
                                    .plain("Status: ")
                                    .bold("Pending")
                                    .plain(" — assigned to ")
                                    .italic("Q4 review")))
                    .addSection("Combined emphasis", section -> labelledRow(section,
                            "boldItalic / underline / strikethrough",
                            rich -> rich
                                    .boldItalic("Action required")
                                    .plain(" — see ")
                                    .underline("policy 4.3")
                                    .plain(" — supersedes ")
                                    .strikethrough("policy 4.2")))
                    .addSection("Colour and accent", section -> labelledRow(section,
                            "color / accent",
                            rich -> rich
                                    .plain("Severity: ")
                                    .color("HIGH", DocumentColor.rgb(178, 30, 30))
                                    .plain(" — risk window: ")
                                    .accent("48h", ACCENT)
                                    .plain(" — owner: ")
                                    .color("Audit team", BRAND)))
                    .addSection("Size shifts", section -> labelledRow(section,
                            "size",
                            rich -> rich
                                    .plain("Drop ")
                                    .size("important", 14.5)
                                    .plain(" or ")
                                    .size("subtle", 7.5)
                                    .plain(" runs inline without breaking the paragraph flow.")))
                    .addSection("Typed runs", section -> labelledRow(section,
                            "style",
                            rich -> rich
                                    .plain("Project ")
                                    .style("GraphCompose", DocumentTextStyle.builder()
                                            .fontName(FontName.HELVETICA_BOLD)
                                            .size(11)
                                            .color(BRAND)
                                            .build())
                                    .plain(" — code: ")
                                    .style("RT-2026-04", DocumentTextStyle.builder()
                                            .fontName(FontName.COURIER_BOLD)
                                            .size(10)
                                            .color(INK)
                                            .build())))
                    .addSection("Hyperlinks", section -> labelledRow(section,
                            "link",
                            rich -> rich
                                    .plain("Read the ")
                                    .link("template authoring cheatsheet",
                                            "https://github.com/DemchaAV/GraphCompose/blob/develop/docs/template-authoring.md")
                                    .plain(" or the ")
                                    .link("v1.6 roadmap",
                                            "https://github.com/DemchaAV/GraphCompose/blob/develop/docs/v1.6-roadmap.md")
                                    .plain(" for the next steps.")))
                    .addSection("Composing runs", section -> labelledRow(section,
                            "append",
                            rich -> rich
                                    .plain("Pre-built ")
                                    .append(reusableRun())
                                    .plain(" composes with ad-hoc fragments — share recurring fragments across paragraphs.")))
                    .addSection("Inline shapes", section -> labelledRow(section,
                            "dot / diamond / star / arrow / chevron / shape",
                            rich -> rich
                                    .plain("Java ")
                                    .dot(5, BRAND)
                                    .dot(5, BRAND)
                                    .dot(5, BRAND)
                                    .dot(5, null, DocumentStroke.of(BRAND, 0.6))
                                    .plain("    Step 1 ")
                                    .arrow(8, ShapeOutline.Direction.RIGHT, ACCENT)
                                    .plain(" Step 2    Home ")
                                    .chevron(6, ShapeOutline.Direction.RIGHT, MUTED)
                                    .plain(" Docs    ")
                                    .diamond(7, ACCENT)
                                    .plain(" ")
                                    .star(8, ACCENT)
                                    .plain(" ")
                                    .shape(ShapeOutline.checkmark(8, 8), BRAND)))
                    .addSection("Footer", section -> section
                            .accentTop(THEME.palette().rule(), 0.6)
                            .padding(new DocumentInsets(8, 0, 0, 0))
                            .addRich(rich -> rich
                                    .plain("Source: ")
                                    .style("examples/.../RichTextShowcaseExample.java",
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

    private static void labelledRow(com.demcha.compose.document.dsl.SectionBuilder section,
                                    String label,
                                    java.util.function.Consumer<com.demcha.compose.document.dsl.RichText> body) {
        section
                .softPanel(PANEL, 6, 12)
                .spacing(4)
                .addParagraph(p -> p
                        .text(label)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA_BOLD)
                                .size(8.5)
                                .color(MUTED)
                                .build())
                        .margin(DocumentInsets.zero()))
                .addRich(body::accept);
    }

    private static com.demcha.compose.document.dsl.RichText reusableRun() {
        return com.demcha.compose.document.dsl.RichText.text("RichText runs ")
                .bold("share state");
    }
}
