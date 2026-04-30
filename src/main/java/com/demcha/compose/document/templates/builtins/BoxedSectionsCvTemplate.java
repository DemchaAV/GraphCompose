package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.BoxedSectionsCvTemplateComposer;
import com.demcha.compose.document.templates.theme.CvTheme;

/**
 * Two-page friendly resume in the conventional Word "boxed-sections" style:
 * each section title sits inside a soft grey banner, work entries stack as
 * bold position + italic company subtitle + body description.
 *
 * <p>Modernised under v1.5: optionally accepts a {@link CvTheme} so the
 * body type and contact line can be re-skinned via
 * {@code CvTheme.fromBusinessTheme(BusinessTheme)} while the soft-grey
 * banner identity stays template-owned.</p>
 */
public final class BoxedSectionsCvTemplate implements CvTemplate {
    private final BoxedSectionsCvTemplateComposer composer;

    public BoxedSectionsCvTemplate() {
        this.composer = new BoxedSectionsCvTemplateComposer();
    }

    public BoxedSectionsCvTemplate(CvTheme theme) {
        this.composer = new BoxedSectionsCvTemplateComposer(theme);
    }

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
