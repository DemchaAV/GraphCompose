package com.demcha.templates.invoice;

import com.demcha.mock.InvoiceDataFixtures;
import com.demcha.templates.builtins.InvoiceTemplateV1;
import com.demcha.templates.data.InvoiceData;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceTemplateV1RenderTest {

    private final InvoiceData data = InvoiceDataFixtures.standardInvoice();
    private final InvoiceTemplateV1 template = new InvoiceTemplateV1();

    @Test
    void shouldExposeTemplateMetadata() {
        assertThat(template.getTemplateId()).isEqualTo("invoice-v1");
        assertThat(template.getTemplateName()).isEqualTo("Invoice V1");
        assertThat(template.getDescription()).isNotBlank();
    }

    @Test
    void shouldRenderInvoiceAsDocument() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("invoice_render_document", "clean", "templates", "invoice");

        try (PDDocument document = template.render(data)) {
            document.save(outputFile.toFile());
        }

        assertPdfLooksValid(outputFile, 1);
    }

    @Test
    void shouldRenderInvoiceDirectlyToFile() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("invoice_render_file", "clean", "templates", "invoice");

        template.render(data, outputFile);

        assertPdfLooksValid(outputFile, 1);
    }

    @Test
    void shouldRenderInvoiceDirectlyToFileWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("invoice_render_file_with_guide_lines", "guides", "templates", "invoice");

        template.render(data, outputFile, true);

        assertPdfLooksValid(outputFile, 1);
    }

    private void assertPdfLooksValid(Path outputFile, int minPages) throws Exception {
        assertThat(outputFile).exists();
        assertThat(outputFile).isRegularFile();
        assertThat(outputFile).isNotEmptyFile();

        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isGreaterThanOrEqualTo(minPages);
        }
    }
}
