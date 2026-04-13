package com.demcha.templates.builtins;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.mock.CoverLetterMock;
import com.demcha.mock.MainPageCVMock;
import com.demcha.templates.JobDetails;
import com.demcha.templates.data.Header;
import com.demcha.testing.layout.LayoutSnapshotAssertions;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

class CoverLetterTemplateV1LayoutSnapshotTest {

    private final MainPageCVMock cvMock = new MainPageCVMock();
    private final CoverLetterTemplateV1 template = new CoverLetterTemplateV1();

    @Test
    void shouldMatchCoverLetterLayoutSnapshot() throws Exception {
        Header header = cvMock.getMainPageCV().getHeader();
        String letter = CoverLetterMock.letter.replace("${companyName}", "Visual Test Company");

        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(15, 10, 15, 15)
                .markdown(true)
                .create()) {
            template.compose(composer, header, letter, testJobDetails());
            LayoutSnapshotAssertions.assertMatches(composer, "templates/cover-letter/cover_letter_standard");
        }
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
}
