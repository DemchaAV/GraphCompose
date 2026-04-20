package com.demcha.compose.document.templates.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.builtins.CoverLetterTemplateV1;
import com.demcha.compose.document.templates.builtins.CvTemplateV1;
import com.demcha.compose.document.templates.builtins.EditorialBlueCvTemplate;
import com.demcha.compose.document.templates.builtins.InvoiceTemplateV1;
import com.demcha.compose.document.templates.builtins.ProposalTemplateV1;
import com.demcha.compose.document.templates.builtins.WeeklyScheduleTemplateV1;
import com.demcha.compose.document.templates.data.CvDocumentSpec;
import com.demcha.compose.document.templates.data.Header;
import com.demcha.compose.document.templates.data.InvoiceData;
import com.demcha.compose.document.templates.data.JobDetails;
import com.demcha.compose.document.templates.data.MainPageCV;
import com.demcha.compose.document.templates.data.MainPageCvDTO;
import com.demcha.compose.document.templates.data.ProposalData;
import com.demcha.compose.document.templates.data.WeeklyScheduleData;
import com.demcha.mock.CoverLetterMock;
import com.demcha.mock.InvoiceDataFixtures;
import com.demcha.mock.MainPageCVMock;
import com.demcha.mock.ProposalDataFixtures;
import com.demcha.mock.WeeklyScheduleDataFixtures;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateComposeApiTest {

    @Test
    void cvTemplateInterfaceShouldExposeDocumentSessionComposeContract() throws Exception {
        assertMethodPresent(CvTemplate.class, "compose", DocumentSession.class, MainPageCV.class, MainPageCvDTO.class);
    }

    @Test
    void coverLetterTemplateInterfaceShouldExposeDocumentSessionComposeContract() throws Exception {
        assertMethodPresent(CoverLetterTemplate.class, "compose", DocumentSession.class, Header.class, String.class, JobDetails.class);
    }

    @Test
    void invoiceTemplateInterfaceShouldExposeDocumentSessionComposeContract() throws Exception {
        assertMethodPresent(InvoiceTemplate.class, "compose", DocumentSession.class, InvoiceData.class);
    }

    @Test
    void proposalTemplateInterfaceShouldExposeDocumentSessionComposeContract() throws Exception {
        assertMethodPresent(ProposalTemplate.class, "compose", DocumentSession.class, ProposalData.class);
    }

    @Test
    void weeklyScheduleTemplateInterfaceShouldExposeDocumentSessionComposeContract() throws Exception {
        assertMethodPresent(WeeklyScheduleTemplate.class, "compose", DocumentSession.class, WeeklyScheduleData.class);
    }

    @Test
    void cvTemplateRegistryShouldResolveCanonicalBuiltIns() {
        CvTemplate standard = new CvTemplateV1();
        CvTemplate editorial = new EditorialBlueCvTemplate();
        CvTemplateRegistry registry = new CvTemplateRegistry(List.of(standard, editorial));

        assertThat(registry.getDefaultTemplateId()).isEqualTo("modern-professional");
        assertThat(registry.hasTemplate("modern-professional")).isTrue();
        assertThat(registry.hasTemplate("editorial-blue")).isTrue();
        assertThat(registry.getTemplate("modern-professional")).isSameAs(standard);
        assertThat(registry.getTemplateOrDefault("missing", "editorial-blue")).isSameAs(editorial);
        assertThat(registry.getAllTemplates()).containsExactlyInAnyOrder(standard, editorial);
    }

    @Test
    void cvBuiltInsShouldComposeThroughDocumentSession() throws Exception {
        MainPageCV original = new MainPageCVMock().getMainPageCV();
        MainPageCvDTO rewritten = MainPageCvDTO.from(original);
        CvDocumentSpec composeFirst = CvDocumentSpec.from(original, rewritten);

        assertComposesToPdf(PDRectangle.A4, 24, document -> new CvTemplateV1().compose(document, original, rewritten));
        assertComposesToPdf(PDRectangle.A4, 24, document -> new CvTemplateV1().compose(document, composeFirst));
        assertComposesToPdf(PDRectangle.A4, 18, document -> new EditorialBlueCvTemplate().compose(document, original, rewritten));
    }

    @Test
    void cvTemplateV1ShouldExposeComposeFirstCvDocumentSpecOverload() throws Exception {
        assertMethodPresent(CvTemplateV1.class, "compose", DocumentSession.class, CvDocumentSpec.class);
    }

    @Test
    void coverLetterBuiltInShouldComposeThroughDocumentSession() throws Exception {
        MainPageCV original = new MainPageCVMock().getMainPageCV();
        String letter = CoverLetterMock.letter.replace("${companyName}", "Compose Path Ltd");
        JobDetails jobDetails = testJobDetails();

        assertComposesToPdf(PDRectangle.A4, 15, 10, 15, 15,
                document -> new CoverLetterTemplateV1().compose(document, original.getHeader(), letter, jobDetails));
    }

    @Test
    void invoiceBuiltInShouldComposeThroughDocumentSession() throws Exception {
        InvoiceData data = InvoiceDataFixtures.standardInvoice();
        assertComposesToPdf(PDRectangle.A4, 22, document -> new InvoiceTemplateV1().compose(document, data));
    }

    @Test
    void proposalBuiltInShouldComposeThroughDocumentSession() throws Exception {
        ProposalData data = ProposalDataFixtures.longProposal();
        assertComposesToPdf(PDRectangle.A4, 22, document -> new ProposalTemplateV1().compose(document, data));
    }

    @Test
    void weeklyScheduleBuiltInShouldComposeThroughDocumentSession() throws Exception {
        WeeklyScheduleData data = WeeklyScheduleDataFixtures.standardSchedule();
        PDRectangle landscapeA4 = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());

        assertComposesToPdf(landscapeA4, 18, document -> new WeeklyScheduleTemplateV1().compose(document, data));
    }

    private void assertComposesToPdf(PDRectangle pageSize, float margin, SessionAction action) throws Exception {
        assertComposesToPdf(pageSize, margin, margin, margin, margin, action);
    }

    private void assertComposesToPdf(PDRectangle pageSize,
                                     float top,
                                     float right,
                                     float bottom,
                                     float left,
                                     SessionAction action) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(pageSize)
                .margin(top, right, bottom, left)
                .create()) {
            action.apply(document);
            byte[] pdfBytes = document.toPdfBytes();
            assertPdfBytesLookValid(pdfBytes);
        }
    }

    private void assertMethodPresent(Class<?> type, String name, Class<?>... parameterTypes) throws Exception {
        Method method = type.getMethod(name, parameterTypes);
        assertThat(method).isNotNull();
        assertThat(method.isAnnotationPresent(Deprecated.class)).isFalse();
    }

    private void assertPdfBytesLookValid(byte[] pdfBytes) throws Exception {
        assertThat(pdfBytes).isNotEmpty();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(0);
        }
    }

    private JobDetails testJobDetails() {
        return new JobDetails(
                "https://linkedin.com/jobs/view/compose-path",
                "Software Engineer",
                "Compose Path Ltd",
                "Remote",
                "Compose-first compatibility test",
                "Mid",
                "Full-time");
    }

    @FunctionalInterface
    private interface SessionAction {
        void apply(DocumentSession document) throws Exception;
    }
}
