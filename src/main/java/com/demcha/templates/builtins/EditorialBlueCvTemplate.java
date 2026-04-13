package com.demcha.templates.builtins;

import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.templates.CvTheme;
import com.demcha.templates.api.CvTemplate;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.data.MainPageCV;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.Color;
import java.nio.file.Path;
import java.util.Objects;

public class EditorialBlueCvTemplate extends PdfTemplateAdapterSupport implements CvTemplate {
    private static final float PAGE_MARGIN = 18f;

    private final EditorialBlueCvSceneBuilder sceneBuilder;

    public EditorialBlueCvTemplate() {
        this(null);
    }

    public EditorialBlueCvTemplate(CvTheme theme) {
        CvTheme resolvedTheme = Objects.requireNonNullElseGet(theme, EditorialBlueCvTemplate::defaultTheme);
        this.sceneBuilder = new EditorialBlueCvSceneBuilder(resolvedTheme);
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
    public PDDocument render(MainPageCV originalCv, MainPageCvDTO rewrittenCv) {
        return render(originalCv, rewrittenCv, false);
    }

    /**
     * Deprecated compatibility adapter for direct PDFBox document output.
     */
    @Deprecated(forRemoval = false)
    @Override
    public PDDocument render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, boolean guideLines) {
        return renderToDocument(
                guideLines,
                "Failed to generate editorial CV",
                this::createComposer,
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

    /**
     * Deprecated compatibility adapter for direct PDF file writing.
     */
    @Deprecated(forRemoval = false)
    @Override
    public void render(MainPageCV originalCv, MainPageCvDTO rewrittenCv, Path path, boolean guideLines) {
        renderToFile(
                path,
                guideLines,
                "Failed to generate editorial CV",
                "File has been saved to {}",
                this::createComposer,
                composer -> compose(composer, originalCv, rewrittenCv));
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

    private PdfComposer createComposer(Path path, boolean guideLines) {
        return createPdfComposer(path, guideLines, PDRectangle.A4, PAGE_MARGIN);
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
