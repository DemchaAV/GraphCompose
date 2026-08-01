package com.demcha.examples.flagships;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RichText;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.examples.support.theme.BusinessTheme;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.examples.support.ExampleVersion;
import com.demcha.examples.support.PerfBaseline;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Kitchen-sink showcase combining the canonical surface end-to-end —
 * {@code BusinessTheme}, page background, rich text, advanced tables
 * (header / zebra / totals / repeated header on page break), shape
 * containers with clip-path and rotation, layered hero blocks, branded
 * QR + Code 128 barcodes, document metadata, and header / footer chrome.
 *
 * <p>The rendered PDF reads like a fictional "GraphCompose Q2 Sample
 * Report" — designed content meant to look at a glance
 * like the kind of business document GraphCompose was built to
 * generate, not a feature checklist. Use it as a reference when
 * composing your own multi-page documents.</p>
 *
 * <p>The narrative is deliberately undated. This file is regenerated and
 * re-committed on every release, so anything that names a version, a date or a
 * measurement would go stale between one release and the next — as it did. What
 * survives is either sample copy that cannot age or a figure read at render time
 * from {@link com.demcha.examples.support.PerfBaseline} and
 * {@link com.demcha.examples.support.ExampleVersion}.</p>
 */
public final class MasterShowcaseExample {
    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(112, 116, 128);
    private static final DocumentColor BRAND = DocumentColor.rgb(20, 80, 95);
    private static final DocumentColor BRAND_DEEP = DocumentColor.rgb(14, 56, 70);
    private static final DocumentColor ACCENT = DocumentColor.rgb(196, 153, 76);
    private static final DocumentColor SOFT_GOLD = DocumentColor.rgb(238, 221, 184);
    private static final DocumentColor PALE_TEAL = DocumentColor.rgb(232, 244, 245);
    private static final DocumentColor ZEBRA = DocumentColor.rgb(247, 243, 235);
    private static final DocumentColor TOTAL_FILL = DocumentColor.rgb(48, 92, 104);

    private MasterShowcaseExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("flagships", "master-showcase.pdf");

