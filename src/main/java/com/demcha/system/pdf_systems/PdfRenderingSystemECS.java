package com.demcha.system.pdf_systems;

import com.demcha.core.CanvasSize;
import com.demcha.core.EntityManager;
import com.demcha.system.RenderingSystemECS;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PdfRenderingSystemECS — diagnostics & hardening
 * <p>
 * Goals:
 * 1) PROVE that margins are not sneaking into the drawn width/height.
 * 2) Avoid any chance that a buggy Padding.zero() leaks non-zero values.
 * 3) Atomically save without PDFBox overwrite warning.
 */
@Slf4j

public class PdfRenderingSystemECS implements RenderingSystemECS {

    public static final PDRectangle DEFAULT_PAGE = PDRectangle.A4;
    private final PDDocument doc;

    public PdfRenderingSystemECS(PDDocument doc, PDPage page) {
        this.doc = doc;
        doc.addPage(page);
    }

    public PdfRenderingSystemECS(PDDocument doc) {
        this.doc = doc;
    }

    @Override
    public void process(EntityManager entityManager) {
        log.info("Processing PdfRenderingSystemECS");
        try (PDPageContentStream cs = openContentStream(0)) {
            var entities = entityManager.getLayers();
            for (Map.Entry<Integer, List<UUID>> e : entities.entrySet()) {
                var entitiesUuid = e.getValue();
                for (UUID id : entitiesUuid) {
                    var entity = entityManager.getEntity(id).orElseThrow();

                    if (entity.hasRender(PdfRender.class)) {
                        var render = entity.getPdfRender();
                        var guideLines = entity.isGuideLines();
                        render.pdfRender(entity, cs,doc,0, guideLines);
                    }
                }

            }
        } catch (IOException ex) {
            log.error("Rendering failed", ex);
        }
    }

    private PDPageContentStream openContentStream(int pageIndex) throws IOException {
        return new PDPageContentStream(
                doc, doc.getPage(pageIndex),
                PDPageContentStream.AppendMode.APPEND,   // keep existing content if any
                true,                                    // compress
                true                                     // resetContext: isolates graphics state (PDFBox 3)
        );
    }

    public CanvasSize pageSize(int pageIndex) {
        float width = doc.getPage(pageIndex).getMediaBox().getWidth();
        float height = doc.getPage(pageIndex).getMediaBox().getHeight();
        float x = doc.getPage(pageIndex).getMediaBox().getLowerLeftX();
        float y = doc.getPage(pageIndex).getMediaBox().getLowerLeftY();
        return new CanvasSize(width, height, x, y);
    }

}


