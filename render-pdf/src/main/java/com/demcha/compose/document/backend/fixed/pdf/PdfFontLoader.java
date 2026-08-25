package com.demcha.compose.document.backend.fixed.pdf;

import lombok.extern.slf4j.Slf4j;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.fontbox.ttf.model.GsubData;
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
     * The one script whose {@code GSUB} substitutions are decoration rather than
     * spelling. Everything else — the Indic scripts PDFBox shapes, above all — needs
     * its substitutions to render at all, and keeps them.
     */
    private static final String DECORATIVE_SUBSTITUTION_SCRIPT = "latn";

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
                TrueTypeFont parsed = new TTFParser().parse(buffer);
                keepLatinTextSpelled(parsed, sourceDescription);
                return parsed;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Stops a Latin face from being drawn as ligatures, so the page says the letters
     * the author wrote.
     *
     * <p>PDFBox runs a font's {@code GSUB} substitutions itself: the moment a
     * {@link PDType0Font} that carries them is made current on a content stream, every
     * string shown through it is rewritten from characters into glyph identifiers, and
     * {@code ti}, {@code tf} and {@code ft} become one glyph each in most of the
     * bundled families. Nothing then records what that glyph <em>meant</em>. The
     * {@code ToUnicode} map a subset font carries is built by reading the font's
     * character map backwards, and a ligature is reachable from no character at all —
     * so the entry is simply absent, and a reader extracting the page loses both
     * letters: {@code Platform} comes back as {@code Pla orm}. It is invisible on
     * screen and fatal everywhere the text layer is what is actually read — search,
     * copy-and-paste, a screen reader, an applicant tracking system parsing a CV.</p>
     *
     * <p>The substitution was never the engine's decision. Layout measures a string
     * with {@code getStringWidth}, which knows nothing of ligatures, so a line drawn
     * with them is a little narrower than the box measured for it; the DOCX and PPTX
     * backends do not substitute either. Turning it off is what makes the PDF draw the
     * text this engine actually laid out, and it is the whole fix: with no
     * substitution the glyphs come from the character map, and the map back to
     * Unicode is complete by construction.</p>
     *
     * <p>What is silenced for a Latin face is the whole of its {@code GSUB}, not the
     * ligature features alone: PDFBox applies {@code ccmp}, {@code liga} and
     * {@code clig} together and offers no way to keep one without the others. In the
     * bundled families that costs nothing — their Latin {@code ccmp} leaves both
     * decomposed combining sequences and precomposed letters drawn exactly as before,
     * and only the ligature pairs change.</p>
     *
     * <p>Only Latin is silenced. PDFBox also shapes Devanagari, Bengali and Gujarati
     * through the same mechanism, and there the substitutions are how the script
     * renders rather than a flourish on top of it — a face whose active script is one
     * of those keeps them.</p>
     *
     * @param ttf               a freshly parsed face
     * @param sourceDescription the face's identity, for logging
     */
    private static void keepLatinTextSpelled(TrueTypeFont ttf, String sourceDescription) {
        try {
            GsubData substitutions = ttf.getGsubData();
            if (substitutions != GsubData.NO_DATA_FOUND
                    && DECORATIVE_SUBSTITUTION_SCRIPT.equals(substitutions.getActiveScriptName())) {
                ttf.setEnableGsub(false);
            }
        } catch (IOException e) {
            // A face whose substitution table cannot be read is still a usable face:
            // PDFBox will reach the same conclusion and substitute nothing.
            log.debug("Unable to read the substitution table of {}", sourceDescription, e);
        }
    }
}
