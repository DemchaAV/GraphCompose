package com.demcha.templates.cover_letter;

import com.demcha.templates.JobDetails;
import com.demcha.templates.data.Header;
import com.demcha.templates.builtins.CoverLetterTemplateV1;
import com.demcha.mock.CoverLetterMock;
import com.demcha.mock.MainPageCVMock;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CoverLetterTemplateV1Test {

    private final MainPageCVMock cvMock = new MainPageCVMock();
    private final String letter = CoverLetterMock.letter.replace("${companyName}", "Visual Test Company");
    private final CoverLetterTemplateV1 template = new CoverLetterTemplateV1();

    @Test
    void shouldExposeTemplateMetadata() {
        assertThat(template.getTemplateId()).isEqualTo("cover-letter-v1");
        assertThat(template.getTemplateName()).isEqualTo("Cover Letter V1");
        assertThat(template.getDescription()).isNotBlank();
    }

    @Test
    void shouldRenderCoverLetterAsDocument() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("cover_letter_render_document", "clean", "templates", "cover-letter");

        Header header = cvMock.getMainPageCV().getHeader();
        JobDetails jobDetails = testJobDetails();

        try (PDDocument document = template.render(header, letter, jobDetails)) {
            document.save(outputFile.toFile());
        }

        assertPdfLooksValid(outputFile);
    }

    @Test
    void shouldRenderCoverLetterDirectlyToFile() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("cover_letter_render_file", "clean", "templates", "cover-letter");

        Header header = cvMock.getMainPageCV().getHeader();
        JobDetails jobDetails = testJobDetails();

        template.render(header, letter, jobDetails, outputFile);

        assertPdfLooksValid(outputFile);
    }

    @Test
    void shouldRenderCoverLetterDirectlyToFileWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("cover_letter_render_file_with_guiede_lines", "guides", "templates", "cover-letter");

        Header header = cvMock.getMainPageCV().getHeader();
        JobDetails jobDetails = testJobDetails();

        template.render(header, letter, jobDetails, outputFile,true);

        assertPdfLooksValid(outputFile);
    }

    private JobDetails testJobDetails() {
        return new JobDetails(
                "https://linkedin.com/jobs/view/visual-test",
                "Software Engineer",
                "Visual Test Company",
                "Remote",
                "Visual verification test",
                "Mid",
                "Full-time");
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
