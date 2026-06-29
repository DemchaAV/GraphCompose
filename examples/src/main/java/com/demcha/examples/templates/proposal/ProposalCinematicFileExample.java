package com.demcha.examples.templates.proposal;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;
import com.demcha.compose.document.templates.proposal.v2.presets.ModernProposal;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for the cinematic proposal look on the layered
 * {@code proposal.v2} surface — {@link ModernProposal} on
 * {@link BrandTheme#proposalModern()}, rendering the shared
 * {@link ProposalDocumentSpec} sample on the cream page background.
 *
 * @author Artem Demchyshyn
 */
public final class ProposalCinematicFileExample {

    private ProposalCinematicFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("templates/proposal", "proposal-cinematic.pdf");
        BrandTheme theme = BrandTheme.proposalModern();
        DocumentTemplate<ProposalDocumentSpec> template = ModernProposal.create(theme);

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(theme.palette().mainFill())
                .margin(28, 28, 28, 28)
                .create()) {
            template.compose(document, ExampleDataFactory.sampleProposal());
            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
