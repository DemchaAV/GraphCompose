package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.ProposalTemplate;
import com.demcha.compose.document.templates.data.proposal.ProposalData;
import com.demcha.compose.document.templates.support.business.BusinessDocumentSceneStyles;
import com.demcha.compose.document.templates.support.business.ProposalTemplateComposer;
import com.demcha.compose.document.templates.support.common.SessionTemplateComposeTarget;

/**
 * Canonical V2 implementation of the proposal template.
 */
public final class ProposalTemplateV1 implements ProposalTemplate {
    private final ProposalTemplateComposer composer = new ProposalTemplateComposer(new BusinessDocumentSceneStyles());

    @Override
    public String getTemplateId() {
        return "proposal-v1";
    }

    @Override
    public String getTemplateName() {
        return "Proposal V1";
    }

    @Override
    public String getDescription() {
        return "A light business proposal template with executive summary, scope, timeline, pricing, and acceptance terms.";
    }

    @Override
    public void compose(DocumentSession document, ProposalData data) {
        composer.compose(new SessionTemplateComposeTarget(document), data);
    }
}
