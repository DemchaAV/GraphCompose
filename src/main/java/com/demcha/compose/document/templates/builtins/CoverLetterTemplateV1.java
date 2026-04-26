package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CoverLetterTemplate;
import com.demcha.compose.document.templates.data.coverletter.CoverLetterDocumentSpec;
import com.demcha.compose.document.templates.support.business.CoverLetterTemplateComposer;
import com.demcha.compose.document.templates.support.common.SessionTemplateComposeTarget;
import com.demcha.compose.document.templates.support.common.TemplateLifecycleLog;
import com.demcha.compose.document.templates.theme.CvTheme;

/**
 * Canonical implementation of the standard cover-letter template.
 */
public final class CoverLetterTemplateV1 implements CoverLetterTemplate {
    private final CoverLetterTemplateComposer composer;

    /**
     * Creates the default cover-letter template.
     */
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
    public void compose(DocumentSession document, CoverLetterDocumentSpec spec) {
        long startNanos = TemplateLifecycleLog.start(getTemplateId(), spec);
        try {
            composer.compose(new SessionTemplateComposeTarget(document), spec);
            TemplateLifecycleLog.success(getTemplateId(), spec, startNanos);
        } catch (RuntimeException | Error ex) {
            TemplateLifecycleLog.failure(getTemplateId(), spec, startNanos, ex);
            throw ex;
        }
    }
}
