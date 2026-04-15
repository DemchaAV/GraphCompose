package com.demcha.compose.document.templates.api;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.data.ProposalData;

/**
 * Canonical compose contract for reusable proposal templates.
 */
public interface ProposalTemplate {

    String getTemplateId();

    String getTemplateName();

    default String getDescription() {
        return "";
    }

    /**
     * Composes a proposal into a live document session.
     *
     * @param document active mutable document session receiving template nodes
     * @param data proposal document data
     */
    void compose(DocumentSession document, ProposalData data);
}
