package com.demcha.templates.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.mock.CoverLetterMock;
import com.demcha.mock.InvoiceDataFixtures;
import com.demcha.mock.MainPageCVMock;
import com.demcha.mock.ProposalDataFixtures;
import com.demcha.mock.WeeklyScheduleDataFixtures;
import com.demcha.templates.JobDetails;
import com.demcha.templates.builtins.CoverLetterTemplateV1;
import com.demcha.templates.builtins.CvTemplateV1;
import com.demcha.templates.builtins.EditorialBlueCvTemplate;
import com.demcha.templates.builtins.InvoiceTemplateV1;
import com.demcha.templates.builtins.ProposalTemplateV1;
import com.demcha.templates.builtins.WeeklyScheduleTemplateV1;
import com.demcha.templates.data.MainPageCV;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateComposeApiTest {

    @Test
    void cvTemplateInterfaceShouldExposeComposeAndDeprecatedRenderMethods() throws Exception {
        assertMethodPresent(CvTemplate.class, "compose", DocumentComposer.class, MainPageCV.class, MainPageCvDTO.class);
        assertDeprecated(CvTemplate.class.getMethod("render", MainPageCV.class, MainPageCvDTO.class));
        assertDeprecated(CvTemplate.class.getMethod("render", MainPageCV.class, MainPageCvDTO.class, boolean.class));
        assertDeprecated(CvTemplate.class.getMethod("render", MainPageCV.class, MainPageCvDTO.class, Path.class));
        assertDeprecated(CvTemplate.class.getMethod("render", MainPageCV.class, MainPageCvDTO.class, Path.class, boolean.class));
    }

    @Test
    void coverLetterTemplateInterfaceShouldExposeComposeAndDeprecatedRenderMethods() throws Exception {
        assertMethodPresent(CoverLetterTemplate.class, "compose", DocumentComposer.class,
                com.demcha.templates.data.Header.class, String.class, JobDetails.class);
        assertDeprecated(CoverLetterTemplate.class.getMethod("render",
                com.demcha.templates.data.Header.class, String.class, JobDetails.class));
        assertDeprecated(CoverLetterTemplate.class.getMethod("render",
                com.demcha.templates.data.Header.class, String.class, JobDetails.class, Path.class));
    }

    @Test
    void invoiceTemplateInterfaceShouldExposeComposeAndDeprecatedRenderMethods() throws Exception {
        assertMethodPresent(InvoiceTemplate.class, "compose", DocumentComposer.class, com.demcha.templates.data.InvoiceData.class);
        assertDeprecated(InvoiceTemplate.class.getMethod("render", com.demcha.templates.data.InvoiceData.class));
        assertDeprecated(InvoiceTemplate.class.getMethod("render", com.demcha.templates.data.InvoiceData.class, boolean.class));
        assertDeprecated(InvoiceTemplate.class.getMethod("render", com.demcha.templates.data.InvoiceData.class, Path.class));
        assertDeprecated(InvoiceTemplate.class.getMethod("render", com.demcha.templates.data.InvoiceData.class, Path.class, boolean.class));
    }

    @Test
    void proposalTemplateInterfaceShouldExposeComposeAndDeprecatedRenderMethods() throws Exception {
        assertMethodPresent(ProposalTemplate.class, "compose", DocumentComposer.class, com.demcha.templates.data.ProposalData.class);
        assertDeprecated(ProposalTemplate.class.getMethod("render", com.demcha.templates.data.ProposalData.class));
        assertDeprecated(ProposalTemplate.class.getMethod("render", com.demcha.templates.data.ProposalData.class, boolean.class));
        assertDeprecated(ProposalTemplate.class.getMethod("render", com.demcha.templates.data.ProposalData.class, Path.class));
        assertDeprecated(ProposalTemplate.class.getMethod("render", com.demcha.templates.data.ProposalData.class, Path.class, boolean.class));
    }

    @Test
    void weeklyScheduleTemplateInterfaceShouldExposeComposeAndDeprecatedRenderMethods() throws Exception {
        assertMethodPresent(WeeklyScheduleTemplate.class, "compose", DocumentComposer.class, com.demcha.templates.data.WeeklyScheduleData.class);
        assertDeprecated(WeeklyScheduleTemplate.class.getMethod("render", com.demcha.templates.data.WeeklyScheduleData.class));
        assertDeprecated(WeeklyScheduleTemplate.class.getMethod("render", com.demcha.templates.data.WeeklyScheduleData.class, boolean.class));
        assertDeprecated(WeeklyScheduleTemplate.class.getMethod("render", com.demcha.templates.data.WeeklyScheduleData.class, Path.class));
        assertDeprecated(WeeklyScheduleTemplate.class.getMethod("render", com.demcha.templates.data.WeeklyScheduleData.class, Path.class, boolean.class));
    }

    @Test
    void cvBuiltInsShouldComposeThroughPublicComposeContract() throws Exception {
        MainPageCV original = new MainPageCVMock().getMainPageCV();
        MainPageCvDTO rewritten = MainPageCvDTO.from(original);
        CvTemplate standardTemplate = new CvTemplateV1();
        CvTemplate editorialTemplate = new EditorialBlueCvTemplate();

        assertComposesToPdf(composer -> standardTemplate.compose(composer, original, rewritten));
        assertComposesToPdf(composer -> editorialTemplate.compose(composer, original, rewritten));
    }

    @Test
    void coverLetterBuiltInShouldComposeThroughPublicComposeContract() throws Exception {
        CoverLetterTemplate template = new CoverLetterTemplateV1();
        MainPageCV original = new MainPageCVMock().getMainPageCV();
        String letter = CoverLetterMock.letter.replace("${companyName}", "Compose Path Ltd");

        assertComposesToPdf(composer -> template.compose(composer, original.getHeader(), letter, testJobDetails()));
    }

    @Test
    void invoiceBuiltInShouldComposeThroughPublicComposeContract() throws Exception {
        InvoiceTemplate template = new InvoiceTemplateV1();
        assertComposesToPdf(composer -> template.compose(composer, InvoiceDataFixtures.standardInvoice()));
    }

    @Test
    void proposalBuiltInShouldComposeThroughPublicComposeContract() throws Exception {
        ProposalTemplate template = new ProposalTemplateV1();
        assertComposesToPdf(composer -> template.compose(composer, ProposalDataFixtures.longProposal()));
    }

    @Test
    void weeklyScheduleBuiltInShouldComposeThroughPublicComposeContract() throws Exception {
        WeeklyScheduleTemplate template = new WeeklyScheduleTemplateV1();
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()))
                .margin(18, 18, 18, 18)
                .markdown(true)
                .create()) {
            template.compose(composer, WeeklyScheduleDataFixtures.standardSchedule());
            assertPdfBytesLookValid(composer.toBytes());
        }
    }

    private void assertComposesToPdf(ComposerAction action) throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(24, 24, 24, 24)
                .markdown(true)
                .create()) {
            action.apply(composer);
            byte[] pdfBytes = composer.toBytes();
            assertPdfBytesLookValid(pdfBytes);
        }
    }

    private void assertMethodPresent(Class<?> type, String name, Class<?>... parameterTypes) throws Exception {
        Method method = type.getMethod(name, parameterTypes);
        assertThat(method).isNotNull();
        assertThat(method.isAnnotationPresent(Deprecated.class)).isFalse();
    }

    private void assertDeprecated(Method method) {
        Deprecated annotation = method.getAnnotation(Deprecated.class);
        assertThat(annotation)
                .as("Expected %s to be marked deprecated", method)
                .isNotNull();
        assertThat(annotation.forRemoval()).isFalse();
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
    private interface ComposerAction {
        void apply(DocumentComposer composer) throws Exception;
    }
}
