package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.BoxedSectionsCvTemplateComposer;

/**
 * Two-page friendly resume in the conventional Word "boxed-sections" style:
 * each section title sits inside a soft grey banner, work entries stack as
 * bold position + italic company subtitle + body description.
 */
public final class BoxedSectionsCvTemplate implements CvTemplate {
    private final BoxedSectionsCvTemplateComposer composer = new BoxedSectionsCvTemplateComposer();

    @Override
    public String getTemplateId() {
        return "boxed-sections";
    }

    @Override
    public String getTemplateName() {
        return "Boxed Sections";
    }

    @Override
    public String getDescription() {
        return "A classic Word-resume layout with grey section banners, centered headline, and bold work entries.";
    }

    @Override
    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        BuiltInCvTemplateSupport.composeDirect(getTemplateId(), document, documentSpec, composer::compose);
    }
}
