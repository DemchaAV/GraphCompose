package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.ExecutiveSlateCvTemplateComposer;
import com.demcha.compose.document.templates.support.common.SessionTemplateComposeTarget;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.components.style.Margin;

import java.awt.Color;
import java.util.Objects;

/**
 * Canonical V2 implementation of the executive slate CV template.
 *
 * <p>This built-in is meant for business-oriented CVs: compact header,
 * restrained slate typography, warm accent headings, and the same universal
 * module model as {@link CvTemplateV1}.</p>
 *
 * @author Artem Demchyshyn
 */
public final class ExecutiveSlateCvTemplate implements CvTemplate {
    private final ExecutiveSlateCvTemplateComposer composer;

    /**
     * Creates the template with its bundled executive slate theme.
     */
    public ExecutiveSlateCvTemplate() {
        this(null);
    }

    /**
     * Creates the template with a custom theme.
     *
     * @param theme visual theme, or {@code null} for the bundled theme
     */
    public ExecutiveSlateCvTemplate(CvTheme theme) {
        this.composer = new ExecutiveSlateCvTemplateComposer(
                Objects.requireNonNullElseGet(theme, ExecutiveSlateCvTemplate::defaultTheme));
    }

    @Override
    public String getTemplateId() {
        return "executive-slate";
    }

    @Override
    public String getTemplateName() {
        return "Executive Slate";
    }

    @Override
    public String getDescription() {
        return "A polished business CV with a compact contact row, slate typography, and warm executive accents.";
    }

    /**
     * Composes the executive slate CV from the compose-first module model.
     *
     * @param document active semantic document session
     * @param documentSpec header plus ordered content modules
     */
    @Override
    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        composer.compose(new SessionTemplateComposeTarget(document), documentSpec);
    }

    private static CvTheme defaultTheme() {
        return new CvTheme(
                new Color(24, 35, 51),
                new Color(24, 35, 51),
                new Color(49, 58, 72),
                new Color(172, 112, 55),
                FontName.POPPINS,
                FontName.LATO,
                24.0,
                10.8,
                9.5,
                2.0,
                Margin.top(6),
                0);
    }
}
