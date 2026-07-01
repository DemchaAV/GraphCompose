package com.demcha.examples.templates.proposal;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;
import com.demcha.compose.document.templates.proposal.presets.ModernProposal;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the layered {@code proposal.v2} Modern Proposal preset against
 * the shared {@code ProposalDocumentSpec} sample data using the default
 * {@code BrandTheme.proposalModern()} theme on the cream page background.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/proposal/proposal-modern-v2.pdf}.</p>
 *
 * <p>The "hello world" for the v2 proposal pipeline: fetch sample data,
 * ask the preset for a template, render it — the same shape as the v2 CV
 * and invoice examples, now on the proposal family.</p>
 */
public final class ModernProposalV2Example {

    private ModernProposalV2Example() {
    }

    /**
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/proposal", "proposal-modern-v2.pdf");
        ProposalDocumentSpec spec = ExampleDataFactory.sampleProposal();
        BrandTheme theme = BrandTheme.proposalModern();
        DocumentTemplate<ProposalDocumentSpec> template = ModernProposal.create(theme);

        float m = (float) ModernProposal.RECOMMENDED_MARGIN;
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(theme.palette().mainFill())
                .margin(m, m, m, m)
                .create()) {
            template.compose(document, spec);
            document.buildPdf();
        }
        return outputFile;
    }

    /**
     * @param args ignored
     * @throws Exception if rendering fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
