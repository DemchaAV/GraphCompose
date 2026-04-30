package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.CenteredHeadlineCvTemplateComposer;
import com.demcha.compose.document.templates.theme.CvTheme;

/**
 * Single-column "headline" resume in the classic Word-resume tradition:
 * centered name and title, separator rules, pipe-delimited contact line, and
 * uppercase section headers above bold work entries with bulleted detail.
 *
 * <p>Modernised under v1.5 — accepts an optional {@link CvTheme}.</p>
 */
public final class CenteredHeadlineCvTemplate implements CvTemplate {
    private final CenteredHeadlineCvTemplateComposer composer;

    public CenteredHeadlineCvTemplate() {
        this.composer = new CenteredHeadlineCvTemplateComposer();
    }

    public CenteredHeadlineCvTemplate(CvTheme theme) {
        this.composer = new CenteredHeadlineCvTemplateComposer(theme);
    }

    @Override
    public String getTemplateId() {
        return "centered-headline";
    }

    @Override
    public String getTemplateName() {
        return "Centered Headline";
    }

    @Override
    public String getDescription() {
        return "A centered single-column resume with a letter-spaced headline, divider rules, and bold work entries.";
    }

    @Override
    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        BuiltInCvTemplateSupport.composeDirect(getTemplateId(), document, documentSpec, composer::compose);
    }
}
