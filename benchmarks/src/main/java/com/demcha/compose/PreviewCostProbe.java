package com.demcha.compose;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.presets.ModernProfessional;
import com.demcha.compose.document.templates.proposal.presets.ModernProposal;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.ToLongFunction;

/**
 * Times each stage of the current preview path, so its cost is measured rather
 * than assumed.
 *
 * <p>{@code DocumentSession.toImages(dpi)} reaches a raster through: open the
 * session &rarr; compose &rarr; layout &rarr; build a {@code PDDocument} and
 * {@code save()} it to bytes &rarr; {@code Loader.loadPDF} &rarr;
 * {@code PDFRenderer}. This probe reports each of those separately, warm, on two
 * canonical workloads.</p>
 *
 * <h2>What these numbers do and do not say</h2>
 *
 * <p>They describe the pipeline that exists. They are <strong>not</strong> a bound
 * on what a direct Java2D backend would save, and no stage here should be read as
 * "removable" or "unavoidable":</p>
 *
 * <ul>
 *   <li>The <em>PDF encode</em> stage is not serialisation. Before {@code save()}
 *       the backend creates pages, walks the whole {@code LayoutGraph}, paints
 *       every fragment, resolves links and bookmarks, and applies headers,
 *       footers, metadata, watermark and protection. A direct renderer would
 *       replace that painting work, not delete it.</li>
 *   <li>The <em>PDFRenderer</em> stage is not pixel production alone. It parses and
 *       interprets PDF operators, then paints them through Java2D. A direct
 *       renderer would skip the interpretation and keep the painting.</li>
 * </ul>
 *
 * <p>A direct backend therefore changes work in <em>both</em> stages, and no
 * measurement of the present pipeline can predict the result. Comparing the two
 * designs needs a second implementation to measure against.</p>
 *
 * <h2>Measurement notes</h2>
 *
 * <ul>
 *   <li>Every DPI renders from a freshly parsed document and a fresh
 *       {@code PDFRenderer}, so a later DPI does not inherit the glyph and
 *       resource caches an earlier one warmed. The parse is outside the timed
 *       region.</li>
 *   <li>{@code toImages} is measured on its own sessions rather than after four
 *       raster passes over the same document.</li>
 *   <li>Opening the session is timed separately from composing into it, so the
 *       measurement-service setup the constructor performs is not reported as
 *       DSL cost.</li>
 *   <li>Totals are summed <em>within</em> an iteration and the median of those
 *       totals is reported: medians of separate stages do not add up to the
 *       median of their sum.</li>
 * </ul>
 *
 * <p>Reads nothing private and changes no {@code src/main} code.</p>
 */
public final class PreviewCostProbe {

    private static final int WARMUP_ITERATIONS = 6;
    private static final int MEASURED_ITERATIONS = 11;
    private static final int REPORT_DPI = 96;

    private PreviewCostProbe() {
    }

    /**
     * Runs the probe and prints one line per measured stage.
     *
     * @param args ignored
     * @throws Exception if any render stage fails
     */
    public static void main(String[] args) throws Exception {
        measure("canonical CV, ModernProfessional",
                CanonicalBenchmarkSupport.canonicalCv(),
                ModernProfessional.create());
        measure("long proposal, ModernProposal",
                CanonicalBenchmarkSupport.canonicalProposal(),
                ModernProposal.create());
    }

    private static <T> void measure(String label, T data, DocumentTemplate<T> template) throws Exception {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            Stages warm = new Stages();
            runStages(data, template, warm);
            runToImages(data, template, warm);
        }

