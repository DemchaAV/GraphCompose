package com.demcha.pdf_render;

import com.demcha.Templatese.JobDetails;
import com.demcha.Templatese.data.Header;
import com.demcha.Templatese.templates.CoverLetterTemplateV1;
import com.demcha.mock.CoverLetterMock;
import com.demcha.mock.MainPageCVMock;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CoverLetterTemplateV1Test {

    private static final Path VISUAL_DIR = Path.of("target", "visual-tests");

    private final MainPageCVMock cvMock = new MainPageCVMock();
    private final String letter = CoverLetterMock.letter.replace("${companyName}", "Visual Test Company");
    private final CoverLetterTemplateV1 template = new CoverLetterTemplateV1();

    @Test
    void shouldRenderCoverLetterAsDocument() throws Exception {
        Path outputFile = VISUAL_DIR.resolve("cover_letter_render_document.pdf");
        Files.createDirectories(VISUAL_DIR);
        Files.deleteIfExists(outputFile);

        Header header = cvMock.getMainPageCV().getHeader();
        JobDetails jobDetails = testJobDetails();

        try (PDDocument document = template.render(header, letter, jobDetails)) {
            document.save(outputFile.toFile());
        }

        assertPdfLooksValid(outputFile);
    }

    @Test
    void shouldRenderCoverLetterDirectlyToFile() throws Exception {
        Path outputFile = VISUAL_DIR.resolve("cover_letter_render_file.pdf");
        Files.createDirectories(VISUAL_DIR);
        Files.deleteIfExists(outputFile);

        Header header = cvMock.getMainPageCV().getHeader();
        JobDetails jobDetails = testJobDetails();

        template.render(header, letter, jobDetails, outputFile);

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
