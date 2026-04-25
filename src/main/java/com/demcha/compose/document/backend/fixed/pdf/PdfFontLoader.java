package com.demcha.compose.document.backend.fixed.pdf;

import lombok.extern.slf4j.Slf4j;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
final class PdfFontLoader {

    private static final Map<String, byte[]> RAW_FONT_CACHE = new ConcurrentHashMap<>();

    private static final ThreadLocal<Map<String, TrueTypeFont>> THREAD_LOCAL_TTF_CACHE = ThreadLocal
            .withInitial(HashMap::new);

    private PdfFontLoader() {
    }

    static PDType0Font loadFont(PDDocument document, InputStream inputStream, String sourceDescription) {
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
