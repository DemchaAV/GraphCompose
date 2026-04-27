package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.ExecutiveSlateCvTemplateComposer;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.font.FontName;

import java.awt.Color;
import java.util.Objects;

/**
 * Professional serif CV for more formal applications and consulting profiles.
 */
public final class ClassicSerifCvTemplate implements CvTemplate {
    private final ExecutiveSlateCvTemplateComposer composer;

    public ClassicSerifCvTemplate() {
        this(null);
    }

    public ClassicSerifCvTemplate(CvTheme theme) {
        this.composer = new ExecutiveSlateCvTemplateComposer(
                Objects.requireNonNullElseGet(theme, ClassicSerifCvTemplate::defaultTheme));
    }

    @Override
    public String getTemplateId() {
        return "classic-serif";
    }

    @Override
    public String getTemplateName() {
        return "Classic Serif";
    }

    @Override
    public String getDescription() {
        return "A formal professional CV with serif typography, strong hierarchy, and subtle navy accents.";
    }

    @Override
    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        BuiltInCvTemplateSupport.compose(getTemplateId(), document, documentSpec, composer::compose);
    }

    private static CvTheme defaultTheme() {
        return new CvTheme(
                new Color(30, 38, 54),
                new Color(30, 38, 54),
                new Color(60, 59, 57),
                new Color(54, 86, 133),
                FontName.PT_SERIF,
                FontName.PT_SERIF,
                24,
                10.7,
                9.5,
                2.2,
                Margin.top(5),
                0);
    }
}
