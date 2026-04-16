package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.ProposalTemplate;
import com.demcha.compose.document.templates.data.ProposalData;
import com.demcha.compose.document.templates.support.LegacyTemplateMappers;
import com.demcha.compose.document.templates.support.LegacyTemplateSessionRenderer;

/**
 * Canonical V2 implementation of the proposal template.
 */
public final class ProposalTemplateV1 implements ProposalTemplate {
    private final com.demcha.templates.builtins.ProposalTemplateV1 legacyBridge = new com.demcha.templates.builtins.ProposalTemplateV1();

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
        LegacyTemplateSessionRenderer.renderInto(document, composer -> legacyBridge.compose(
                composer,
                LegacyTemplateMappers.toLegacy(data)));
    }
}
