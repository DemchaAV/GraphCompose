package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.TechLeadCvTemplateComposer;
import com.demcha.compose.document.templates.theme.CvTheme;

/**
 * Strong technical leadership CV with compact business structure.
 */
public final class TechLeadCvTemplate implements CvTemplate {
    private final TechLeadCvTemplateComposer composer;

    public TechLeadCvTemplate() {
        this(null);
    }

    public TechLeadCvTemplate(CvTheme theme) {
        this.composer = theme == null ? new TechLeadCvTemplateComposer() : new TechLeadCvTemplateComposer(theme);
    }

    @Override
    public String getTemplateId() {
        return "tech-lead";
    }

    @Override
    public String getTemplateName() {
        return "Tech Lead";
    }

    @Override
    public String getDescription() {
        return "A confident CV for senior engineers with dark navy hierarchy, green accent rules, and compact sections.";
    }

    @Override
    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        BuiltInCvTemplateSupport.composeDirect(getTemplateId(), document, documentSpec, composer::compose);
    }
}
