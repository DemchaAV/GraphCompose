package com.demcha.system.pdf_systems;

import com.demcha.components.core.Entity;
import com.demcha.core.CanvasSize;
import com.demcha.core.EntityManager;
import com.demcha.system.RenderingSystemECS;
import com.demcha.utils.page_brecker.PageBreaker;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.IOException;
import java.util.*;

/**
 * PdfRenderingSystemECS — diagnostics & hardening
 * <p>
 * Goals:
 * 1) PROVE that margins are not sneaking into the drawn width/height.
 * 2) Avoid any chance that a buggy Padding.zero() leaks non-zero values.
 * 3) Atomically save without PDFBox overwrite warning.
 */
@Slf4j
@Getter
public class PdfRenderingSystemECS implements RenderingSystemECS {

    private final PDDocument doc;
    private final CanvasSize canvasSize;

    public PdfRenderingSystemECS(PDDocument doc, CanvasSize canvasSize) {
        this.doc = doc;
        this.canvasSize = canvasSize;
    }


    @Override
    public void process(EntityManager entityManager) {
        log.info("Processing PdfRenderingSystemECS");

        var entities = entityManager.getLayers();
        for (Map.Entry<Integer, List<UUID>> e : entities.entrySet()) {
            var entitiesUuid = e.getValue();
            LinkedHashMap<UUID, Entity> uuidEntityLinkedHashMap = PageBreaker.sortByYPositionToMap(entityManager, entitiesUuid);

            PageBreaker.sortByYPositionToMap(entityManager,entitiesUuid);
            uuidEntityLinkedHashMap.forEach((uuid, entity) -> {
                if (entity.hasRender(PdfRender.class)) {
                    var render = entity.getPdfRender();
                    var guideLines = entity.isGuideLines();
                    try {
                        render.pdfRender(entity, doc, this, guideLines);
                    } catch (IOException ex) {
                        log.error(ex.getMessage());
                        throw new RuntimeException(ex);
                    }
                }
            });

        }

    }

    public CanvasSize pageSize(int pageIndex) {
        float width = doc.getPage(pageIndex).getMediaBox().getWidth();
        float height = doc.getPage(pageIndex).getMediaBox().getHeight();
        float x = doc.getPage(pageIndex).getMediaBox().getLowerLeftX();
        float y = doc.getPage(pageIndex).getMediaBox().getLowerLeftY();
        return new CanvasSize(width, height, x, y);
    }


    @Override
    public CanvasSize getCanvasSize() {
       return  canvasSize;
    }
}


