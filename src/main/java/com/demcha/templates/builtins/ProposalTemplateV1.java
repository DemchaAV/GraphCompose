package com.demcha.templates.builtins;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.templates.api.ProposalTemplate;
import com.demcha.templates.data.ProposalData;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;

@Slf4j
public class ProposalTemplateV1 implements ProposalTemplate {
    private static final float PAGE_MARGIN = 22f;

    private final ProposalSceneBuilder sceneBuilder;

    public ProposalTemplateV1() {
        this.sceneBuilder = new ProposalSceneBuilder(new BusinessDocumentSceneStyles());
    }

    /**
     * Preferred backend-neutral template contract.
     */
    @Override
    public void compose(DocumentComposer composer, ProposalData data) {
        sceneBuilder.compose(composer, data);
    }

    @Override
    public String getTemplateId() {
        return "proposal-v1";
    }

    @Override
    public String getTemplateName() {
        return "Proposal V1";
    }

    @Override
    public String getDescription() {
        return "A light business proposal template with executive summary, scope, timeline, pricing, and acceptance terms.";
    }

    /**
     * Deprecated compatibility adapter for direct PDFBox document output.
     */
    @Deprecated(forRemoval = false)
    @Override
    public PDDocument render(ProposalData data) {
        return render(data, false);
    }

    /**
     * Deprecated compatibility adapter for direct PDFBox document output.
     */
    @Deprecated(forRemoval = false)
    @Override
    public PDDocument render(ProposalData data, boolean guideLines) {
        try {
            PdfComposer composer = createComposer(null, guideLines);
            compose(composer, data);
            return composer.toPDDocument();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate proposal", ex);
        }
    }

    /**
     * Deprecated compatibility adapter for direct PDF file writing.
     */
    @Deprecated(forRemoval = false)
    @Override
    public void render(ProposalData data, Path path) {
        render(data, path, false);
    }

    /**
     * Deprecated compatibility adapter for direct PDF file writing.
     */
    @Deprecated(forRemoval = false)
    @Override
    public void render(ProposalData data, Path path, boolean guideLines) {
        try (PdfComposer composer = createComposer(path, guideLines)) {
            compose(composer, data);
            composer.build();
            log.info("Proposal saved to {}", path.toAbsolutePath());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate proposal", ex);
        }
    }

    private PdfComposer createComposer(Path path, boolean guideLines) {
        GraphCompose.PdfBuilder builder = path != null ? GraphCompose.pdf(path) : GraphCompose.pdf();
        return builder.pageSize(PDRectangle.A4)
                .margin(PAGE_MARGIN, PAGE_MARGIN, PAGE_MARGIN, PAGE_MARGIN)
                .markdown(true)
                .guideLines(guideLines)
                .create();
    }
}
