package com.demcha.compose;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Deterministic content-stream probe for the {@code PdfImageCache} dedup path:
 * the same raster image is placed {@code N} times and counted against {@code N}
 * distinct images, so the embed structure isolates exactly what the cache saves.
 *
 * <p>Placing one logical image {@code N} times must embed a single image XObject
 * (referenced by {@code N} {@code Do} draws), while {@code N} distinct images must
 * embed {@code N} XObjects. Counting the distinct image XObjects in the output PDF
 * proves the cache reuses by fingerprint and catches a regression where embeds
 * scale with placements (PDF bloat). Byte-deterministic — no A/B build needed.
 * The image render/scale hot path is also entirely uncovered without this and the
 * companion {@code ImageJmhBenchmark}.</p>
 *
 * @author Artem Demchyshyn
 */
public final class ImageCacheOperatorProbe {

    private static final int PLACEMENTS = 30;

    /** Distinct image XObjects embedded in a PDF, and the number of {@code Do} draws. */
    record EmbedCounts(int embeds, int draws) {
    }

    public static void main(String[] args) throws Exception {
        BenchmarkSupport.configureQuietLogging();

        System.out.println("GraphCompose image-cache embed probe (" + PLACEMENTS + " placements each)");
        System.out.printf("%-22s | %8s | %8s%n", "Mode", "Embeds", "Draws");
        System.out.println("-".repeat(44));
        report("same image x N", countPdf(renderSameImage(PLACEMENTS)));
        report("N distinct images", countPdf(renderDistinctImages(PLACEMENTS)));
        System.out.println();
        System.out.println("Embeds = distinct image XObjects in the PDF, Draws = Do operators. "
                + "PdfImageCache must hold embeds at 1 for the same image regardless of placements; "
                + "distinct images embed once each.");
    }

    private static void report(String mode, EmbedCounts counts) {
        System.out.printf("%-22s | %8d | %8d%n", mode, counts.embeds(), counts.draws());
    }

    /** Renders {@code count} placements of one shared image (cache should embed it once). */
    static byte[] renderSameImage(int count) throws Exception {
        DocumentImageData image = ImageBenchmarkFixtures.demoImage();
        return render(flow -> {
            for (int i = 0; i < count; i++) {
                flow.addImage(spec -> spec.source(image)
                        .size(ImageBenchmarkFixtures.DRAW_WIDTH_PT, ImageBenchmarkFixtures.DRAW_HEIGHT_PT));
            }
        });
    }

    /** Renders {@code count} distinct images (cache embeds each once). */
    static byte[] renderDistinctImages(int count) throws Exception {
        return render(flow -> {
            for (int i = 0; i < count; i++) {
                DocumentImageData image = ImageBenchmarkFixtures.distinctImage(i);
                flow.addImage(spec -> spec.source(image)
                        .size(ImageBenchmarkFixtures.DRAW_WIDTH_PT, ImageBenchmarkFixtures.DRAW_HEIGHT_PT));
            }
        });
    }

    private static byte[] render(Consumer<PageFlowBuilder> author) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4).margin(28, 28, 28, 28).create()) {
            session.pageFlow(flow -> {
                flow.name("ImageCacheProbe").spacing(8);
                author.accept(flow);
            });
            return session.toPdfBytes();
        }
    }

    /** Counts distinct embedded image XObjects (by COS identity) and {@code Do} draws. */
    static EmbedCounts countPdf(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            Set<COSBase> embeds = Collections.newSetFromMap(new IdentityHashMap<>());
            int draws = 0;
            for (PDPage page : document.getPages()) {
                for (var name : page.getResources().getXObjectNames()) {
                    PDXObject xobject = page.getResources().getXObject(name);
                    if (xobject instanceof PDImageXObject image) {
                        embeds.add(image.getCOSObject());
                    }
                }
                List<Object> tokens = new PDFStreamParser(page).parse();
                for (Object token : tokens) {
                    if (token instanceof Operator operator && "Do".equals(operator.getName())) {
                        draws++;
                    }
                }
            }
            return new EmbedCounts(embeds.size(), draws);
        }
    }
}
