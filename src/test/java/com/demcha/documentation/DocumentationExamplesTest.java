package com.demcha.documentation;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.InvoiceTemplate;
import com.demcha.compose.document.templates.builtins.InvoiceTemplateV1;
import com.demcha.compose.document.templates.data.InvoiceData;
import com.demcha.compose.document.templates.data.InvoiceLineItem;
import com.demcha.compose.document.templates.data.InvoiceParty;
import com.demcha.compose.document.templates.data.InvoiceSummaryRow;
import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.components_builders.TableColumnSpec;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentationExamplesTest {
    private static final String LEGACY_RENDER_CALL = ".render(";
    private static final String LEGACY_PDF_ENTRYPOINT = "GraphCompose" + ".pdf(";
    private static final String LEGACY_PDF_COMPOSER = "Pdf" + "Composer";
    private static final String LEGACY_TEMPLATE_BUILDER = "Template" + "Builder";
    private static final String LEGACY_TEMPLATE_NAMESPACE = "com.demcha." + "templates";
    private static final String LEGACY_V2_NAMESPACE = "com.demcha.compose." + "v2";

    @Test
    void shouldRenderQuickStartExampleToFile() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("quick-start", "clean", "documentation");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(24, 24, 24, 24)
                .create()) {
            document.pageFlow()
                    .name("QuickStart")
                    .spacing(8)
                    .margin(Margin.of(8))
                    .addParagraph("Hello GraphCompose", TextStyle.DEFAULT_STYLE)
                    .build();

            document.buildPdf();
        }

        assertPdfFileLooksValid(outputFile);
    }

    @Test
    void shouldRenderInMemoryQuickStartExampleToBytes() throws Exception {
        byte[] pdfBytes;
        Path outputFile = VisualTestOutputs.preparePdf("quick-start-bytes", "clean", "documentation");

        try (DocumentSession document = GraphCompose.document()
                .pageSize(PDRectangle.A4)
                .margin(24, 24, 24, 24)
                .create()) {
            document.pageFlow()
                    .name("QuickStartBytes")
                    .spacing(8)
                    .margin(Margin.of(8))
                    .addText("In-memory PDF", TextStyle.DEFAULT_STYLE)
                    .build();

            pdfBytes = document.toPdfBytes();
        }

        assertPdfBytesLookValid(pdfBytes, outputFile);
    }

    @Test
    void shouldRenderSectionDslExampleToBytes() throws Exception {
        byte[] pdfBytes;
        Path outputFile = VisualTestOutputs.preparePdf("section-dsl-bytes", "clean", "documentation");

        try (DocumentSession document = GraphCompose.document()
                .pageSize(PDRectangle.A4)
                .margin(24, 24, 24, 24)
                .create()) {
            document.compose(dsl -> dsl.pageFlow(flow -> flow
                    .name("TemplateStyleFlow")
                    .spacing(12)
                    .addSection("Profile", section -> section
                            .spacing(6)
                            .addParagraph("Profile", TextStyle.DEFAULT_STYLE)
                            .addParagraph(paragraph -> paragraph
                                    .text("Analytical engineer focused on reliable platform design.")
                                    .padding(Padding.of(4))))));

            pdfBytes = document.toPdfBytes();
        }

        assertPdfBytesLookValid(pdfBytes, outputFile);
    }

    @Test
    void shouldRenderComposeFirstBuiltInTemplateExampleToFile() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("compose-first-invoice-template", "clean", "documentation");
        InvoiceTemplate template = new InvoiceTemplateV1();

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(22, 22, 22, 22)
                .create()) {
            template.compose(document, sampleInvoice());
            document.buildPdf();
        }

        assertPdfFileLooksValid(outputFile);
    }

    @Test
    void runnableExamplesShouldUseCanonicalDocumentTemplates() throws IOException {
        Path examplesRoot = Path.of("examples/src/main/java/com/demcha/examples").toAbsolutePath().normalize();

        try (var paths = Files.walk(examplesRoot)) {
            List<String> violations = new TreeSet<>(paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(this::containsLegacyTemplateOrPdfApi)
                    .map(path -> examplesRoot.relativize(path).toString().replace('\\', '/'))
                    .collect(Collectors.toList()))
                    .stream()
                    .toList();

            assertThat(violations).isEmpty();
        }
    }

    @Test
    void shouldRenderAvailableFontsPreviewExample() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("available-fonts-preview", "clean", "documentation");

        GraphCompose.renderAvailableFontsPreview(outputFile);

        assertThat(GraphCompose.availableFonts())
                .contains(FontName.HELVETICA, FontName.LATO, FontName.SPECTRAL);
        assertPdfFileLooksValid(outputFile);
    }

    @Test
    void shouldRenderLinePrimitiveExampleToFile() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("line-primitive", "clean", "documentation");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(24, 24, 24, 24)
                .create()) {
            document.pageFlow()
                    .name("LinePrimitives")
                    .spacing(12)
                    .margin(Margin.of(8))
                    .addDivider(divider -> divider
                            .name("HorizontalRule")
                            .width(220)
                            .thickness(3)
                            .color(ComponentColor.ROYAL_BLUE)
                            .padding(Padding.of(6)))
                    .addShape(shape -> shape
                            .name("VerticalAccent")
                            .size(3, 90)
                            .fillColor(ComponentColor.ORANGE)
                            .padding(Padding.of(6)))
                    .build();

            document.buildPdf();
        }

        assertPdfFileLooksValid(outputFile);
    }

    @Test
    void shouldRenderCanonicalTableExampleToFile() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("table-component", "clean", "documentation");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(24, 24, 24, 24)
                .create()) {
            document.pageFlow()
                    .name("StatusSection")
                    .spacing(12)
                    .addTable(table -> table
                            .name("StatusTable")
                            .columns(
                                    TableColumnSpec.fixed(90),
                                    TableColumnSpec.auto(),
                                    TableColumnSpec.auto())
                            .width(520)
                            .defaultCellStyle(TableCellStyle.builder()
                                    .padding(Padding.of(6))
                                    .build())
                            .headerStyle(TableCellStyle.builder()
                                    .fillColor(ComponentColor.LIGHT_GRAY)
                                    .padding(Padding.of(6))
                                    .build())
                            .header("Role", "Owner", "Status")
                            .rows(
                                    new String[]{"Engine", "GraphCompose", "Stable"},
                                    new String[]{"Feature", "Table Builder", "Canonical"}))
                    .build();

            document.buildPdf();
        }

        assertPdfFileLooksValid(outputFile);
    }

    private void assertPdfBytesLookValid(byte[] pdfBytes, Path outputFile) throws Exception {
        assertThat(pdfBytes).isNotEmpty();
        assertThat(pdfBytes.length).isGreaterThan(4);
        assertThat(new String(pdfBytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");

        Files.write(outputFile, pdfBytes);
        assertPdfFileLooksValid(outputFile);
    }

    private void assertPdfFileLooksValid(Path outputFile) throws Exception {
        assertThat(outputFile).exists();
        assertThat(outputFile).isRegularFile();
        assertThat(outputFile).isNotEmptyFile();

        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isGreaterThan(0);
        }
    }

    private boolean containsLegacyTemplateOrPdfApi(Path path) {
        try {
            String source = Files.readString(path);
            return source.contains(LEGACY_RENDER_CALL)
                    || source.contains(LEGACY_PDF_ENTRYPOINT)
                    || source.contains(LEGACY_PDF_COMPOSER)
                    || source.contains(LEGACY_TEMPLATE_BUILDER)
                    || source.contains(LEGACY_TEMPLATE_NAMESPACE)
                    || source.contains(LEGACY_V2_NAMESPACE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to inspect " + path, e);
        }
    }

    private InvoiceData sampleInvoice() {
        return new InvoiceData(
                "Invoice",
                "GC-2026-041",
                "02 Apr 2026",
                "16 Apr 2026",
                "Platform Refresh Sprint",
                "Pending",
                new InvoiceParty(
                        "GraphCompose Studio",
                        List.of("18 Layout Street", "London, UK", "EC1A 4GC"),
                        "billing@graphcompose.dev",
                        "+44 20 5555 1000",
                        "GB-99887766"),
                new InvoiceParty(
                        "Northwind Systems",
                        List.of("Attn: Finance Team", "410 Market Avenue", "Manchester, UK"),
                        "ap@northwind.example",
                        "+44 161 555 2200",
                        "NW-2026-01"),
                List.of(
                        new InvoiceLineItem("Discovery workshop", "Stakeholder interviews and current-state review", "1", "GBP 1,450", "GBP 1,450"),
                        new InvoiceLineItem("Template architecture", "Reusable document flows for invoice and proposal output", "2", "GBP 980", "GBP 1,960"),
                        new InvoiceLineItem("Render QA", "Visual validation and guideline passes", "3", "GBP 320", "GBP 960"),
                        new InvoiceLineItem("Developer enablement", "Examples module and onboarding notes", "1", "GBP 780", "GBP 780")),
                List.of(
                        new InvoiceSummaryRow("Subtotal", "GBP 5,150", false),
                        new InvoiceSummaryRow("VAT (20%)", "GBP 1,030", false),
                        new InvoiceSummaryRow("Total", "GBP 6,180", true)),
                List.of(
                        "Please include the invoice number on your remittance advice.",
                        "All work was delivered as agreed during the April implementation window."),
                List.of(
                        "Payment due within 14 calendar days.",
                        "Bank transfer preferred; contact billing@graphcompose.dev for remittance details.",
                        "Late payments may delay additional template customization work."),
                "Thank you for choosing GraphCompose for production document rendering.");
    }
}

