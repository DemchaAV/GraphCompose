package com.demcha.system;

import com.demcha.components.content.Stroke;
import com.demcha.components.content.rectangle.Rectangle;
import com.demcha.components.content.text.Text;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.BoxSize;
import com.demcha.components.layout.ComputedPosition;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.PdfDocument;
import com.demcha.helper.PdfRebuildOpenInAdobe;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
            log.info("Saved PDF to {} (atomic)", target);
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignore) {
            }
        }
    }

    @Override
    public void process(PdfDocument pdfDocument) {
        log.info("Processing RenderingSystem");
        try (PDDocument doc = pdfDocument.getDocument();
             PDPageContentStream cs = pdfDocument.openContentStream()) {
            Optional<Set<UUID>> optionalUUIDList = pdfDocument.getEntitiesWithComponent(ComputedPosition.class);
            var entities = optionalUUIDList.stream().map(
                            (Set<UUID> id) -> id.stream()
                                    .map((uuid -> pdfDocument.getEntity(uuid)))
                                    .toList())
                    .flatMap(List::stream)
                    .filter(Optional::isPresent)
                    .map(Optional::get).toList();
            for (Entity e : entities) {
                boolean any = false;
                any |= renderRectangleIfPresent(e, cs);
                any |= renderTextIfPresent(e, cs);
                if (!any) log.debug("Nothing to render for {}", e);
            }
            cs.close();
            saveAtomic(doc, outputPath);
        } catch (IOException ex) {
            log.error("Rendering failed", ex);
        }
    }

    private boolean renderRectangleIfPresent(Entity e, PDPageContentStream cs) throws IOException {
        var rectOpt = e.getComponent(Rectangle.class);
        if (rectOpt.isEmpty()) return false;

        var boxOpt = e.getComponent(BoxSize.class);
        var posOpt = e.getComponent(ComputedPosition.class);
        if (boxOpt.isEmpty() || posOpt.isEmpty()) {
            log.warn("Rectangle missing BoxSize/ComputedPosition; skipping: {}", e);
            return false;
        }

        var box = boxOpt.get();
        var pos = posOpt.get();

        // Use padding ONLY if explicitly present (don't rely on Padding.zero()).
        var padOpt = e.getComponent(Padding.class);
        double padH = padOpt.map(Padding::horizontal).orElse(0.0);
        double padV = padOpt.map(Padding::vertical).orElse(0.0);

        // Diagnostics: ensure we are not using margins in geometry
        var m = e.getComponent(Margin.class).orElse(Margin.zero());
        if (Math.abs(padH - (m.left() + m.right())) < 1e-6 || Math.abs(padV - (m.top() + m.bottom())) < 1e-6) {
            log.warn("[DIAG] Padding equals Margin on entity {}; check component wiring. padH={}, padV={}, marginH={}, marginV={}",
                    e, padH, padV, m.horizontal(), m.vertical());
        }

        double drawW = box.width() + padH;   // content + padding (NOT margin)
        double drawH = box.height() + padV;  // content + padding (NOT margin)
        double x = pos.x();                  // position already includes margin
        double y = pos.y();

        float strokeWidth = e.getComponent(Stroke.class).map(s -> (float) s.width()).orElse(1.0f);

        log.debug("Rendering rectangle {} at ({}, {}) content=({}, {}) pad=({}, {}) -> draw=({}, {})", rectOpt.get(), x, y,
                box.width(), box.height(), padH, padV, drawW, drawH);

        cs.saveGraphicsState();
        cs.setLineWidth(strokeWidth);
        cs.addRect((float) x, (float) y, (float) drawW, (float) drawH);
        cs.stroke();
        cs.restoreGraphicsState();
        return true;
    }

    private boolean renderTextIfPresent(Entity e, PDPageContentStream cs) throws IOException {
        var textOpt = e.getComponent(Text.class);
        if (textOpt.isEmpty()) return false;
        var posOpt = e.getComponent(ComputedPosition.class);
        if (posOpt.isEmpty()) {
            log.warn("Text has no ComputedPosition; skipping: {}", e);
            return false;
        }

        var text = textOpt.get();
        var pos = posOpt.get();
        TextStyle style = text.textStyle();
        float size = style != null ? style.size() : 12f;

        log.debug("Rendering text '{}' at ({}, {}) size={}", text.text(), pos.x(), pos.y(), size);

        cs.saveGraphicsState();
        cs.setFont(style != null ? style.font() : new PDType1Font(Standard14Fonts.FontName.HELVETICA), size);
        cs.beginText();
        cs.newLineAtOffset((float) pos.x(), (float) pos.y());
        cs.showText(text.text());
        cs.endText();
        cs.restoreGraphicsState();
        return true;
    }
}
