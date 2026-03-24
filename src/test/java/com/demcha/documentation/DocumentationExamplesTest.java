package com.demcha.documentation;

import com.demcha.Templatese.CvTheme;
import com.demcha.Templatese.TemplateBuilder;
import com.demcha.compose.GraphCompose;
import com.demcha.compose.font_library.FontName;
import com.demcha.compose.loyaut_core.components.ComponentBuilder;
import com.demcha.compose.loyaut_core.components.content.text.TextStyle;
import com.demcha.compose.loyaut_core.components.layout.Align;
import com.demcha.compose.loyaut_core.components.layout.Anchor;
import com.demcha.compose.loyaut_core.components.style.Margin;
import com.demcha.compose.loyaut_core.core.PdfComposer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentationExamplesTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRenderQuickStartExampleToFile() throws Exception {
        Path outputFile = tempDir.resolve("quick-start.pdf");

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(24, 24, 24, 24)
                .markdown(true)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();

            cb.vContainer(Align.middle(8))
                    .anchor(Anchor.topLeft())
                    .margin(Margin.of(8))
                    .addChild(cb.text()
                            .textWithAutoSize("Hello GraphCompose")
                            .textStyle(TextStyle.DEFAULT_STYLE)
                            .anchor(Anchor.topLeft())
                            .build())
                    .build();

            composer.build();
        }

        assertPdfFileLooksValid(outputFile);
    }

    @Test
    void shouldRenderInMemoryQuickStartExampleToBytes() throws Exception {
        byte[] pdfBytes;

        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(24, 24, 24, 24)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();

            cb.vContainer(Align.middle(8))
                    .anchor(Anchor.topLeft())
                    .margin(Margin.of(8))
                    .addChild(cb.text()
                            .textWithAutoSize("In-memory PDF")
                            .textStyle(TextStyle.DEFAULT_STYLE)
                            .anchor(Anchor.topLeft())
                            .build())
                    .build();

            pdfBytes = composer.toBytes();
        }

        assertPdfBytesLookValid(pdfBytes, tempDir.resolve("quick-start-bytes.pdf"));
    }

    @Test
    void shouldRenderTemplateBuilderExampleToBytes() throws Exception {
        byte[] pdfBytes;

        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(24, 24, 24, 24)
                .create()) {

            TemplateBuilder template = TemplateBuilder.from(
                    composer.componentBuilder(),
                    CvTheme.defaultTheme());

            template.moduleBuilder("Profile", composer.canvas())
                    .addChild(template.blockText(
                            "Analytical engineer focused on reliable platform design.",
                            composer.canvas().innerWidth()))
                    .build();

            pdfBytes = composer.toBytes();
        }

        assertPdfBytesLookValid(pdfBytes, tempDir.resolve("template-builder-bytes.pdf"));
    }

    @Test
    void shouldRenderAvailableFontsPreviewExample() throws Exception {
        Path outputFile = tempDir.resolve("available-fonts-preview.pdf");

        GraphCompose.renderAvailableFontsPreview(outputFile);

        assertThat(GraphCompose.availableFonts())
                .contains(FontName.HELVETICA, FontName.LATO, FontName.SPECTRAL);
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
}
