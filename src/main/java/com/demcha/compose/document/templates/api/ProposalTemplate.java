package com.demcha.compose.document.templates.api;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;

/**
 * Canonical compose contract for reusable proposal templates.
 *
 * <p><b>Responsibility:</b> define one reusable proposal scene that emits
 * semantic proposal structure into a caller-owned
 * {@link DocumentSession}.</p>
 *
 * <pre>{@code
 * ProposalTemplate template = new ProposalTemplateV1();
 * ProposalDocumentSpec proposal = ProposalDocumentSpec.builder()
 *         .projectTitle("GraphCompose rollout")
 *         .section("Scope", "Introduce reusable invoice and proposal templates.")
 *         .timelineItem("Week 1", "5 days", "Foundation and review loop")
 *         .pricingRow("Delivery", "Template implementation", "GBP 4,450")
 *         .emphasizedPricingRow("Total", "Fixed-price delivery", "GBP 4,450")
 *         .build();
 *
 * try (DocumentSession document = GraphCompose.document(Path.of("proposal.pdf")).create()) {
 *     template.compose(document, proposal);
 *     document.buildPdf();
 * }
 * }</pre>
 */
public interface ProposalTemplate {

    /**
     * Stable public template identifier.
     *
     * @return unique template id used by registries and integrations
     */
    String getTemplateId();

    /**
     * Human-readable display name.
     *
     * @return template display name
     */
    String getTemplateName();

    /**
     * Optional human-readable description.
     *
     * @return template description, or an empty string when omitted
     */
    default String getDescription() {
        return "";
    }

    /**
     * Composes a proposal into a live document session.
     *
     * @param document active mutable document session receiving template nodes
     * @param spec proposal document spec
     * @throws NullPointerException if an implementation requires non-null inputs
     */
    void compose(DocumentSession document, ProposalDocumentSpec spec);
}
