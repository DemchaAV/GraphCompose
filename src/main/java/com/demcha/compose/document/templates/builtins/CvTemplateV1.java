package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.CvTemplateComposer;
import com.demcha.compose.document.templates.support.common.SessionTemplateComposeTarget;
import com.demcha.compose.document.templates.support.common.TemplateLifecycleLog;
import com.demcha.compose.document.templates.theme.CvTheme;

import java.util.Objects;

/**
 * Canonical V2 implementation of the standard CV template.
 */
public final class CvTemplateV1 implements CvTemplate {
    private final CvTemplateComposer composer;

    /**
     * Creates the default standard CV template.
     */
    public CvTemplateV1() {
        this(CvTheme.defaultTheme());
    }

    /**
     * Creates the standard CV template with a custom visual theme.
     *
     * @param theme theme override, or {@code null} for the default
     */
    public CvTemplateV1(CvTheme theme) {
        this.composer = new CvTemplateComposer(Objects.requireNonNullElseGet(theme, CvTheme::defaultTheme));
    }

    @Override
    public String getTemplateId() {
        return "modern-professional";
    }

    @Override
    public String getTemplateName() {
        return "Modern Professional";
    }

    @Override
    public String getDescription() {
        return "A clean, professional template with centered header and well-organized sections.";
    }

    /**
     * Composes the standard CV template from the ordered compose-first module
     * model.
     *
     * @param document active semantic document session
     * @param documentSpec header plus ordered content modules
     */
    @Override
    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        long startNanos = TemplateLifecycleLog.start(getTemplateId(), documentSpec);
        try {
            composer.compose(new SessionTemplateComposeTarget(document), documentSpec);
            TemplateLifecycleLog.success(getTemplateId(), documentSpec, startNanos);
        } catch (RuntimeException | Error ex) {
            TemplateLifecycleLog.failure(getTemplateId(), documentSpec, startNanos, ex);
            throw ex;
        }
    }
}
