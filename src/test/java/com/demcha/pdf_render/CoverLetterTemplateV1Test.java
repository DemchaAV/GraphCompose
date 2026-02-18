package com.demcha.pdf_render;

import com.demcha.Templatese.CoverLetterTemplate;
import com.demcha.Templatese.JobDetails;
import com.demcha.Templatese.data.Header;
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

    @Test
    void shouldRenderCoverLetterWithGuidesEnabled() throws Exception {
        Path outputFile = VISUAL_DIR.resolve("cover_letter_guides_on.pdf");
        renderAndSave(true, outputFile);
        assertPdfLooksValid(outputFile);
    }

    @Test
    void shouldRenderCoverLetterWithGuidesDisabled() throws Exception {
        Path outputFile = VISUAL_DIR.resolve("cover_letter_guides_off.pdf");
        renderAndSave(false, outputFile);
        assertPdfLooksValid(outputFile);
    }

    private void renderAndSave(boolean guideLines, Path outputFile) throws Exception {
        Files.createDirectories(VISUAL_DIR);
        Files.deleteIfExists(outputFile);

        Header header = cvMock.getMainPageCV().getHeader();
        JobDetails jobDetails = new JobDetails(
                "https://linkedin.com/jobs/view/visual-test",
                "Software Engineer",
                "Visual Test Company",
                "Remote",
                "Visual verification test",
                "Mid",
                "Full-time"
        );

        try (PDDocument document = new CoverLetterTemplate().render(header, letter, jobDetails, guideLines)) {
            document.save(outputFile.toFile());
        }
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
