package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.SidebarPortraitCvTemplateComposer;

/**
 * Two-column resume with a portrait sidebar (photo placeholder, contact
 * icons, education and skills) on the left and the main hero, professional
 * profile, and experience timeline on the right.
 */
public final class SidebarPortraitCvTemplate implements CvTemplate {
    private final SidebarPortraitCvTemplateComposer composer = new SidebarPortraitCvTemplateComposer();

    @Override
    public String getTemplateId() {
        return "sidebar-portrait";
    }

    @Override
    public String getTemplateName() {
        return "Sidebar Portrait";
    }

    @Override
    public String getDescription() {
        return "A two-column resume with a portrait sidebar of contact icons and education, plus a serif headline and experience timeline on the right.";
    }

    @Override
    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        BuiltInCvTemplateSupport.composeDirect(getTemplateId(), document, documentSpec, composer::compose);
    }
}
