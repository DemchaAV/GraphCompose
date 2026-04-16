package com.demcha.templates.builtins;

import com.demcha.compose.document.templates.support.EditorialBlueCvTemplateComposer;
import com.demcha.compose.document.templates.support.LegacyTemplateMappers;
import com.demcha.compose.document.templates.support.SessionTemplateComposeTarget;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.templates.CvTheme;
import com.demcha.templates.api.CvTemplate;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.data.MainPageCV;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Deprecated bridge to the canonical editorial-blue CV template.
 */
@Deprecated(forRemoval = false)
public class EditorialBlueCvTemplate extends PdfTemplateAdapterSupport implements CvTemplate {
    private final EditorialBlueCvTemplateComposer composer;
    private final EditorialBlueCvSceneBuilder legacySceneBuilder;

    public EditorialBlueCvTemplate() {
        this(null);
    }

    public EditorialBlueCvTemplate(CvTheme theme) {
        CvTheme resolvedTheme = Objects.requireNonNullElseGet(theme, EditorialBlueCvTemplate::defaultTheme);
        this.composer = new EditorialBlueCvTemplateComposer(LegacyTemplateMappers.toCanonical(resolvedTheme));
        this.legacySceneBuilder = new EditorialBlueCvSceneBuilder(resolvedTheme);
    }

    @Override
    public void compose(DocumentComposer composer, MainPageCV originalCv, MainPageCvDTO rewrittenCv) {
        legacySceneBuilder.compose(composer, originalCv, rewrittenCv);
    }

    @Deprecated(forRemoval = false)
    @Override
    public PDDocument render(MainPageCV originalCv, MainPageCvDTO rewrittenCv) {
        return render(originalCv, rewrittenCv, false);
    }

    @Deprecated(forRemoval = false)
    @Override
    public PDDocument render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, boolean guideLines) {
        return renderToDocumentSession(
                guideLines,
                "Failed to generate editorial CV",
                PDRectangle.A4,
                18,
                18,
                18,
                18,
                session -> composer.compose(
                        new SessionTemplateComposeTarget(session),
                        LegacyTemplateMappers.toCanonical(originalCv),
                        LegacyTemplateMappers.toCanonical(rewrittenCv)));
    }

    @Deprecated(forRemoval = false)
    @Override
    public void render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, Path path) {
        render(originalCv, rewrittenCv, path, false);
    }

    @Deprecated(forRemoval = false)
    @Override
    public void render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, Path path, boolean guideLines) {
        renderToFileSession(
                path,
                guideLines,
                "Failed to generate editorial CV",
                "File has been saved to {}",
                PDRectangle.A4,
                18,
                18,
                18,
                18,
                session -> composer.compose(
                        new SessionTemplateComposeTarget(session),
                        LegacyTemplateMappers.toCanonical(originalCv),
                        LegacyTemplateMappers.toCanonical(rewrittenCv)));
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

    private static CvTheme defaultTheme() {
        return new CvTheme(
                new java.awt.Color(18, 31, 72),
                new java.awt.Color(86, 136, 255),
                new java.awt.Color(60, 72, 106),
                new java.awt.Color(86, 136, 255),
                com.demcha.compose.font_library.FontName.HELVETICA,
                com.demcha.compose.font_library.FontName.HELVETICA,
                22,
                10.6,
                9.6,
                3,
                com.demcha.compose.layout_core.components.style.Margin.top(3),
                0);
    }
}