        try (DocumentSession document = document(outputFile).create()) {
            compose(document);
            document.buildPdf();
        }
        return outputFile;
    }

    /**
     * The example's canonical session setup, shared by the PDF {@link #generate()}
     * and its PPTX twin so both emit from an identically configured document —
     * the "one source, two formats" contract.
     *
     * @param outputFile the file the session writes to (.pdf or .pptx)
     * @return the configured builder, ready to {@code create()}
     */
    static GraphCompose.DocumentBuilder document(Path outputFile) {
        return GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(THEME.pageBackground())
                .margin(48, 34, 48, 34);
    }

    /**
     * Composes the master showcase into {@code document}. Shared by
     * {@link #generate()} and its PPTX twin so both lay out identical geometry.
     *
     * @param document the open session to compose into
     */
    static void compose(DocumentSession document) {

        document.metadata(DocumentMetadata.builder()
                .title("GraphCompose master showcase")
                .author("Jordan Rivera")
                .subject("Comprehensive end-to-end demo of the canonical document surface")
                .keywords("graphcompose, showcase, business, theme, rich-text, table, shape, transform, barcode")
                .creator("GraphCompose Examples")
                .producer("GraphCompose")
                .build());

        document.header(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.HEADER)
                .leftText("GraphCompose · Master showcase")
                // A fixed period rather than {date}: this document is committed as a
                // README preview, and a header that re-dates itself on every render
                // makes the committed copy differ from a fresh one for no reason a
                // reader would care about. The report is a Q2 fiction anyway.
                .rightText("Q2 2026")
                .fontSize(9f)
                .textColor(MUTED)
                .showSeparator(true)
                .separatorColor(THEME.palette().rule())
                .separatorThickness(0.5f)
                .build());

        document.footer(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.FOOTER)
                .leftText("GraphCompose — sample report")
                .rightText("Page {page} of {pages}")
                .fontSize(9f)
                .textColor(MUTED)
                .showSeparator(true)
                .separatorColor(THEME.palette().rule())
                .separatorThickness(0.5f)
                .build());

        document.pageFlow()
                .name("MasterShowcase")
                .spacing(14)

                // ───── Hero ─────
                .addSection("Hero", section -> section
                        .softPanel(THEME.palette().surfaceMuted(), 14, 22)
                        .accentLeft(ACCENT, 5)
                        .spacing(8)
                        .addParagraph(p -> p
                                .text("Q2 SAMPLE REPORT")
                                .textStyle(label())
                                .margin(DocumentInsets.zero()))
                        .addParagraph(p -> p
                                .text("Composing real documents with GraphCompose")
                                .textStyle(THEME.text().h1())
                                .margin(DocumentInsets.zero()))
                        .addRich(rich -> rich
                                .plain("A single Java DSL describes intent — ")
                                .bold("modules, sections, rows, tables, shapes, layers")
                                .plain(" — and the engine resolves layout, pagination, and ")
                                .accent("PDFBox rendering", BRAND)
                                .plain(" deterministically. This document is generated by one ")
                                .style("MasterShowcaseExample.java", DocumentTextStyle.builder()
                                        .fontName(FontName.COURIER)
                                        .size(9.5)
                                        .color(BRAND)
                                        .build())
                                .plain(" file.")))

                // ───── Hero badge row: shape container with rotated outline + branded QR ─────
                .addRow("HeroBadges", row -> row
                        .spacing(18)
                        .weights(2, 1, 1)
                        .addSection("Highlight", section -> section
                                .softPanel(DocumentColor.WHITE, 10, 16)
                                .stroke(DocumentStroke.of(THEME.palette().rule(), 0.6))
                                .accentLeft(ACCENT, 3)
                                .spacing(6)
                                .addParagraph(p -> p
                                        .text("Highlight")
                                        .textStyle(panelHeadline())
                                        .margin(DocumentInsets.zero()))
                                .addRich(rich -> rich
                                        .plain("Status: ")
                                        .bold("On track")
                                        .plain(" — ")
                                        .accent("every gate green", BRAND)
                                        .plain(" — release window confirmed for the ")
                                        .underline("current quarter")
                                        .plain("."))
                                .addRich(rich -> rich
                                        .plain("Performance: ")
                                        .color("invoice-template", BRAND_DEEP)
                                        .plain(" " + speed("invoice-template") + "; ")
                                        .color("feature-rich", BRAND_DEEP)
                                        .plain(" " + speed("feature-rich") + ". One machine, "
                                               + PerfBaseline.get().capturedOn() + ".")))
                        .addSection("Seal", section -> section
                                .padding(DocumentInsets.of(2))
                                .addCircle(118, BRAND, circle -> circle
                                        .name("RotatedSeal")
                                        .padding(8)
                                        .stroke(DocumentStroke.of(ACCENT, 1.5))
                                        .rotate(-12)
                                        .clipPolicy(ClipPolicy.CLIP_PATH)
                                        .center(label("APPROVED",
                                                style(FontName.HELVETICA_BOLD, 13,
                                                        DocumentTextDecoration.BOLD,
                                                        DocumentColor.WHITE)))
                                        .position(label("SAMPLE",
                                                        style(FontName.HELVETICA_BOLD, 7.5,
                                                                DocumentTextDecoration.BOLD,
                                                                SOFT_GOLD)),
                                                0, -16, LayerAlign.BOTTOM_CENTER)))
                        .addSection("Code", section -> section
                                .softPanel(PALE_TEAL, 8, 10)
                                .spacing(5)
                                .addParagraph(p -> p
                                        .text("Audit ID")
                                        .textStyle(label())
                                        .margin(DocumentInsets.zero()))
                                .addBarcode(barcode -> barcode
                                        .qrCode()
                                        .data("https://github.com/DemchaAV/GraphCompose")
                                        .foreground(BRAND)
                                        .background(PALE_TEAL)
                                        .quietZone(2)
                                        .size(106, 106))
                                .addParagraph(p -> p
                                        .text("DemchaAV/GraphCompose")
                                        .textStyle(monoCaption())
                                        .margin(DocumentInsets.zero()))))

                // ───── Executive summary ─────
                .addSection("Summary", section -> section
                        .softPanel(DocumentColor.WHITE, 10, 16)
                        .stroke(DocumentStroke.of(THEME.palette().rule(), 0.5))
                        .accentLeft(BRAND, 3)
                        .spacing(8)
                        .addParagraph(p -> p
                                .text("Executive summary")
                                .textStyle(THEME.text().h2())
                                .margin(DocumentInsets.zero()))
                        .addRich(rich -> rich
                                .plain("The features below are what turn GraphCompose from a \"tidy PDF layouter\" into a designed-document engine: ")
                                .bold("shape-as-container with clip path")
                                .plain(", ")
                                .bold("rotate / scale + per-layer z-index")
                                .plain(", ")
                                .bold("advanced tables (row span, zebra, totals, repeating header)")
                                .plain(", and two cinematic templates ")
                                .accent("ModernInvoice / ModernProposal", BRAND)
                                .plain(" driven by ")
                                .accent("BrandTheme", BRAND)
                                .plain(".")))

                // ───── Quarterly numbers — full advanced table ─────
                .addSection("Numbers", section -> section
                        .padding(DocumentInsets.zero())
                        .spacing(6)
                        .addParagraph(p -> p
                                .text("Quarterly numbers")
                                .textStyle(THEME.text().h2())
                                .margin(DocumentInsets.zero()))
                        .addParagraph(p -> p
                                .text("Repeated header + zebra rows + bold totals; the table paginates row-by-row across the next page so column titles stay visible.")
                                .textStyle(caption())
                                .lineSpacing(1.4)
                                .margin(new DocumentInsets(0, 0, 6, 0))))

                .addTable(table -> {
                    table.name("QuarterlyTable")
                            .columns(
                                    DocumentTableColumn.fixed(60),
                                    DocumentTableColumn.auto(),
                                    DocumentTableColumn.fixed(80),
                                    DocumentTableColumn.fixed(80),
                                    DocumentTableColumn.fixed(80))
                            .defaultCellStyle(DocumentTableStyle.builder()
                                    .padding(new DocumentInsets(7, 9, 7, 9))
                                    .textStyle(DocumentTextStyle.builder()
                                            .fontName(FontName.HELVETICA)
                                            .size(9.5)
                                            .color(INK)
                                            .build())
                                    .stroke(DocumentStroke.of(THEME.palette().rule(), 0.4))
                                    .build())
                            .headerStyle(DocumentTableStyle.builder()
                                    .padding(new DocumentInsets(8, 9, 8, 9))
                                    .textStyle(DocumentTextStyle.builder()
                                            .fontName(FontName.HELVETICA_BOLD)
                                            .size(9.5)
                                            .color(DocumentColor.WHITE)
                                            .build())
                                    .fillColor(BRAND_DEEP)
                                    .stroke(DocumentStroke.of(BRAND_DEEP, 0.4))
                                    .build())
                            .headerRow("Code", "Initiative", "Hours", "Owner", "Amount")
                            .repeatHeader()
                            .zebra(DocumentColor.WHITE, ZEBRA);
                    for (int i = 0; i < 24; i++) {
                        table.row(
                                String.format("INV-%03d", 100 + i),
                                "Initiative " + (i + 1) + " — quarterly delivery slice",
                                String.format("%d h", 40 + i * 3),
                                i % 3 == 0 ? "Studio" : i % 3 == 1 ? "Audit" : "Build",
                                String.format("£ %,d", 1_800 + i * 215));
                    }
                    table.totalRow(DocumentTableStyle.builder()
                                    .padding(new DocumentInsets(8, 9, 8, 9))
                                    .textStyle(DocumentTextStyle.builder()
                                            .fontName(FontName.HELVETICA_BOLD)
                                            .size(10)
                                            .color(DocumentColor.WHITE)
                                            .build())
                                    .fillColor(TOTAL_FILL)
                                    .stroke(DocumentStroke.of(TOTAL_FILL, 0.4))
                                    .build(),
                            "TOT", "Total quarterly spend", "—", "—", "£ 102,720");
                })

                // ───── Highlights row: layered card + accent rules ─────
                .addRow("HighlightsRow", row -> row
                        .spacing(14)
                        .weights(1, 1, 1)
                        .addSection("Card1", section -> highlightCard(section,
                                "Cinematic templates",
                                "ModernInvoice",
                                "Same data renders through a BrandTheme — pass a different theme to create(...), ship a new look."))
                        .addSection("Card2", section -> highlightCard(section,
                                "Shape-as-container",
                                "addCircle + clip path",
                                "ClipPolicy.CLIP_PATH clips children to the actual outline — DOCX falls back inline with a one-time capability warning."))
                        .addSection("Card3", section -> highlightCard(section,
                                "Transformable mixin",
                                "rotate / scale on every shape",
                                "Transformable<T> reaches every leaf builder, so any shape rotates around its placement centre.")))

                // ───── Action items + status legend ─────
                .addSection("ActionItems", section -> section
                        .softPanel(DocumentColor.WHITE, 8, 14)
                        .stroke(DocumentStroke.of(THEME.palette().rule(), 0.5))
                        .accentLeft(BRAND, 3)
                        .accentBottom(ACCENT, 2)
                        .spacing(8)
                        .addParagraph(p -> p
                                .text("Action items")
                                .textStyle(THEME.text().h2())
                                .margin(DocumentInsets.zero()))
                        .addRich(rich -> rich
                                .plain("• Publish the release notes once the branch ")
                                .bold("merges through review")
                                .plain(". ")
                                .accent("Owner: maintainer", BRAND)
                                .plain("."))
                        .addRich(rich -> rich
                                .plain("• Open ")
                                .bold("ADR 0003")
                                .plain(" for Phase A nested-list ergonomics. ")
                                .accent("Owner: contributor", BRAND)
                                .plain("."))
                        .addRich(rich -> rich
                                .plain("• Ship Phase B composed-cell content ahead of Phase C canvas. ")
                                .accent("Owner: contributor", BRAND)
                                .plain(".")))

                // ───── Footer audit code ─────
                .addRow("FooterRow", row -> row
                        .spacing(14)
                        .weights(2, 1)
                        .addSection("FooterText", section -> section
                                .accentTop(THEME.palette().rule(), 0.6)
                                .padding(new DocumentInsets(8, 0, 0, 0))
                                .addParagraph(p -> p
                                        .text("Source: examples/.../MasterShowcaseExample.java — every visible token routes through BusinessTheme.modern() so the same source renders any of classic / modern / executive themes.")
                                        .textStyle(caption())
                                        .lineSpacing(1.4)
                                        .margin(DocumentInsets.zero())))
                        .addSection("FooterCode", section -> section
                                .padding(DocumentInsets.zero())
                                .addBarcode(barcode -> barcode
                                        .code128()
                                        .data("GC-MASTER-SAMPLE")
                                        .foreground(BRAND)
                                        .size(180, 36))))
                .build();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }

    private static void highlightCard(SectionBuilder section,
                                      String title,
                                      String tag,
                                      String body) {
        section
                .softPanel(DocumentColor.WHITE, 8, 14)
                .stroke(DocumentStroke.of(THEME.palette().rule(), 0.5))
                .accentTop(BRAND, 3)
                .spacing(6)
                .addParagraph(p -> p
                        .text(title)
                        .textStyle(panelHeadline())
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p
                        .text(tag)
                        .textStyle(monoCaption())
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p
                        .text(body)
                        .textStyle(caption())
                        .lineSpacing(1.4)
                        .margin(DocumentInsets.zero()));
    }

    private static DocumentNode label(String text, DocumentTextStyle style) {
        return new ParagraphBuilder()
                .text(text)
                .textStyle(style)
                .align(TextAlign.CENTER)
                .margin(DocumentInsets.zero())
                .build();
    }

    /**
     * One scenario's measured speed, phrased for the report.
     *
     * <p>Restates the committed baseline rather than a literal, so the figure
     * moves when the measurement does instead of when somebody remembers.</p>
     *
     * @param scenario benchmark scenario name
     * @return e.g. {@code "6.6 ms avg, 152 docs/sec"}, or a plain note when the
     *         baseline does not carry the scenario
     */
    private static String speed(String scenario) {
        return PerfBaseline.get().scenario(scenario)
                .map(measured -> String.format(Locale.ROOT, "%.1f ms avg, %.0f docs/sec",
                        measured.avgMillis(), measured.docsPerSecond()))
                .orElse("not measured");
    }

    private static DocumentTextStyle label() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(8.5)
                .color(MUTED)
                .build();
    }

    private static DocumentTextStyle panelHeadline() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(11)
                .color(INK)
                .build();
    }

    private static DocumentTextStyle caption() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(8.6)
                .color(MUTED)
                .build();
    }

    private static DocumentTextStyle monoCaption() {
        return DocumentTextStyle.builder()
                .fontName(FontName.COURIER)
                .size(8.5)
                .color(BRAND)
                .build();
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
