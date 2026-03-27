package com.demcha.templates.cv_templates;

import com.demcha.mock.MainPageCVMock;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.builtins.EditorialBlueCvTemplate;
import com.demcha.templates.data.MainPageCV;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EditorialBlueCvTemplateRenderTest {
    private static final Path VISUAL_DIR = Path.of("target", "visual-tests");

    private final MainPageCV original = new MainPageCVMock().getMainPageCV();
    private final MainPageCvDTO rewritten = MainPageCvDTO.from(original);

    @Test
    void shouldRenderEditorialBlueTemplateToFile() throws Exception {
        Path outputFile = prepareOutputFile("editorial_blue_cv_render_file");

        EditorialBlueCvTemplate template = new EditorialBlueCvTemplate();
        template.render(original, rewritten, outputFile);

        assertPdfLooksValid(outputFile);
    }

    @Test
    void shouldRenderEditorialBlueTemplateToFileWithGuideLines() throws Exception {
        Path outputFile = prepareOutputFile("editorial_blue_cv_render_file_with_guide_lines");

        EditorialBlueCvTemplate template = new EditorialBlueCvTemplate();
        template.render(original, rewritten, outputFile, true);

        assertPdfLooksValid(outputFile);
    }

    private void assertPdfLooksValid(Path outputFile) throws Exception {
        assertThat(outputFile).exists();
        assertThat(outputFile).isRegularFile();
        assertThat(outputFile).isNotEmptyFile();

        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isGreaterThan(0);
        }
    }

    private static Path prepareOutputFile(String baseName) throws Exception {
        Files.createDirectories(VISUAL_DIR);
        Path outputFile = VISUAL_DIR.resolve(baseName + ".pdf");
        try {
            Files.deleteIfExists(outputFile);
            return outputFile;
        } catch (FileSystemException ignored) {
            return VISUAL_DIR.resolve(baseName + "_" + UUID.randomUUID() + ".pdf");
        }
    }
}
