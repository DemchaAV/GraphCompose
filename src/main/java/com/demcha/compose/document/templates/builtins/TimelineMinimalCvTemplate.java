package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.TimelineMinimalCvTemplateComposer;

/**
 * Minimal two-column CV with a central timeline rule.
 */
public final class TimelineMinimalCvTemplate implements CvTemplate {
    private final TimelineMinimalCvTemplateComposer composer = new TimelineMinimalCvTemplateComposer();

    @Override
    public String getTemplateId() {
        return "timeline-minimal";
    }

    @Override
    public String getTemplateName() {
        return "Timeline Minimal";
    }

    @Override
    public String getDescription() {
        return "A clean grayscale CV with a left sidebar, central timeline markers, and compact professional sections.";
    }

    @Override
    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        BuiltInCvTemplateSupport.composeDirect(getTemplateId(), document, documentSpec, composer::compose);
    }
}
