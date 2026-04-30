package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.ClassicSerifCvTemplateComposer;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.font.FontName;

import java.awt.Color;
import java.util.Objects;

/**
 * Professional serif CV for more formal applications and consulting profiles.
 */
public final class ClassicSerifCvTemplate implements CvTemplate {
    private final ClassicSerifCvTemplateComposer composer;

    public ClassicSerifCvTemplate() {
        this(null);
    }

    public ClassicSerifCvTemplate(CvTheme theme) {
        CvTheme resolvedTheme = Objects.requireNonNullElseGet(theme, ClassicSerifCvTemplate::defaultTheme);
        this.composer = new ClassicSerifCvTemplateComposer(resolvedTheme);
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
        BuiltInCvTemplateSupport.composeDirect(getTemplateId(), document, documentSpec, composer::compose);
    }

    private static CvTheme defaultTheme() {
        return new CvTheme(
                new Color(30, 38, 54),
                new Color(126, 93, 52),
                new Color(45, 43, 40),
                new Color(126, 93, 52),
                FontName.PT_SERIF,
                FontName.PT_SERIF,
                27,
                9.2,
                9.0,
                2.2,
                Margin.top(3),
                0);
    }
}
