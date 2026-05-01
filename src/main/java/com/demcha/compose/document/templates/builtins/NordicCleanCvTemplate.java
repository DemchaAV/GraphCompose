package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.NordicCleanCvTemplateComposer;
import com.demcha.compose.document.templates.theme.CvTheme;

/**
 * Calm modern CV with restrained teal accents and generous whitespace.
 */
public final class NordicCleanCvTemplate implements CvTemplate {
    private final NordicCleanCvTemplateComposer composer;

    public NordicCleanCvTemplate() {
        this(null);
    }

    public NordicCleanCvTemplate(CvTheme theme) {
        this.composer = theme == null ? new NordicCleanCvTemplateComposer() : new NordicCleanCvTemplateComposer(theme);
    }

    @Override
    public String getTemplateId() {
        return "nordic-clean";
    }

    @Override
    public String getTemplateName() {
        return "Nordic Clean";
    }

    @Override
    public String getDescription() {
        return "A quiet modern CV with teal accents, clean spacing, and highly readable professional sections.";
    }

    @Override
    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        BuiltInCvTemplateSupport.composeDirect(getTemplateId(), document, documentSpec, composer::compose);
    }
}
