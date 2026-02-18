package com.demcha.pdf_render;

import com.demcha.CoverLetterTemplate;
import com.demcha.JobDetails;
import com.demcha.Templatese.data.Header;
import com.demcha.mock.CoverLetterMock;
import com.demcha.mock.MainPageCVMock;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CoverLetterTemplateV1Test {

    @Mock
    private final MainPageCVMock mockCV = new MainPageCVMock();
    @Mock
    private String letter = CoverLetterMock.letter;
    ;

    @Test
    void renderPDDocument() {
        CoverLetterTemplate coverLetterTemplate = new CoverLetterTemplate();
        Header header = mockCV.getMainPageCV().getHeader();

        try (PDDocument document = coverLetterTemplate.render(header, letter, null)) {
            assertNotNull(document);
            assertTrue(document.getNumberOfPages() > 0);
        } catch (Exception e) {
            fail("Rendering failed: " + e.getMessage());
        }
    }

    @Test
    void testRenderSaveToDisk() throws IOException {
        CoverLetterTemplate coverLetterTemplate = new CoverLetterTemplate();
        Header header = mockCV.getMainPageCV().getHeader();
        JobDetails jobDetails = null;

        Path path = Files.createTempFile("cover_letter_visual_test_", ".pdf");


        assertDoesNotThrow(() -> coverLetterTemplate.
                render(header, letter, jobDetails));

        assertTrue(Files.exists(path));
        assertTrue(Files.isRegularFile(path));
        assertTrue(Files.size(path) > 0);
    }


}
