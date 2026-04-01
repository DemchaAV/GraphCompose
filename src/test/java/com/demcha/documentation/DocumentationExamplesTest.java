package com.demcha.documentation;

import com.demcha.templates.CvTheme;
import com.demcha.templates.TemplateBuilder;
import com.demcha.compose.GraphCompose;
import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentationExamplesTest {

    @Test
    void shouldRenderQuickStartExampleToFile() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("quick-start", "clean", "documentation");

        try (DocumentComposer composer = GraphCompose.pdf(outputFile)
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
        Path outputFile = VisualTestOutputs.preparePdf("quick-start-bytes", "clean", "documentation");

        try (DocumentComposer composer = GraphCompose.pdf()
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

        assertPdfBytesLookValid(pdfBytes, outputFile);
    }

    @Test
    void shouldRenderTemplateBuilderExampleToBytes() throws Exception {
        byte[] pdfBytes;
        Path outputFile = VisualTestOutputs.preparePdf("template-builder-bytes", "clean", "documentation");

        try (DocumentComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(24, 24, 24, 24)
                .create()) {

            TemplateBuilder template = TemplateBuilder.from(
                    composer.componentBuilder(),
                    CvTheme.defaultTheme());

            var profile = template.moduleBuilder("Profile", composer.canvas())
                    .addChild(template.blockText(
                            "Analytical engineer focused on reliable platform design.",
                            composer.canvas().innerWidth()))
                    .build();

            template.pageFlow(composer.canvas())
                    .addChild(profile)
                    .build();

            pdfBytes = composer.toBytes();
        }

        assertPdfBytesLookValid(pdfBytes, outputFile);
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

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(24, 24, 24, 24)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();

            cb.vContainer(Align.left(12))
                    .anchor(Anchor.topLeft())
                    .margin(Margin.of(8))
                    .addChild(cb.line()
                            .horizontal()
                            .size(220, 16)
                            .padding(Padding.of(6))
                            .stroke(new Stroke(ComponentColor.ROYAL_BLUE, 3))
                            .anchor(Anchor.topLeft())
                            .build())
                    .addChild(cb.line()
                            .vertical()
                            .size(16, 90)
                            .padding(Padding.of(6))
                            .stroke(new Stroke(ComponentColor.ORANGE, 3))
                            .anchor(Anchor.topLeft())
                            .build())
                    .build();

            composer.build();
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
}

