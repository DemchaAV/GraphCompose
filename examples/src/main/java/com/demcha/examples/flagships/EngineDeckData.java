package com.demcha.examples.flagships;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Data layer for {@link EngineDeckExample} — the v2-style separation of WHAT the
 * deck shows from HOW it is rendered. Holds the real comparative benchmark run
 * (loaded from the bundled result file) and the deck's structured content
 * (capability chips, engine capabilities, output backends, pipeline steps, proof
 * points) as plain records. No colours, no styling, no layout: the composition
 * reads this and decides how to draw it.
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
final class EngineDeckData {

    /** Library column order for the comparison/scaling charts. */
    static final String[] LIBS = {"GraphCompose", "iText 9", "JasperReports"};

    /** Row counts swept by the scaling benchmark. */
    static final int[] SIZES = {40, 200, 1000};

    private EngineDeckData() {
    }

    // ── Real benchmark data ───────────────────────────────────────────────────

    /** A comparative benchmark run: metadata plus per-scenario time/heap. */
    record BenchRun(String timestamp, int warmup, int measure, Map<String, double[]> rows) {
        double timeMs(String label) {
            return row(label)[0];
        }

        double heapMb(String label) {
            return row(label)[1];
        }

        private double[] row(String label) {
            return Objects.requireNonNull(rows.get(label), () -> "no benchmark row: " + label);
        }

        double timeMs(String lib, int size) {
            return timeMs(lib + " (" + size + " rows)");
        }

        double heapMb(String lib, int size) {
            return heapMb(lib + " (" + size + " rows)");
        }
    }

    /**
     * Loads the bundled snapshot of {@code target/benchmarks/comparative/latest.json}.
     * Refresh: re-run {@code scripts/run-benchmarks.ps1} and recopy that file over
     * {@code resources/benchmarks/comparative.json}.
     */
    static BenchRun loadBench() {
        try (InputStream in = Objects.requireNonNull(
                EngineDeckData.class.getResourceAsStream("/benchmarks/comparative.json"),
                "benchmark data missing: /benchmarks/comparative.json")) {
            JsonNode root = new ObjectMapper().readTree(in);
            Map<String, double[]> rows = new LinkedHashMap<>();
            for (JsonNode r : root.get("libraries")) {
                rows.put(r.get("library").asText(),
                        new double[]{r.get("avgTimeMs").asDouble(), r.get("avgHeapMb").asDouble()});
            }
            return new BenchRun(root.get("timestamp").asText(),
                    root.get("warmupIterations").asInt(),
                    root.get("measurementIterations").asInt(),
                    rows);
        } catch (Exception e) {
            throw new IllegalStateException("failed to load benchmark data", e);
        }
    }

    // ── Banner content (page 1) ───────────────────────────────────────────────

    /** A labelled icon — used for the capability strip and the engine grid. */
    record IconLabel(String icon, String label) {
    }

    /** Bottom capability strip. */
    static List<IconLabel> capabilities() {
        return List.of(
                new IconLabel("github", "Open Source"),
                new IconLabel("maven", "Maven Central"),
                new IconLabel("java", "Java 17+"),
                new IconLabel("license", "MIT License"));
    }

    /** Engine capabilities, in pipeline order (Layout first, DSL last). */
    static List<IconLabel> engineCapabilities() {
        return List.of(
                new IconLabel("layout", "Layout"),
                new IconLabel("page-break", "Pagination"),
                new IconLabel("themes", "Themes"),
                new IconLabel("code", "Components"),
                new IconLabel("testing", "Snapshot Tests"),
                new IconLabel("dsl", "DSL"));
    }

    /** An output backend card; {@code live} drives the accent colour. */
    record Backend(String icon, String title, String sub, boolean live) {
    }

    static List<Backend> backends() {
        return List.of(
                new Backend("pdf-file", "PDFBox 3.0", "Production backend", true),
                new Backend("docx", "DOCX export", "Semantic export", false),
                new Backend("ppt-file", "PPTX", "Planned", false));
    }

    // ── "How it works" content (page 2) ───────────────────────────────────────

    /** A pipeline step: number, icon, title, one-line description. */
    record PipelineStep(String number, String icon, String title, String desc) {
    }

    static List<PipelineStep> pipeline() {
        return List.of(
                new PipelineStep("1", "dsl", "AUTHOR", "Fluent DSL describes intent."),
                new PipelineStep("2", "layout", "MEASURE", "Two-pass geometry, every node."),
                new PipelineStep("3", "page-break", "PAGINATE", "Split the flow across pages."),
                new PipelineStep("4", "pdf-file", "RENDER", "PDFBox writes the bytes."));
    }

    /** A "why it's solid" proof card: bold title + a sentence. */
    record Proof(String title, String desc) {
    }

    static List<Proof> proof() {
        return List.of(
                new Proof("Deterministic",
                        "The same input renders the same bytes on every machine — layout is reproducible, "
                                + "not best-effort."),
                new Proof("Regression-tested",
                        "layoutSnapshot() diffs the geometry and PdfVisualRegression pixel-tests fonts and "
                                + "colour, right in your pull requests."),
                new Proof("Production-ready",
                        "An isolated PDFBox backend streams straight to an HTTP response — no temp files, "
                                + "no manual coordinates."));
    }
}
