package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.CvTemplateComposer;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.font.FontName;

import java.awt.Color;
import java.util.Objects;

/**
 * Calm modern CV with restrained teal accents and generous whitespace.
 */
public final class NordicCleanCvTemplate implements CvTemplate {
    private final CvTemplateComposer composer;

    public NordicCleanCvTemplate() {
        this(null);
    }

    public NordicCleanCvTemplate(CvTheme theme) {
        this.composer = new CvTemplateComposer(Objects.requireNonNullElseGet(theme, NordicCleanCvTemplate::defaultTheme));
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
        BuiltInCvTemplateSupport.compose(getTemplateId(), document, documentSpec, composer::compose);
    }

    private static CvTheme defaultTheme() {
        return new CvTheme(
                new Color(17, 43, 60),
                new Color(34, 128, 141),
                new Color(48, 64, 78),
                new Color(34, 128, 141),
                FontName.LATO,
                FontName.LATO,
                27,
                14.2,
                9.6,
                4,
                Margin.top(5),
                0);
    }
}
