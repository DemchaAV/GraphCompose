package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.SidebarPortraitCvTemplateComposer;
import com.demcha.compose.document.templates.theme.CvTheme;

/**
 * Two-column resume with a portrait sidebar (circular photo, contact icons,
 * education and skills) on the left and the main hero, professional profile,
 * and experience timeline on the right.
 *
 * <p>Modernised under v1.5 — accepts an optional {@link CvTheme}.</p>
 */
public final class SidebarPortraitCvTemplate implements CvTemplate {
    private final SidebarPortraitCvTemplateComposer composer;

    public SidebarPortraitCvTemplate() {
        this.composer = new SidebarPortraitCvTemplateComposer();
    }

    public SidebarPortraitCvTemplate(CvTheme theme) {
        this.composer = new SidebarPortraitCvTemplateComposer(theme);
    }

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
