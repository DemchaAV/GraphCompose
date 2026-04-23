package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.EditorialBlueCvTemplateComposer;
import com.demcha.compose.document.templates.support.common.SessionTemplateComposeTarget;
import com.demcha.compose.document.templates.theme.CvTheme;

import java.awt.Color;
import java.util.Objects;

/**
 * Canonical V2 implementation of the editorial blue CV template.
 */
public final class EditorialBlueCvTemplate implements CvTemplate {
    private final EditorialBlueCvTemplateComposer composer;

    public EditorialBlueCvTemplate() {
        this(null);
    }

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
        composer.compose(new SessionTemplateComposeTarget(document), documentSpec);
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
