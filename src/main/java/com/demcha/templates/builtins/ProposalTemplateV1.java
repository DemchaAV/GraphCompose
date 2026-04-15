package com.demcha.templates.builtins;

import com.demcha.compose.document.templates.support.BusinessDocumentSceneStyles;
import com.demcha.compose.document.templates.support.LegacyComposerTemplateComposeTarget;
import com.demcha.compose.document.templates.support.LegacyTemplateMappers;
import com.demcha.compose.document.templates.support.ProposalTemplateComposer;
import com.demcha.compose.document.templates.support.SessionTemplateComposeTarget;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.templates.api.ProposalTemplate;
import com.demcha.templates.data.ProposalData;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;

/**
 * Deprecated bridge to the canonical proposal template.
 */
@Deprecated(forRemoval = false)
public class ProposalTemplateV1 extends PdfTemplateAdapterSupport implements ProposalTemplate {
    private final ProposalTemplateComposer composer = new ProposalTemplateComposer(new BusinessDocumentSceneStyles());

    @Override
    public void compose(DocumentComposer composer, ProposalData data) {
        this.composer.compose(
                new LegacyComposerTemplateComposeTarget(composer),
                LegacyTemplateMappers.toCanonical(data));
    }

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

    @Deprecated(forRemoval = false)
    @Override
    public PDDocument render(ProposalData data) {
        return render(data, false);
    }

    @Deprecated(forRemoval = false)
    @Override
    public PDDocument render(ProposalData data, boolean guideLines) {
        return renderToDocumentSession(
                guideLines,
                "Failed to generate proposal",
                PDRectangle.A4,
                22,
                22,
                22,
                22,
                session -> composer.compose(
                        new SessionTemplateComposeTarget(session),
                        LegacyTemplateMappers.toCanonical(data)));
    }

    @Deprecated(forRemoval = false)
    @Override
    public void render(ProposalData data, Path path) {
        render(data, path, false);
    }

    @Deprecated(forRemoval = false)
    @Override
    public void render(ProposalData data, Path path, boolean guideLines) {
        renderToFileSession(
                path,
                guideLines,
                "Failed to generate proposal",
                "Proposal saved to {}",
                PDRectangle.A4,
                22,
                22,
                22,
                22,
                session -> composer.compose(
                        new SessionTemplateComposeTarget(session),
                        LegacyTemplateMappers.toCanonical(data)));
    }
}
