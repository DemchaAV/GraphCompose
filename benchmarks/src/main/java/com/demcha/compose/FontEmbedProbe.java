package com.demcha.compose;

import com.demcha.compose.document.backend.fixed.pdf.PdfFontLibraryFactory;
import com.demcha.compose.document.backend.fixed.pdf.PdfMeasurementResources;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.font.FontName;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.awt.Color;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Finding 4 probe — quantifies the wasted cost of embedding binary (Google) font
 * families into the throwaway measurement {@code PDDocument}.
 *
 * <p>The canonical pipeline builds two {@code PDDocument}s on a first render:</p>
 * <ul>
 *   <li>a <b>measurement doc</b> ({@code DocumentSession} ->
 *       {@link PdfMeasurementResources#open}) used only to read glyph widths /
 *       line metrics during layout, and</li>
 *   <li>a <b>render doc</b> ({@code PdfFixedLayoutBackend.renderToOutput}) that is
 *       actually saved.</li>
 * </ul>
 *
 * <p>Each binary family the document uses is
 * {@code PDType0Font.load(doc, ttf, subset=true)}-ed into <b>both</b>. The
 * measurement doc is never saved, so that embed is pure waste — Finding 4. The
 * bundled standard-14 families (Helvetica/Times/Courier) use {@code PDType1Font}
 * and embed nothing; only the 30 Google families are binary TTF.</p>
 *
 * <p>This probe measures, <b>warm</b> (steady state, the honest signal per the
 * perf-change workflow) and deterministically (allocated bytes via
 * {@code ThreadMXBean}), the cost of resolving N binary families into a fresh
 * measurement doc. After warm-up the raw font bytes and parsed {@code TrueTypeFont}
 * are already cached, so the residual is precisely the per-document
 * {@code PDType0Font.load} embed — the work F4 proposes to remove from the
 * measurement side. Standard-14 (Helvetica) is the zero-embed baseline; the
 * (binary − Helvetica) delta isolates the embed. One resolved family loads all
 * four faces (regular/bold/italic/boldItalic), so a family costs 4
 * {@code PDType0Font.load} calls. Needs no {@code src/main} changes.</p>
 */
public final class FontEmbedProbe {

    private static final com.sun.management.ThreadMXBean THREAD_MX =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    private static final String SAMPLE = "The quick brown fox Ag 0123456789";
    private static final int WARMUP_ITERATIONS = 8;
    private static final int MEASURED_ITERATIONS = 11;

    /** All 30 bundled binary (Google) families — every family that embeds. */
    private static final List<FontName> GOOGLE_FAMILIES = List.of(
            FontName.LATO, FontName.PT_SANS, FontName.PT_SERIF, FontName.FIRA_SANS, FontName.UBUNTU,
            FontName.ALEGREYA_SANS, FontName.CARLITO, FontName.POPPINS, FontName.BARLOW,
            FontName.BARLOW_CONDENSED, FontName.ASAP_CONDENSED, FontName.ARSENAL, FontName.IBM_PLEX_SERIF,
            FontName.IBM_PLEX_MONO, FontName.CRIMSON_TEXT, FontName.SPECTRAL, FontName.ZILLA_SLAB,
            FontName.GENTIUM_PLUS, FontName.TINOS, FontName.COUSINE, FontName.FIRA_SANS_CONDENSED,
            FontName.KANIT, FontName.VOLKHOV, FontName.TAVIRAJ, FontName.TRIRONG, FontName.SARABUN,
            FontName.PROMPT, FontName.ANDIKA, FontName.BAI_JAMJUREE, FontName.JETBRAINS_MONO);

    private static final List<TextDecoration> FACES = List.of(
            TextDecoration.DEFAULT, TextDecoration.BOLD, TextDecoration.ITALIC, TextDecoration.BOLD_ITALIC);

    /** Width-parity battery: plain text, kerning-prone runs, and sanitize/unencodable cases. */
    private static final List<String> PARITY_STRINGS = List.of(
            "The quick brown fox jumps over the lazy dog",
            "Ag",
            "01234567890",
            "Proportional WAVE Type AVA To. kerning",
            "Em dash — and “smart quotes”  nbsp",
            "Arrows → bullet ● emoji 😀 fallback",
            "   leading and trailing spaces   ",
            "Mixed CASE punctuation!?.,;: (parens) [brackets]");

    public static void main(String[] args) throws Exception {
        BenchmarkSupport.configureQuietLogging();
        new FontEmbedProbe().run();
    }

    private void run() throws Exception {
        enableAllocationMeasurement();

        List<Scenario> scenarios = List.of(
                new Scenario("helvetica (std-14)", List.of(FontName.HELVETICA)),
                new Scenario("1 google (Lato)", List.of(FontName.LATO)),
                new Scenario("2 google (Lato+Poppins)", List.of(FontName.LATO, FontName.POPPINS)),
                new Scenario("3 google (Lato+Poppins+Ubuntu)",
                        List.of(FontName.LATO, FontName.POPPINS, FontName.UBUNTU)));

        System.out.println("GraphCompose Finding-4 Font-Embed Probe (measurement document)");
        System.out.println("Allocation measurement: " + (allocationSupported() ? "enabled" : "UNAVAILABLE"));
        System.out.println("Warm iterations: " + WARMUP_ITERATIONS + ", measured (median): " + MEASURED_ITERATIONS);
        System.out.println();

        // Warm up class-load / JIT / TTF-parse so the measured window reflects the
        // steady-state PDType0Font.load embed, not one-time cold-start cost.
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            for (Scenario scenario : scenarios) {
                measureOnce(scenario);
            }
        }

        List<Result> results = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            long[] allocs = new long[MEASURED_ITERATIONS];
            double[] millis = new double[MEASURED_ITERATIONS];
            for (int i = 0; i < MEASURED_ITERATIONS; i++) {
                Sample sample = measureOnce(scenario);
                allocs[i] = sample.allocBytes();
                millis[i] = sample.nanos() / 1_000_000.0;
            }
            results.add(new Result(scenario, medianLong(allocs), medianDouble(millis)));
        }

        long baselineAlloc = results.get(0).medianAllocBytes();
        double baselineMs = results.get(0).medianMillis();

        System.out.printf("%-32s | %12s | %10s | %14s | %10s%n",
                "Scenario", "Alloc (KB)", "Time (ms)", "Embed Δalloc", "Embed Δms");
        System.out.println("-".repeat(92));
        for (Result result : results) {
            long deltaAlloc = result.medianAllocBytes() - baselineAlloc;
            double deltaMs = result.medianMillis() - baselineMs;
            boolean isBaseline = result == results.get(0);
            System.out.printf("%-32s | %12s | %10.3f | %14s | %10s%n",
                    result.scenario().label(),
                    formatKb(result.medianAllocBytes()),
                    result.medianMillis(),
                    isBaseline ? "(baseline)" : formatKb(deltaAlloc),
                    isBaseline ? "—" : "%.3f".formatted(deltaMs));
        }

        System.out.println();
        System.out.println("Embed Δ = scenario − Helvetica baseline = measurement-doc binary embed (the F4 waste).");
        System.out.println("After F4 the per-thread cache absorbs the embed, so warm google rows collapse toward baseline.");

        parityCheck();
    }

    /**
     * Proves the F4 change is geometry-neutral: for every binary family and face,
     * the measurement-path width must equal the render-path width to the bit. Both
     * resolve through the same cached {@link org.apache.fontbox.ttf.TrueTypeFont},
     * so any non-zero delta would mean a real measurement regression.
     */
    private void parityCheck() throws Exception {
        long comparisons = 0;
        double maxAbsDiff = 0.0;
        String worst = "";

        try (PDDocument renderDocument = new PDDocument();
             PdfMeasurementResources measurement = PdfMeasurementResources.open(List.of())) {
            // Exactly what PdfFixedLayoutBackend builds: a render library that embeds
            // a subset into the (saved) render document.
            FontLibrary renderLibrary = PdfFontLibraryFactory.library(renderDocument, List.of());
            TextMeasurementSystem measure = measurement.textMeasurementSystem();

            for (FontName family : GOOGLE_FAMILIES) {
                PdfFont renderFont = renderLibrary.getFont(family, PdfFont.class)
                        .orElseThrow(() -> new IllegalStateException("missing render font " + family));
                for (TextDecoration face : FACES) {
                    for (String text : PARITY_STRINGS) {
                        TextStyle style = new TextStyle(family, 11.0, face, Color.BLACK);
                        double renderWidth = renderFont.getTextWidth(style, text);
                        double measureWidth = measure.textWidth(style, text);
                        double diff = Math.abs(renderWidth - measureWidth);
                        comparisons++;
                        if (diff > maxAbsDiff) {
                            maxAbsDiff = diff;
                            worst = family + "/" + face + " : \"" + text + "\" (render=" + renderWidth
                                    + ", measure=" + measureWidth + ")";
                        }
                    }
                }
            }
        }

        boolean pass = maxAbsDiff == 0.0;
        System.out.println();
        System.out.printf("PARITY: %s — %d comparisons (%d google families x %d faces x %d strings), max|Δwidth| = %s%n",
                pass ? "PASS (byte-identical render vs measurement)" : "FAIL",
                comparisons, GOOGLE_FAMILIES.size(), FACES.size(), PARITY_STRINGS.size(),
                maxAbsDiff);
        if (!pass) {
            System.out.println("  worst: " + worst);
        }
    }

    private Sample measureOnce(Scenario scenario) throws Exception {
        List<TextStyle> styles = new ArrayList<>();
        for (FontName fontName : scenario.fonts()) {
            styles.add(new TextStyle(fontName, 10.0, TextDecoration.DEFAULT, Color.BLACK));
        }

        long allocBefore = currentThreadAllocatedBytes();
        long t0 = System.nanoTime();
        PdfMeasurementResources resources = PdfMeasurementResources.open(List.of());
        TextMeasurementSystem measurement = resources.textMeasurementSystem();
        double sink = 0;
        for (TextStyle style : styles) {
            // First width call lazily resolves the family -> loads all 4 faces
            // via PDType0Font.load into this throwaway measurement document.
            sink += measurement.textWidth(style, SAMPLE);
        }
        long nanos = System.nanoTime() - t0;
        long allocBytes = allocBefore < 0 ? -1 : currentThreadAllocatedBytes() - allocBefore;

        if (sink < 0) {
            throw new IllegalStateException("unreachable");
        }
        resources.close();
        return new Sample(allocBytes, nanos);
    }

    private static void enableAllocationMeasurement() {
        try {
            if (THREAD_MX.isThreadAllocatedMemorySupported() && !THREAD_MX.isThreadAllocatedMemoryEnabled()) {
                THREAD_MX.setThreadAllocatedMemoryEnabled(true);
            }
        } catch (UnsupportedOperationException ignored) {
            // Allocation measurement unsupported; Alloc column reports n/a.
        }
    }

    private static boolean allocationSupported() {
        try {
            return THREAD_MX.isThreadAllocatedMemorySupported() && THREAD_MX.isThreadAllocatedMemoryEnabled();
        } catch (UnsupportedOperationException ex) {
            return false;
        }
    }

    private static long currentThreadAllocatedBytes() {
        if (!allocationSupported()) {
            return -1;
        }
        return THREAD_MX.getCurrentThreadAllocatedBytes();
    }

    private static long medianLong(long[] values) {
        long[] copy = values.clone();
        Arrays.sort(copy);
        return copy[copy.length / 2];
    }

    private static double medianDouble(double[] values) {
        double[] copy = values.clone();
        Arrays.sort(copy);
        return copy[copy.length / 2];
    }

    private static String formatKb(long bytes) {
        return bytes < 0 ? "n/a" : "%.1f".formatted(bytes / 1024.0);
    }

    private record Scenario(String label, List<FontName> fonts) {
    }

    private record Sample(long allocBytes, long nanos) {
    }

    private record Result(Scenario scenario, long medianAllocBytes, double medianMillis) {
    }
}
