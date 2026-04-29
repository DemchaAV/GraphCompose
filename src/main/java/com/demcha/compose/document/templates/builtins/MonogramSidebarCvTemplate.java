package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.MonogramSidebarCvTemplateComposer;

/**
 * Two-column resume with a pale neutral sidebar holding a monogram circle,
 * centered contact icons, plus education and expertise blocks. The right
 * column carries a large two-line letter-spaced headline, professional
 * profile, and experience entries.
 */
public final class MonogramSidebarCvTemplate implements CvTemplate {
    private final MonogramSidebarCvTemplateComposer composer = new MonogramSidebarCvTemplateComposer();

    @Override
    public String getTemplateId() {
        return "monogram-sidebar";
    }

    @Override
    public String getTemplateName() {
        return "Monogram Sidebar";
    }

    @Override
    public String getDescription() {
        return "A boutique two-column resume with a monogram circle, centered contact icons, and a serif headline.";
    }

    @Override
    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        BuiltInCvTemplateSupport.composeDirect(getTemplateId(), document, documentSpec, composer::compose);
    }
}
