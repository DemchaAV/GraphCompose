package com.demcha.templates.cv_templates;

import com.demcha.mock.MainPageCVMock;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.builtins.EditorialBlueCvTemplate;
import com.demcha.templates.data.MainPageCV;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EditorialBlueCvTemplateRenderTest {

    private final MainPageCV original = new MainPageCVMock().getMainPageCV();
    private final MainPageCvDTO rewritten = MainPageCvDTO.from(original);

    @Test
    void shouldRenderEditorialBlueTemplateToFile() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("editorial_blue_cv_render_file", "clean", "templates", "cv");

        EditorialBlueCvTemplate template = new EditorialBlueCvTemplate();
        template.render(original, rewritten, outputFile);

        assertPdfLooksValid(outputFile);
    }

    @Test
    void shouldRenderEditorialBlueTemplateToFileWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("editorial_blue_cv_render_file_with_guide_lines", "guides", "templates", "cv");

        EditorialBlueCvTemplate template = new EditorialBlueCvTemplate();
        template.render(original, rewritten, outputFile, true);

        assertPdfLooksValid(outputFile);
    }

    private void assertPdfLooksValid(Path outputFile) throws Exception {
        assertThat(outputFile).exists();
        assertThat(outputFile).isRegularFile();
        assertThat(outputFile).isNotEmptyFile();

        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isEqualTo(1);
        }
    }
}
