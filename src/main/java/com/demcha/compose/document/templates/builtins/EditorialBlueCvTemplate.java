package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.font.FontName;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.EditorialBlueCvTemplateComposer;
import com.demcha.compose.document.templates.support.common.SessionTemplateComposeTarget;
import com.demcha.compose.document.templates.support.common.TemplateLifecycleLog;
import com.demcha.compose.document.templates.theme.CvTheme;

import java.awt.Color;
import java.util.Objects;

/**
 * Canonical implementation of the editorial blue CV template.
 */
public final class EditorialBlueCvTemplate implements CvTemplate {
    private final EditorialBlueCvTemplateComposer composer;

    /**
     * Creates the default editorial blue CV template.
     */
    public EditorialBlueCvTemplate() {
        this(null);
    }

    /**
     * Creates the editorial blue CV template with a custom theme.
     *
     * @param theme theme override, or {@code null} for the default
     */
    public EditorialBlueCvTemplate(CvTheme theme) {
        this.composer = new EditorialBlueCvTemplateComposer(
                Objects.requireNonNullElseGet(theme, EditorialBlueCvTemplate::defaultTheme));
    }

    @Override
    public String getTemplateId() {
        return "editorial-blue";
    }

    @Override
    public String getTemplateName() {
        return "Editorial Blue";
    }

    @Override
    public String getDescription() {
        return "A light editorial CV with a centered header, blue section rules, and a structured skills table.";
    }

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

    private static CvTheme defaultTheme() {
        return new CvTheme(
                new Color(18, 31, 72),
                new Color(86, 136, 255),
                new Color(60, 72, 106),
                new Color(86, 136, 255),
                FontName.HELVETICA,
                FontName.HELVETICA,
                22,
                10.6,
                9.6,
                3,
                Margin.top(3),
                0);
    }
}
