package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CoverLetterTemplate;
import com.demcha.compose.document.templates.data.common.Header;
import com.demcha.compose.document.templates.data.coverletter.JobDetails;
import com.demcha.compose.document.templates.support.business.CoverLetterTemplateComposer;
import com.demcha.compose.document.templates.support.common.SessionTemplateComposeTarget;
import com.demcha.compose.document.templates.theme.CvTheme;

/**
 * Canonical V2 implementation of the standard cover-letter template.
 */
public final class CoverLetterTemplateV1 implements CoverLetterTemplate {
    private final CoverLetterTemplateComposer composer;

    public CoverLetterTemplateV1() {
        this.composer = new CoverLetterTemplateComposer(CvTheme.defaultTheme(), CvTheme.courier());
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
        composer.compose(new SessionTemplateComposeTarget(document), header, wroteLetter, jobDetails);
    }
}
