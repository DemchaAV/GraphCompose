package com.demcha.templates.builtins;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.templates.CvTheme;
import com.demcha.templates.api.CvTemplate;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.data.MainPageCV;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.Color;
import java.nio.file.Path;
import java.util.Objects;

@Slf4j
public class EditorialBlueCvTemplate implements CvTemplate {
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
        try {
            PdfComposer composer = createComposer(null, guideLines);
            compose(composer, originalCv, rewrittenCv);
            return composer.toPDDocument();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate editorial CV", e);
        }
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
        try (PdfComposer composer = createComposer(path, guideLines)) {
            compose(composer, originalCv, rewrittenCv);
            composer.build();
            log.info("File has been saved to {}", path.toAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate editorial CV", e);
        }
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
        GraphCompose.PdfBuilder builder = path != null ? GraphCompose.pdf(path) : GraphCompose.pdf();
        return builder.pageSize(PDRectangle.A4)
                .margin(PAGE_MARGIN, PAGE_MARGIN, PAGE_MARGIN, PAGE_MARGIN)
                .markdown(true)
                .guideLines(guideLines)
                .create();
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
