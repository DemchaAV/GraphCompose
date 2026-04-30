package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.TimelineMinimalCvTemplateComposer;
import com.demcha.compose.document.templates.theme.CvTheme;

/**
 * Minimal two-column CV with a central timeline rule.
 *
 * <p>Modernised under v1.5 — accepts an optional {@link CvTheme}.</p>
 */
public final class TimelineMinimalCvTemplate implements CvTemplate {
    private final TimelineMinimalCvTemplateComposer composer;

    public TimelineMinimalCvTemplate() {
        this.composer = new TimelineMinimalCvTemplateComposer();
    }

    public TimelineMinimalCvTemplate(CvTheme theme) {
        this.composer = new TimelineMinimalCvTemplateComposer(theme);
    }

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
