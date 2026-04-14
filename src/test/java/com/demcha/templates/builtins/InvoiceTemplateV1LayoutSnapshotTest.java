package com.demcha.templates.builtins;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.mock.InvoiceDataFixtures;
import com.demcha.templates.data.InvoiceData;
import com.demcha.testing.VisualTestOutputs;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceTemplateV1LayoutSnapshotTest {

    private final InvoiceTemplateV1 template = new InvoiceTemplateV1();
    private final InvoiceData data = InvoiceDataFixtures.standardInvoice();

    @Test
    void shouldMatchInvoiceLayoutSnapshotAndRenderPdf() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("invoice_layout_snapshot", "clean", "templates", "invoice");

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(22, 22, 22, 22)
                .markdown(true)
                .create()) {
            template.compose(composer, data);
            LayoutSnapshotAssertions.assertMatches(composer, "templates/invoice/invoice_standard_layout");
            composer.build();
        }

        assertThat(outputFile).exists().isRegularFile().isNotEmptyFile();
        try (PDDocument document = Loader.loadPDF(outputFile.toFile())) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }
    }
}
