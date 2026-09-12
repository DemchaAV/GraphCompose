package com.demcha.compose.document.templates.data.proposal;

/**
 * Public compose-first input for structured proposal templates.
 *
 * <p><b>Authoring role:</b> the document-level object structured proposal
 * presets are parameterised on, the way narrative presets are parameterised
 * on {@link ProposalDocumentSpec}. Presets that render the structured shape
 * — brand marks, fact card, goal cells, phase grid, priced block, signing
 * card — consume this spec; presets that render prose sections stay on the
 * narrative one.</p>
 *
 * @param proposal normalized structured proposal content
 */
public record StructuredProposalDocumentSpec(StructuredProposalData proposal) {

    /**
     * Creates a normalized structured proposal document spec.
     */
    public StructuredProposalDocumentSpec {
        proposal = proposal == null ? StructuredProposalData.builder().build() : proposal;
    }

    /**
     * Wraps existing structured proposal data in the document-level spec
     * expected by structured proposal templates.
     *
     * @param proposal structured proposal data
     * @return document spec
     */
    public static StructuredProposalDocumentSpec from(StructuredProposalData proposal) {
        return new StructuredProposalDocumentSpec(proposal);
    }
}
