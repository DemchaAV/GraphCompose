package com.demcha.system;

import com.demcha.components.core.Entity;
import com.demcha.core.PdfDocument;
import com.demcha.helper.PdfRebuildOpenInAdobe;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * RenderingSystem — diagnostics & hardening
 * <p>
 * Goals:
 * 1) PROVE that margins are not sneaking into the drawn width/height.
 * 2) Avoid any chance that a buggy Padding.zero() leaks non-zero values.
 * 3) Atomically save without PDFBox overwrite warning.
 */
@Slf4j
public class RenderingSystem implements System {

    private final Path outputPath;

    public RenderingSystem() {
        this(Path.of("output.pdf"));
    }

    public RenderingSystem(Path outputPath) {
        this.outputPath = outputPath;
    }

    private static void saveAtomic(PDDocument doc, Path target) throws IOException {
        String closingBatFile = "close-adobe.bat";
        Path parent = target.getParent();
        try {
            PdfRebuildOpenInAdobe.closeAdobeFromResources(closingBatFile, 3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (parent == null) parent = Path.of(".");
        Path tmp = Files.createTempFile(parent, "pdf_", ".tmp");
        try {
            doc.save(tmp.toFile());
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            PdfRebuildOpenInAdobe.openInAdobe(target);
            log.info("Saved PDF to {} (atomic)", target);
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignore) {
            }
        }
    }

//    @Override
//    public void process(PdfDocument pdfDocument) {
//        log.info("Processing RenderingSystem");
//        try (PDDocument doc = pdfDocument.getDocument();
//             PDPageContentStream cs = pdfDocument.openContentStream()) {
//            var vieEntities = pdfDocument.getEntities();
//
//                for (Map.Entry<UUID, Entity> e : vieEntities.entrySet()) {
//                    var entity = e.getValue();
//                    if (entity.hasRender()) {
//                        entity.render(cs);
//                    }
//                }
//            cs.close();
//            saveAtomic(doc, outputPath);
//        } catch (IOException ex) {
//            log.error("Rendering failed", ex);
//        }
//    }

    @Override
    public void process(PdfDocument pdfDocument) {
        log.info("Processing RenderingSystem");
        try (PDDocument doc = pdfDocument.getDocument();
             PDPageContentStream cs = pdfDocument.openContentStream()) {
            var vieEntities = pdfDocument.getEntities();
            var entities = pdfDocument.getLayers();

            for (Map.Entry<Integer, List<UUID>> e : entities.entrySet()) {
                var entitiesUuid = e.getValue();
                for(UUID id : entitiesUuid) {
                    var entity = pdfDocument.getEntity(id).orElseThrow();

                    if (entity.hasRender()) {
                        entity.render(cs);
                    }
                }

            }
            cs.close();
            saveAtomic(doc, outputPath);
        } catch (IOException ex) {
            log.error("Rendering failed", ex);
        }
    }


}
