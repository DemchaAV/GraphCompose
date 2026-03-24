package com.demcha.compose.font_library;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class Pdf_FontLoader {

    private Pdf_FontLoader() {
    }

    public static PDType0Font loadFont(PDDocument document, Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return loadFont(document, inputStream, path.toAbsolutePath().toString());
        } catch (IOException e) {
            log.error("Unable to load font from path {}", path, e);
            throw new RuntimeException(e);
        }
    }

    public static PDType0Font loadFont(PDDocument document, InputStream inputStream, String sourceDescription) {
        try (InputStream closableStream = inputStream) {
            return PDType0Font.load(document, closableStream, true);
        } catch (IOException e) {
            log.error("Unable to load font from {}", sourceDescription, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * @deprecated PDFBox fonts must be loaded into a concrete {@link PDDocument}.
     */
    @Deprecated(forRemoval = false)
    public static PDType0Font loadFont(String path) {
        throw new UnsupportedOperationException(
                "Use loadFont(PDDocument, Path) or loadFont(PDDocument, InputStream, String) instead");
    }
}
