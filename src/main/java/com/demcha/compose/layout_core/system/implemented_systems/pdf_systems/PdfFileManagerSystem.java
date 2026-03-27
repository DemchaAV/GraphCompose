package com.demcha.compose.layout_core.system.implemented_systems.pdf_systems;

import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.interfaces.FileManagerSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@RequiredArgsConstructor
public class PdfFileManagerSystem implements FileManagerSystem {
    private final Path pathOut;
    private final PDDocument doc;

    @Override
    public void process(EntityManager entityManager) {
        try {
            saveAtomic();
        } catch (IOException e) {
            log.error("\"{}\" Failed to save PDF atomically",this.getClass().getName(), e);
            throw new RuntimeException("Failed to save PDF atomically", e);
        }
    }

    @Override
    public Path outPath() {
        return pathOut;
    }

    @Override
    public boolean saveAtomic() throws IOException {
        Path parent = pathOut.getParent();
        if (parent == null) parent = Path.of(".");

        boolean done = false;
        Path tmp = Files.createTempFile(parent, "pdf_", ".tmp");

        try {
            // Save PDF into a temporary file
            doc.save(tmp.toFile());

            // Atomically replace existing file
            Files.move(tmp, pathOut, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            log.info("\"{}\" Saved PDF to {} (atomic)",this.getClass().getName(), pathOut);

            done = true;
            return true;
        } catch (IOException e) {
            log.error("Failed to save PDF atomically", e);
            throw new IOException("Failed to save PDF atomically", e);
        } finally {
            if (!done) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignore) {
                    log.warn("Could not delete temp file {}", tmp, ignore);
                }
            }
        }
    }

}
