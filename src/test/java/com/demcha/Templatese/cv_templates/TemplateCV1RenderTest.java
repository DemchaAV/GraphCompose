package com.demcha.Templatese.cv_templates;

import com.demcha.Templatese.data.MainPageCV;
import com.demcha.mock.MainPageCVMock;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateCV1RenderTest {

    private static final Path VISUAL_DIR = Path.of("target", "visual-tests");
    private final MainPageCV data = new MainPageCVMock().getMainPageCV();

    @Test
    void shouldRenderTemplateCvWithGuidesEnabled() throws Exception {
        Path outputFile = VISUAL_DIR.resolve("template_cv_1_guides_on.pdf");
        renderAndAssert(true, outputFile);
    }

    @Test
    void shouldRenderTemplateCvWithGuidesDisabled() throws Exception {
        Path outputFile = VISUAL_DIR.resolve("template_cv_1_guides_off.pdf");
        renderAndAssert(false, outputFile);
    }

    private void renderAndAssert(boolean guideLines, Path outputFile) throws Exception {
        Files.createDirectories(VISUAL_DIR);

        new TemplateCV_1().process(data, outputFile, guideLines);

        assertThat(outputFile).exists();
        assertThat(outputFile).isRegularFile();
        assertThat(outputFile).isNotEmptyFile();

        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isGreaterThan(0);
        }
    }
}
