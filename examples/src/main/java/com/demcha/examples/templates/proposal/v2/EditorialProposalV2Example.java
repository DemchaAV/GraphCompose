package com.demcha.examples.templates.proposal;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.proposal.StructuredProposalDocumentSpec;
import com.demcha.compose.document.templates.proposal.presets.EditorialProposal;
import com.demcha.examples.support.EditorialProposalSampleData;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the layered {@code proposal.v2} Editorial Proposal preset against
 * the structured proposal sample data.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/proposal/proposal-editorial-v2.pdf}.</p>
 *
 * <p>The same document shape the Northline example renders, in the serif
 * editorial look: the preset owns its page geometry, so the session starts
 * unconfigured.</p>
 */
public final class EditorialProposalV2Example {

    private EditorialProposalV2Example() {
    }

    /**
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/proposal", "proposal-editorial-v2.pdf");
        StructuredProposalDocumentSpec spec = EditorialProposalSampleData.sample();
        DocumentTemplate<StructuredProposalDocumentSpec> template = EditorialProposal.create();

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
