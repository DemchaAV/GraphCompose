package com.demcha.compose.document.backend.fixed.pdf;

import lombok.extern.slf4j.Slf4j;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
final class PdfFontLoader {

    /**
     * Maximum number of parsed TrueType font instances kept in the per-thread
     * cache. Real documents almost never use more than a handful of fonts;
     * the cap protects long-lived servlet/worker threads from unbounded
     * accumulation while leaving generous headroom for showcase examples.
     */
    private static final int MAX_TTF_CACHE_ENTRIES = 32;

    private static final Map<String, byte[]> RAW_FONT_CACHE = new ConcurrentHashMap<>();

    /**
     * Per-thread access-order LRU. ThreadLocal already confines the map to one
     * thread, so we do not need an external synchronization wrapper. The cap
     * + eldest-eviction prevents the accumulation observed when a single
     * worker thread renders documents that pull from many font families over
     * its lifetime.
     */
    private static final ThreadLocal<Map<String, TrueTypeFont>> THREAD_LOCAL_TTF_CACHE = ThreadLocal
            .withInitial(() -> new LinkedHashMap<String, TrueTypeFont>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, TrueTypeFont> eldest) {
                    return size() > MAX_TTF_CACHE_ENTRIES;
                }
            });

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
