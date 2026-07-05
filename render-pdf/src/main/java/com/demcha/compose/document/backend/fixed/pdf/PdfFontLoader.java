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

    /**
     * Per-thread, never-saved document that owns measurement-only embedded fonts.
     *
     * <p>The layout pipeline reads glyph widths, vertical metrics and glyph
     * coverage from a real {@link PDType0Font}. Those answers are derived from the
     * parsed {@link TrueTypeFont} (advance widths, descriptor tables, cmap) and do
     * not depend on which document owns the font, so a single reusable owner per
     * thread produces byte-identical metrics to the per-render embed. The document
     * is never saved, so the deferred subset build never runs; it only accumulates
     * the bounded set of distinct font faces touched on the thread.</p>
     */
    private static final ThreadLocal<PDDocument> THREAD_LOCAL_MEASUREMENT_DOCUMENT =
            ThreadLocal.withInitial(PDDocument::new);

    /**
     * Per-thread cache of measurement-only fonts keyed by source description,
     * bound to {@link #THREAD_LOCAL_MEASUREMENT_DOCUMENT}.
     *
     * <p>Deliberately <b>uncapped</b>, unlike {@link #THREAD_LOCAL_TTF_CACHE}.
     * Evicting an entry would not free anything: the {@link PDType0Font} stays
     * registered in the never-pruned measurement document, and the next use of
     * that face would {@code PDType0Font.load} a <em>second</em> copy into the same
     * document — so an LRU here grows the document on every evict/reload instead of
     * bounding it. Loading each face exactly once per thread keeps the document at
     * one font per distinct face, which is the real bound (≈ the bundled face count
     * plus any custom faces the thread touches).</p>
     */
    private static final ThreadLocal<Map<String, PDType0Font>> THREAD_LOCAL_MEASUREMENT_FONT_CACHE =
            ThreadLocal.withInitial(HashMap::new);

    private PdfFontLoader() {
    }

    /**
     * Loads a binary font and embeds a fresh subset into {@code document}. Used by
     * the render path, where the font program is written when the document is
     * saved.
     */
    static PDType0Font loadFont(PDDocument document, InputStream inputStream, String sourceDescription) {
        try (InputStream streamToClose = inputStream) {
            TrueTypeFont ttf = resolveTrueTypeFont(streamToClose, sourceDescription);
            return PDType0Font.load(document, ttf, true);
        } catch (IOException e) {
            log.error("Unable to load font from {}", sourceDescription, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads a binary font for the <b>measurement</b> pipeline.
     *
     * <p>Unlike {@link #loadFont(PDDocument, InputStream, String)} — which embeds a
     * fresh subset into the saved render document on every render — this returns a
     * per-thread cached {@link PDType0Font} bound to a reusable, never-saved
     * measurement document. Width, vertical-metric and glyph-coverage answers are
     * derived from the parsed {@link TrueTypeFont} and are therefore byte-identical
     * to the render font, so layout geometry is unchanged; the only difference is
     * that the embed cost is paid once per thread instead of once per
     * {@code DocumentSession} (Finding 4: the measurement document was discarded,
     * so its embed was pure waste).</p>
     *
     * @param inputStream       font data stream (closed by this method)
     * @param sourceDescription stable identity used as the cache key
     * @return a reusable measurement font for the current thread
     */
    static PDType0Font loadMeasurementFont(InputStream inputStream, String sourceDescription) {
        try (InputStream streamToClose = inputStream) {
            Map<String, PDType0Font> measurementFonts = THREAD_LOCAL_MEASUREMENT_FONT_CACHE.get();
            PDType0Font cached = measurementFonts.get(sourceDescription);
            if (cached != null) {
                return cached;
            }

            TrueTypeFont ttf = resolveTrueTypeFont(streamToClose, sourceDescription);
            PDType0Font measurementFont = PDType0Font.load(THREAD_LOCAL_MEASUREMENT_DOCUMENT.get(), ttf, true);
            measurementFonts.put(sourceDescription, measurementFont);
            return measurementFont;
        } catch (IOException e) {
            log.error("Unable to load measurement font from {}", sourceDescription, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Resolves the parsed {@link TrueTypeFont} for a source, reusing the shared raw
     * byte cache and the per-thread parsed-font cache. Shared by the render and
     * measurement load paths so both observe identical font programs.
     */
    private static TrueTypeFont resolveTrueTypeFont(InputStream streamToClose, String sourceDescription) {
        byte[] fontBytes = RAW_FONT_CACHE.computeIfAbsent(sourceDescription, key -> {
            try {
                return streamToClose.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return THREAD_LOCAL_TTF_CACHE.get().computeIfAbsent(sourceDescription, key -> {
            try {
                RandomAccessReadBuffer buffer = new RandomAccessReadBuffer(fontBytes);
                return new TTFParser().parse(buffer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
