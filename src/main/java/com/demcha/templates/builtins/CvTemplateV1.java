package com.demcha.templates.builtins;

import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.templates.CvTheme;
import com.demcha.templates.api.CvTemplate;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.data.MainPageCV;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.util.Objects;

public class CvTemplateV1 extends PdfTemplateAdapterSupport implements CvTemplate {
    private final CvSceneBuilder sceneBuilder;

    public CvTemplateV1() {
        this(CvTheme.defaultTheme());
    }

    public CvTemplateV1(CvTheme theme) {
        CvTheme resolvedTheme = Objects.requireNonNullElseGet(theme, CvTheme::defaultTheme);
        this.sceneBuilder = new CvSceneBuilder(resolvedTheme);
    }

    /**
     * Preferred backend-neutral template contract.
     */
    @Override
    public void compose(DocumentComposer composer, MainPageCV originalCv, MainPageCvDTO rewrittenCv) {
        sceneBuilder.compose(composer, originalCv, rewrittenCv);
    }

    /**
     * Deprecated compatibility adapter for direct PDFBox document output.
     */
    @Deprecated(forRemoval = false)
    @Override
    public PDDocument render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, boolean guideLines) {
        return renderToDocument(
                guideLines,
                "Failed to generate CV",
                this::createPdfComposer,
                composer -> compose(composer, originalCv, rewrittenCv));
    }

    /**
     * Deprecated compatibility adapter for direct PDFBox document output.
     */
    @Deprecated(forRemoval = false)
    @Override
    public PDDocument render(MainPageCV originalCv, MainPageCvDTO rewrittenCv) {
        return render(originalCv, rewrittenCv, false);
    }

    /**
     * Deprecated compatibility adapter for direct PDF file writing.
     */
    @Deprecated(forRemoval = false)
    @Override
    public void render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, Path path, boolean guideLines) {
        renderToFile(
                path,
                guideLines,
                "Failed to generate CV",
                "File has been saved to {}",
                this::createPdfComposer,
                composer -> compose(composer, originalCv, rewrittenCv));
    }

    /**
     * Deprecated compatibility adapter for direct PDF file writing.
     */
    @Deprecated(forRemoval = false)
    @Override
    public void render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, Path path) {
        render(originalCv, rewrittenCv, path, false);
    }

    private PdfComposer createPdfComposer(Path path, boolean guideLines) {
        return createPdfComposer(path, guideLines, PDRectangle.A4, 15, 10, 15, 15);
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
