package com.demcha.compose.font_library;

import lombok.extern.slf4j.Slf4j;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class Pdf_FontLoader {

    private static final Map<String, byte[]> RAW_FONT_CACHE = new ConcurrentHashMap<>();

    private static final ThreadLocal<Map<String, TrueTypeFont>> THREAD_LOCAL_TTF_CACHE = ThreadLocal
            .withInitial(HashMap::new);

    private Pdf_FontLoader() {
    }

    public static PDType0Font loadFont(PDDocument document, Path path) {
        String absolutePath = path.toAbsolutePath().toString();

        try {
            byte[] fontBytes = RAW_FONT_CACHE.computeIfAbsent(absolutePath, key -> {
                try {
                    return Files.readAllBytes(path);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read font bytes", e);
                }
            });

            TrueTypeFont ttf = THREAD_LOCAL_TTF_CACHE.get().computeIfAbsent(absolutePath, key -> {
                try {
                    RandomAccessReadBuffer buffer = new RandomAccessReadBuffer(fontBytes);
                    return new TTFParser().parse(buffer);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to parse TTF", e);
                }
            });

            return PDType0Font.load(document, ttf, true);

        } catch (IOException e) {
            log.error("Unable to load font into document from path {}", path, e);
            throw new RuntimeException(e);
        }
    }

    public static PDType0Font loadFont(PDDocument document, InputStream inputStream, String sourceDescription) {
        try (InputStream streamToClose = inputStream) {
            byte[] fontBytes = RAW_FONT_CACHE.computeIfAbsent(sourceDescription, key -> {
                try {
                    return streamToClose.readAllBytes();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            TrueTypeFont ttf = THREAD_LOCAL_TTF_CACHE.get().computeIfAbsent(sourceDescription, key -> {
                try {
                    RandomAccessReadBuffer buffer = new RandomAccessReadBuffer(fontBytes);
                    return new TTFParser().parse(buffer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            return PDType0Font.load(document, ttf, true);
        } catch (IOException e) {
            log.error("Unable to load font from {}", sourceDescription, e);
            throw new RuntimeException(e);
        }
    }
}