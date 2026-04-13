package com.demcha.templates.builtins;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.templates.CvTheme;
import com.demcha.templates.api.CvTemplate;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.data.MainPageCV;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.util.Objects;

@Slf4j
public class CvTemplateV1 implements CvTemplate {
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
        try {
            PdfComposer composer = createPdfComposer(null, guideLines);
            compose(composer, originalCv, rewrittenCv);
            return composer.toPDDocument();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CV", e);
        }
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
        try (PdfComposer composer = createPdfComposer(path, guideLines)) {
            compose(composer, originalCv, rewrittenCv);
            composer.build();
            log.info("File has been saved to {}", path.toAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CV", e);
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

    private PdfComposer createPdfComposer(Path path, boolean guideLines) {
        GraphCompose.PdfBuilder builder = path != null ? GraphCompose.pdf(path) : GraphCompose.pdf();
        return builder.pageSize(PDRectangle.A4)
                .margin(15, 10, 15, 15)
                .markdown(true)
                .guideLines(guideLines)
                .create();
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
