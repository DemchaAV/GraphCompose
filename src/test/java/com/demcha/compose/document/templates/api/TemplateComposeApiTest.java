package com.demcha.compose.document.templates.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import com.demcha.compose.document.templates.builtins.CoverLetterTemplateV1;
import com.demcha.compose.document.templates.builtins.CvTemplateV1;
import com.demcha.compose.document.templates.builtins.EditorialBlueCvTemplate;
import com.demcha.compose.document.templates.builtins.ExecutiveSlateCvTemplate;
import com.demcha.compose.document.templates.builtins.InvoiceTemplateV1;
import com.demcha.compose.document.templates.builtins.ProposalTemplateV1;
import com.demcha.compose.document.templates.builtins.WeeklyScheduleTemplateV1;
import com.demcha.compose.document.templates.data.coverletter.CoverLetterDocumentSpec;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;
import com.demcha.compose.document.templates.data.schedule.WeeklyScheduleDocumentSpec;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateComposeApiTest {

    @Test
    void graphComposeShouldExposeCanonicalDocumentEntrypointOnly() {
        List<String> methodNames = List.of(GraphCompose.class.getDeclaredMethods()).stream()
                .map(Method::getName)
                .toList();

        assertThat(methodNames).contains("document");
        assertThat(methodNames).doesNotContain("pdf");
    }

    @Test
    void cvTemplateInterfaceShouldExposeDocumentSessionComposeContract() throws Exception {
        assertMethodPresent(CvTemplate.class, "compose", DocumentSession.class, CvDocumentSpec.class);
    }

    @Test
    void coverLetterTemplateInterfaceShouldExposeDocumentSessionComposeContract() throws Exception {
        assertMethodPresent(CoverLetterTemplate.class, "compose", DocumentSession.class, CoverLetterDocumentSpec.class);
    }

    @Test
    void invoiceTemplateInterfaceShouldExposeDocumentSessionComposeContract() throws Exception {
        assertMethodPresent(InvoiceTemplate.class, "compose", DocumentSession.class, InvoiceDocumentSpec.class);
    }

    @Test
    void proposalTemplateInterfaceShouldExposeDocumentSessionComposeContract() throws Exception {
        assertMethodPresent(ProposalTemplate.class, "compose", DocumentSession.class, ProposalDocumentSpec.class);
    }

    @Test
    void weeklyScheduleTemplateInterfaceShouldExposeDocumentSessionComposeContract() throws Exception {
        assertMethodPresent(WeeklyScheduleTemplate.class, "compose", DocumentSession.class, WeeklyScheduleDocumentSpec.class);
    }

    @Test
    void cvTemplateRegistryShouldResolveCanonicalBuiltIns() {
        CvTemplate standard = new CvTemplateV1();
        CvTemplate editorial = new EditorialBlueCvTemplate();
        CvTemplate executive = new ExecutiveSlateCvTemplate();
        CvTemplateRegistry registry = new CvTemplateRegistry(List.of(standard, editorial, executive));

        assertThat(registry.getDefaultTemplateId()).isEqualTo("modern-professional");
        assertThat(registry.hasTemplate("modern-professional")).isTrue();
        assertThat(registry.hasTemplate("editorial-blue")).isTrue();
        assertThat(registry.hasTemplate("executive-slate")).isTrue();
        assertThat(registry.getTemplate("modern-professional")).isSameAs(standard);
        assertThat(registry.getTemplateOrDefault("missing", "editorial-blue")).isSameAs(editorial);
        assertThat(registry.getTemplate("executive-slate")).isSameAs(executive);
        assertThat(registry.getAllTemplates()).containsExactlyInAnyOrder(standard, editorial, executive);
    }

    @Test
    void cvBuiltInsShouldComposeThroughDocumentSession() throws Exception {
        CvDocumentSpec composeFirst = TemplateTestSupport.canonicalCv();

        assertComposesToPdf(PDRectangle.A4, 24, document -> new CvTemplateV1().compose(document, composeFirst));
        assertComposesToPdf(PDRectangle.A4, 18, document -> new EditorialBlueCvTemplate().compose(document, composeFirst));
        assertComposesToPdf(PDRectangle.A4, 20, document -> new ExecutiveSlateCvTemplate().compose(document, composeFirst));
    }

    @Test
    void cvBuiltInsShouldExposeComposeFirstCvDocumentSpecContract() throws Exception {
        assertMethodPresent(CvTemplateV1.class, "compose", DocumentSession.class, CvDocumentSpec.class);
        assertMethodPresent(EditorialBlueCvTemplate.class, "compose", DocumentSession.class, CvDocumentSpec.class);
        assertMethodPresent(ExecutiveSlateCvTemplate.class, "compose", DocumentSession.class, CvDocumentSpec.class);
    }

    @Test
    void coverLetterBuiltInShouldComposeThroughDocumentSession() throws Exception {
        assertComposesToPdf(PDRectangle.A4, 15, 10, 15, 15,
                document -> new CoverLetterTemplateV1().compose(
                        document,
                        TemplateTestSupport.canonicalCoverLetter("Compose Path Ltd")));
    }

    @Test
    void invoiceBuiltInShouldComposeThroughDocumentSession() throws Exception {
        assertComposesToPdf(PDRectangle.A4, 22,
                document -> new InvoiceTemplateV1().compose(document, TemplateTestSupport.canonicalInvoice()));
    }

    @Test
    void proposalBuiltInShouldComposeThroughDocumentSession() throws Exception {
        assertComposesToPdf(PDRectangle.A4, 22,
                document -> new ProposalTemplateV1().compose(document, TemplateTestSupport.canonicalProposal()));
    }

    @Test
    void weeklyScheduleBuiltInShouldComposeThroughDocumentSession() throws Exception {
        PDRectangle landscapeA4 = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());

        assertComposesToPdf(landscapeA4, 18,
                document -> new WeeklyScheduleTemplateV1().compose(document, TemplateTestSupport.canonicalWeeklySchedule()));
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
                .pageSize(DocumentPageSize.of(pageSize.getWidth(), pageSize.getHeight()))
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

    @FunctionalInterface
    private interface SessionAction {
        void apply(DocumentSession document) throws Exception;
    }
}
