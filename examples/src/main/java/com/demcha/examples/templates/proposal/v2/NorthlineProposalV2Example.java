package com.demcha.examples.templates.proposal;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.proposal.StructuredProposalDocumentSpec;
import com.demcha.compose.document.templates.proposal.presets.NorthlineProposal;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.examples.support.StructuredProposalSampleData;

import java.nio.file.Path;

/**
 * Renders the layered {@code proposal.v2} Northline Proposal preset
 * against the shared structured proposal sample data.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/proposal/proposal-northline-v2.pdf}.</p>
 *
 * <p>The preset owns its page geometry — size, margins, the footer band
 * and the page background — so the session starts unconfigured; see
 * {@code NorthlineProposal.RECOMMENDED_MARGIN}.</p>
 */
public final class NorthlineProposalV2Example {

    private NorthlineProposalV2Example() {
    }

    /**
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/proposal", "proposal-northline-v2.pdf");
        StructuredProposalDocumentSpec spec = StructuredProposalSampleData.sample();
        DocumentTemplate<StructuredProposalDocumentSpec> template = NorthlineProposal.create();

        try (DocumentSession document = GraphCompose.document(outputFile).create()) {
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
