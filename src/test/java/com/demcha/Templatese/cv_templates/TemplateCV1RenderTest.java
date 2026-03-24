package com.demcha.Templatese.cv_templates;

import com.demcha.Templatese.data.MainPageCV;
import com.demcha.Templatese.template.MainPageCvDTO;
import com.demcha.Templatese.templates.Template_CV1;
import com.demcha.mock.MainPageCVMock;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateCV1RenderTest {

    private static final Path VISUAL_DIR = Path.of("target", "visual-tests");
    private final MainPageCV original = new MainPageCVMock().getMainPageCV();
    private final MainPageCvDTO rewritten = MainPageCvDTO.from(original);

    @Test
    void shouldRenderTemplateCvAsDocument() throws Exception {
        Path outputFile = VISUAL_DIR.resolve("template_cv_1_render_document.pdf");
        Files.createDirectories(VISUAL_DIR);
        Files.deleteIfExists(outputFile);

        Template_CV1 template = new Template_CV1();

        try (PDDocument document = template.render(original, rewritten)) {
            document.save(outputFile.toFile());
        }

        assertPdfLooksValid(outputFile);
    }

    @Test
    void shouldRenderTemplateCvDirectlyToFile() throws Exception {
        Path outputFile = VISUAL_DIR.resolve("template_cv_1_render_file.pdf");
        Files.createDirectories(VISUAL_DIR);
        Files.deleteIfExists(outputFile);

        Template_CV1 template = new Template_CV1();
        template.render(original, rewritten, outputFile);
        System.out.printf("Document saves %s",outputFile.toAbsolutePath());

        assertPdfLooksValid(outputFile);
    }
    @Test
    void shouldRenderTemplateCvDirectlyToFileWithGuideLines() throws Exception {
        Path outputFile = VISUAL_DIR.resolve("template_cv_1_render_file_with_guide_lines.pdf");
        Files.createDirectories(VISUAL_DIR);
        Files.deleteIfExists(outputFile);

        Template_CV1 template = new Template_CV1();
        template.render(original, rewritten, outputFile,true);
        System.out.printf("Document saves %s",outputFile.toAbsolutePath());

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
}
