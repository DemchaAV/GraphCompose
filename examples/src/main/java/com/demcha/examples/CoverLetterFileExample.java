package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Modern cinematic cover letter rendered directly through the canonical
 * DSL — `BusinessTheme.modern()` drives colour and type, sections use
 * v1.5 presets ({@code softPanel}, {@code accentLeft}, {@code accentTop})
 * for the visual hierarchy, and an opening rich-text strip highlights
 * the candidate's headline value proposition.
 */
public final class CoverLetterFileExample {
    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(112, 116, 128);
    private static final DocumentColor BRAND = DocumentColor.rgb(20, 80, 95);
    private static final DocumentColor ACCENT = DocumentColor.rgb(196, 153, 76);

    private CoverLetterFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("cover-letter.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(THEME.pageBackground())
                .margin(56, 48, 56, 48)
                .create()) {

            document.pageFlow()
                    .name("CoverLetter")
                    .spacing(18)

                    // Header band — sender identity + date.
                    .addRow("Header", row -> row
                            .spacing(14)
                            .weights(2, 1)
                            .addSection("Identity", section -> section
                                    .spacing(2)
                                    .addParagraph(p -> p
                                            .text("Mariia Demchyshyn")
                                            .textStyle(DocumentTextStyle.builder()
                                                    .fontName(FontName.HELVETICA_BOLD)
                                                    .size(22)
                                                    .color(INK)
                                                    .build())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text("Senior Backend Engineer · Document Tooling")
                                            .textStyle(headlineMuted())
                                            .margin(DocumentInsets.zero()))
                                    .addRich(rich -> rich
                                            .plain("mariia.demchyshyn@example.com  ·  +44 20 7946 0234  ·  ")
                                            .accent("github.com/mariia-d", BRAND)))
                            .addSection("Date", section -> section
                                    .padding(new DocumentInsets(2, 0, 0, 0))
                                    .addParagraph(p -> p
                                            .text("15 May 2026")
                                            .textStyle(DocumentTextStyle.builder()
                                                    .fontName(FontName.HELVETICA)
                                                    .size(10.5)
                                                    .color(MUTED)
                                                    .build())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text("Re: Senior Engineer · Documents")
                                            .textStyle(DocumentTextStyle.builder()
                                                    .fontName(FontName.HELVETICA_BOLD)
                                                    .size(10.5)
                                                    .color(BRAND)
                                                    .build())
                                            .margin(DocumentInsets.zero()))))

                    // Recipient block.
                    .addSection("Recipient", section -> section
                            .accentLeft(ACCENT, 3)
                            .padding(new DocumentInsets(2, 12, 2, 12))
                            .spacing(2)
                            .addParagraph(p -> p
                                    .text("Northwind Systems Ltd.")
                                    .textStyle(DocumentTextStyle.builder()
                                            .fontName(FontName.HELVETICA_BOLD)
                                            .size(11.5)
                                            .color(INK)
                                            .build())
                                    .margin(DocumentInsets.zero()))
                            .addParagraph(p -> p
                                    .text("Attention: Hiring Team — Document Platform")
                                    .textStyle(body())
                                    .margin(DocumentInsets.zero()))
                            .addParagraph(p -> p
                                    .text("180 Strand · London WC2R 1EA · United Kingdom")
                                    .textStyle(caption())
                                    .margin(DocumentInsets.zero())))

                    // Headline pull-quote — the value proposition.
                    .addSection("Headline", section -> section
                            .softPanel(THEME.palette().surfaceMuted(), 10, 18)
                            .accentLeft(ACCENT, 4)
                            .spacing(6)
                            .addRich(rich -> rich
                                    .plain("I help teams ship ")
                                    .style("designed PDFs as code", DocumentTextStyle.builder()
                                            .fontName(FontName.HELVETICA_BOLD)
                                            .size(13)
                                            .color(BRAND)
                                            .build())
                                    .plain(" — semantic Java DSL, deterministic layout, regression snapshots, and zero hand-tuned coordinates."))
                            .addRich(rich -> rich
                                    .plain("Most recently I shipped a templating engine for ")
                                    .accent("invoices, proposals, and CVs", BRAND)
                                    .plain(" used by 1.2M monthly documents in production.")))

                    // Salutation.
                    .addParagraph(p -> p
                            .text("Dear Hiring Team,")
                            .textStyle(DocumentTextStyle.builder()
                                    .fontName(FontName.HELVETICA_BOLD)
                                    .size(11.5)
                                    .color(INK)
                                    .build())
                            .margin(new DocumentInsets(8, 0, 0, 0)))

                    // Body paragraphs.
                    .addParagraph(p -> p
                            .text("I am writing to apply for the Senior Backend Engineer role on your Documents team. Over the past five years I have built and shipped PDF and DOCX rendering services that survive real production load, with deterministic regression coverage so design changes never silently break customer documents.")
                            .textStyle(body())
                            .lineSpacing(1.55)
                            .margin(new DocumentInsets(0, 0, 0, 0)))

                    .addParagraph(p -> p
                            .text("My current focus is on declarative document authoring — describing semantic intent and letting the engine resolve layout, pagination, and rendering. The same approach drives my open-source work on GraphCompose, where I separate semantic nodes from PDFBox rendering behind a public NodeDefinition seam, with snapshot tests that pin layout state before any byte is rendered.")
                            .textStyle(body())
                            .lineSpacing(1.55)
                            .margin(DocumentInsets.zero()))

                    // Highlights row — three quick cards.
                    .addRow("Highlights", row -> row
                            .spacing(12)
                            .weights(1, 1, 1)
                            .addSection("Hit1", section -> highlightCard(section,
                                    "Production scale",
                                    "1.2M docs / month",
                                    "Invoice + statement service for a fintech adopter; <0.01% render errors over 18 months."))
                            .addSection("Hit2", section -> highlightCard(section,
                                    "Tooling depth",
                                    "Java 21 · PDFBox 3 · Apache POI",
                                    "Backend-neutral output options, deterministic snapshots, visual regression harness."))
                            .addSection("Hit3", section -> highlightCard(section,
                                    "Design partnership",
                                    "Cinematic templates",
                                    "Tokenised business themes drive every visual choice — single source for invoice + proposal + CV.")))

                    // Closing paragraph.
                    .addParagraph(p -> p
                            .text("I would welcome the chance to walk through the architecture and show how it could fit your platform. Sample renders, benchmark numbers, and the underlying source are all linked from the address above.")
                            .textStyle(body())
                            .lineSpacing(1.55)
                            .margin(new DocumentInsets(8, 0, 0, 0)))

                    // Closing + signature.
                    .addSection("Closing", section -> section
                            .padding(new DocumentInsets(12, 0, 0, 0))
                            .spacing(2)
                            .addParagraph(p -> p
                                    .text("Sincerely,")
                                    .textStyle(body())
                                    .margin(DocumentInsets.zero()))
                            .addParagraph(p -> p
                                    .text("Mariia Demchyshyn")
                                    .textStyle(DocumentTextStyle.builder()
                                            .fontName(FontName.HELVETICA_BOLD)
                                            .size(13)
                                            .color(INK)
                                            .build())
                                    .margin(new DocumentInsets(2, 0, 0, 0)))
                            .addParagraph(p -> p
                                    .text("Senior Backend Engineer · Document Tooling")
                                    .textStyle(caption())
                                    .margin(DocumentInsets.zero())))

                    // Footer rule + audit line.
                    .addSection("Footer", section -> section
                            .accentTop(THEME.palette().rule(), 0.6)
                            .padding(new DocumentInsets(8, 0, 0, 0))
                            .addRich(rich -> rich
                                    .plain("Composed with GraphCompose v1.5 — ")
                                    .style("examples/.../CoverLetterFileExample.java", DocumentTextStyle.builder()
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

    private static void highlightCard(com.demcha.compose.document.dsl.SectionBuilder section,
                                      String title,
                                      String tag,
                                      String body) {
        section
                .softPanel(DocumentColor.WHITE, 6, 12)
                .stroke(DocumentStroke.of(THEME.palette().rule(), 0.5))
                .accentTop(BRAND, 2)
                .spacing(4)
                .addParagraph(p -> p
                        .text(title)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA_BOLD)
                                .size(10.5)
                                .color(INK)
                                .build())
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p
                        .text(tag)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.COURIER)
                                .size(8)
                                .color(BRAND)
                                .build())
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p
                        .text(body)
                        .textStyle(caption())
                        .lineSpacing(1.4)
                        .margin(DocumentInsets.zero()));
    }

    private static DocumentTextStyle body() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10.5)
                .color(INK)
                .build();
    }

    private static DocumentTextStyle headlineMuted() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(11)
                .color(MUTED)
                .build();
    }

    private static DocumentTextStyle caption() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(9)
                .color(MUTED)
                .build();
    }
}
