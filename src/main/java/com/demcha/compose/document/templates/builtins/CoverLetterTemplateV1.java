package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CoverLetterTemplate;
import com.demcha.compose.document.templates.data.Header;
import com.demcha.compose.document.templates.data.JobDetails;
import com.demcha.compose.document.templates.support.LegacyTemplateMappers;
import com.demcha.compose.document.templates.support.LegacyTemplateSessionRenderer;

/**
 * Canonical V2 implementation of the standard cover-letter template.
 */
public final class CoverLetterTemplateV1 implements CoverLetterTemplate {
    private final com.demcha.templates.builtins.CoverLetterTemplateV1 legacyBridge;

    public CoverLetterTemplateV1() {
        this.legacyBridge = new com.demcha.templates.builtins.CoverLetterTemplateV1();
    }

    @Override
    public String getTemplateId() {
        return "cover-letter-v1";
    }

    @Override
    public String getTemplateName() {
        return "Cover Letter V1";
    }

    @Override
    public String getDescription() {
        return "A cover letter template with header details, contact links, and a single-column letter body.";
    }

    @Override
    public void compose(DocumentSession document, Header header, String wroteLetter, JobDetails jobDetails) {
        LegacyTemplateSessionRenderer.renderInto(document, composer -> legacyBridge.compose(
                composer,
                LegacyTemplateMappers.toLegacy(header),
                wroteLetter,
                LegacyTemplateMappers.toLegacy(jobDetails)));
    }
}
