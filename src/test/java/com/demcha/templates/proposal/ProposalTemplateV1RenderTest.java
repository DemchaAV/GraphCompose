package com.demcha.templates.proposal;

import com.demcha.mock.ProposalDataFixtures;
import com.demcha.templates.builtins.ProposalTemplateV1;
import com.demcha.templates.data.ProposalData;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProposalTemplateV1RenderTest {

    private final ProposalData data = ProposalDataFixtures.longProposal();
    private final ProposalTemplateV1 template = new ProposalTemplateV1();

    @Test
    void shouldExposeTemplateMetadata() {
        assertThat(template.getTemplateId()).isEqualTo("proposal-v1");
        assertThat(template.getTemplateName()).isEqualTo("Proposal V1");
        assertThat(template.getDescription()).isNotBlank();
    }

    @Test
    void shouldRenderProposalAsDocument() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("proposal_render_document", "clean", "templates", "proposal");

        try (PDDocument document = template.render(data)) {
            document.save(outputFile.toFile());
        }

        assertPdfLooksValid(outputFile, 2);
    }

    @Test
    void shouldRenderProposalDirectlyToFile() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("proposal_render_file", "clean", "templates", "proposal");

        template.render(data, outputFile);

        assertPdfLooksValid(outputFile, 2);
    }

    @Test
    void shouldRenderProposalDirectlyToFileWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("proposal_render_file_with_guide_lines", "guides", "templates", "proposal");

        template.render(data, outputFile, true);

        assertPdfLooksValid(outputFile, 2);
    }

    private void assertPdfLooksValid(Path outputFile, int minPages) throws Exception {
        assertThat(outputFile).exists();
        assertThat(outputFile).isRegularFile();
        assertThat(outputFile).isNotEmptyFile();

        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isGreaterThanOrEqualTo(minPages);
        }
    }
}
