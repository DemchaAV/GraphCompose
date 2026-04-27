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
 * Strong technical leadership CV with compact business structure.
 */
public final class TechLeadCvTemplate implements CvTemplate {
    private final ExecutiveSlateCvTemplateComposer composer;

    public TechLeadCvTemplate() {
        this(null);
    }

    public TechLeadCvTemplate(CvTheme theme) {
        this.composer = new ExecutiveSlateCvTemplateComposer(
                Objects.requireNonNullElseGet(theme, TechLeadCvTemplate::defaultTheme));
    }

    @Override
    public String getTemplateId() {
        return "tech-lead";
    }

    @Override
    public String getTemplateName() {
        return "Tech Lead";
    }

    @Override
    public String getDescription() {
        return "A confident CV for senior engineers with dark navy hierarchy, green accent rules, and compact sections.";
    }

    @Override
    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        BuiltInCvTemplateSupport.compose(getTemplateId(), document, documentSpec, composer::compose);
    }

    private static CvTheme defaultTheme() {
        return new CvTheme(
                new Color(13, 36, 53),
                new Color(13, 36, 53),
                new Color(42, 55, 70),
                new Color(31, 127, 93),
                FontName.BARLOW,
                FontName.LATO,
                25,
                10.8,
                9.5,
                2.3,
                Margin.top(5),
                0);
    }
}
