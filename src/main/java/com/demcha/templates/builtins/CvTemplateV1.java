package com.demcha.templates.builtins;

import com.demcha.compose.document.templates.support.CvTemplateComposer;
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
 * Deprecated bridge to the canonical {@code document.templates} CV template.
 */
@Deprecated(forRemoval = false)
public class CvTemplateV1 extends PdfTemplateAdapterSupport implements CvTemplate {
    private final CvTemplateComposer composer;
    private final CvSceneBuilder legacySceneBuilder;

    public CvTemplateV1() {
        this(CvTheme.defaultTheme());
    }

    public CvTemplateV1(CvTheme theme) {
        CvTheme resolvedTheme = Objects.requireNonNullElseGet(theme, CvTheme::defaultTheme);
        this.composer = new CvTemplateComposer(LegacyTemplateMappers.toCanonical(resolvedTheme));
        this.legacySceneBuilder = new CvSceneBuilder(resolvedTheme);
    }

    @Override
    public void compose(DocumentComposer composer, MainPageCV originalCv, MainPageCvDTO rewrittenCv) {
        legacySceneBuilder.compose(composer, originalCv, rewrittenCv);
    }

    @Deprecated(forRemoval = false)
    @Override
    public PDDocument render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, boolean guideLines) {
        return renderToDocumentSession(
                guideLines,
                "Failed to generate CV",
                PDRectangle.A4,
                15,
                10,
                15,
                15,
                session -> composer.compose(
                        new SessionTemplateComposeTarget(session),
                        LegacyTemplateMappers.toCanonical(originalCv),
                        LegacyTemplateMappers.toCanonical(rewrittenCv)));
    }

    @Deprecated(forRemoval = false)
    @Override
    public PDDocument render(MainPageCV originalCv, MainPageCvDTO rewrittenCv) {
        return render(originalCv, rewrittenCv, false);
    }

    @Deprecated(forRemoval = false)
    @Override
    public void render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, Path path, boolean guideLines) {
        renderToFileSession(
                path,
                guideLines,
                "Failed to generate CV",
                "File has been saved to {}",
                PDRectangle.A4,
                15,
                10,
                15,
                15,
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
}