        List<Stages> samples = new ArrayList<>();
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            Stages stages = new Stages();
            runStages(data, template, stages);
            runToImages(data, template, stages);
            samples.add(stages);
        }

        System.out.println("=== preview pipeline, stage by stage (" + label + ") ===");
        System.out.printf("pages=%d pdfBytes=%d%n", samples.get(0).pages, samples.get(0).pdfBytes);
        report("session open", samples, s -> s.sessionOpenNanos);
        report("compose (DSL build)", samples, s -> s.composeNanos);
        report("layout (compile graph)", samples, s -> s.layoutNanos);
        report("PDF encode: paint graph + save", samples, s -> s.encodeNanos);
        report("PDF parse: Loader.loadPDF", samples, s -> s.parseNanos);
        report("PDFRenderer 72dpi, all pages", samples, s -> s.raster72Nanos);
        report("PDFRenderer 96dpi, all pages", samples, s -> s.raster96Nanos);
        report("PDFRenderer 150dpi, all pages", samples, s -> s.raster150Nanos);
        report("PDFRenderer 96dpi, first page", samples, s -> s.raster96FirstNanos);
        report("sum of the stages at 96dpi", samples, Stages::pipelineSumAt96);
        report("toImages(96), layout cached", samples, s -> s.toImagesCachedNanos);
        report("toImages(96), fresh session", samples, s -> s.toImagesFromScratchNanos);
        System.out.println();
    }

    /** One pass over the pipeline, timing each stage in isolation. */
    private static <T> void runStages(T data, DocumentTemplate<T> template, Stages stages) throws Exception {
        // Opening the session is its own stage: the constructor resolves the
        // backend's measurement services before a single node is composed.
        // Folding it into the compose figure would attribute that setup to the
        // DSL, which is the one thing this decomposition exists to avoid.
        long t0 = System.nanoTime();
        DocumentSession document = session();
        long tOpen = System.nanoTime();
        stages.sessionOpenNanos = tOpen - t0;
        try (document) {
            template.compose(document, data);
            long t1 = System.nanoTime();
            stages.composeNanos = t1 - tOpen;

            document.layoutGraph();
            long t2 = System.nanoTime();
            stages.layoutNanos = t2 - t1;

            // Layout is cached per revision, so this times the backend painting the
            // graph into a PDDocument and saving it — not serialisation alone.
            byte[] bytes = document.toPdfBytes();
            stages.encodeNanos = System.nanoTime() - t2;
            stages.pdfBytes = bytes.length;

            long t3 = System.nanoTime();
            try (PDDocument parsed = Loader.loadPDF(bytes)) {
                stages.parseNanos = System.nanoTime() - t3;
                stages.pages = parsed.getNumberOfPages();
            }

            // A fresh parse per DPI: one shared PDFRenderer would hand every later
            // DPI the caches the first one warmed.
            stages.raster72Nanos = rasterAllPages(bytes, 72);
            stages.raster96Nanos = rasterAllPages(bytes, REPORT_DPI);
            stages.raster150Nanos = rasterAllPages(bytes, 150);
            stages.raster96FirstNanos = rasterFirstPage(bytes, REPORT_DPI);
        }
    }

    /**
     * The public call, measured on its own sessions so it is not preceded by four
     * raster passes over the same document.
     */
    private static <T> void runToImages(T data, DocumentTemplate<T> template, Stages stages) throws Exception {
        try (DocumentSession document = session()) {
            template.compose(document, data);
            document.layoutGraph();
            long start = System.nanoTime();
            sinkAll(document.toImages(REPORT_DPI));
            stages.toImagesCachedNanos = System.nanoTime() - start;
        }

        long start = System.nanoTime();
        try (DocumentSession document = session()) {
            template.compose(document, data);
            sinkAll(document.toImages(REPORT_DPI));
        }
        stages.toImagesFromScratchNanos = System.nanoTime() - start;
    }

    private static DocumentSession session() {
        return GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(36))
                .create();
    }

    private static long rasterAllPages(byte[] pdf, int dpi) throws Exception {
        try (PDDocument parsed = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(parsed);
            int pages = parsed.getNumberOfPages();
            long start = System.nanoTime();
            for (int page = 0; page < pages; page++) {
                sink(renderer.renderImageWithDPI(page, dpi, ImageType.RGB));
            }
            return System.nanoTime() - start;
        }
    }

    private static long rasterFirstPage(byte[] pdf, int dpi) throws Exception {
        try (PDDocument parsed = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(parsed);
            long start = System.nanoTime();
            sink(renderer.renderImageWithDPI(0, dpi, ImageType.RGB));
            return System.nanoTime() - start;
        }
    }

    private static int guard;

    private static void sinkAll(List<BufferedImage> images) {
        for (BufferedImage image : images) {
            sink(image);
        }
    }

    private static void sink(BufferedImage image) {
        guard += image.getWidth() + image.getHeight();
    }

    private static void report(String label, List<Stages> samples, ToLongFunction<Stages> field) {
        List<Long> values = new ArrayList<>(samples.size());
        for (Stages sample : samples) {
            values.add(field.applyAsLong(sample));
        }
        Collections.sort(values);
        double medianMs = values.get(values.size() / 2) / 1_000_000.0;
        double minMs = values.get(0) / 1_000_000.0;
        double maxMs = values.get(values.size() - 1) / 1_000_000.0;
        System.out.printf("%-32s median=%8.2f ms   min=%8.2f   max=%8.2f%n", label, medianMs, minMs, maxMs);
    }

    private static final class Stages {
        long sessionOpenNanos;
        long composeNanos;
        long layoutNanos;
        long encodeNanos;
        long parseNanos;
        long raster72Nanos;
        long raster96Nanos;
        long raster150Nanos;
        long raster96FirstNanos;
        long toImagesCachedNanos;
        long toImagesFromScratchNanos;
        int pages;
        int pdfBytes;

        /** Summed per iteration, so the reported median is a median of real totals. */
        long pipelineSumAt96() {
            return sessionOpenNanos + composeNanos + layoutNanos + encodeNanos
                    + parseNanos + raster96Nanos;
        }
    }
}
