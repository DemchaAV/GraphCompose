package com.demcha.compose.jmh;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.ImageBenchmarkFixtures;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.style.DocumentInsets;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Strict JMH micro-benchmark: end-to-end render of an image-heavy document — a
 * dozen distinct raster images placed at thumbnail size — to PDF bytes. Drawing
 * below 50% of native resolution drives {@code PdfImageCache}'s downscale path
 * ({@code ImageIO} decode + bicubic rescale + re-encode + embed), so this covers
 * the raster embed/scale hot path that no other bench touches.
 *
 * <pre>
 *   ./mvnw -f benchmarks/pom.xml clean package -DskipTests
 *   java -jar benchmarks/target/benchmarks.jar Image
 * </pre>
 *
 * @author Artem Demchyshyn
 */
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class ImageJmhBenchmark {

    private static final int IMAGES = 12;

    /** Distinct images built once in setup; the bench measures render, not image synthesis. */
    private List<DocumentImageData> images;

    @Setup
    public void setUp() {
        images = IntStream.range(0, IMAGES)
                .mapToObj(ImageBenchmarkFixtures::distinctImage)
                .toList();
    }

    /**
     * Renders the image-heavy document to PDF bytes.
     *
     * @param blackhole JMH sink
     * @throws Exception if rendering fails
     */
    @Benchmark
    public void renderImageDocument(Blackhole blackhole) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(28))
                .create()) {
            PageFlowBuilder flow = document.pageFlow().name("ImageBenchmark").spacing(8);
            for (DocumentImageData image : images) {
                // 60x33 pt -> ~120x66 px target at 144 DPI, i.e. <50% of the
                // 360x200 native, so the cache builds a downscaled variant.
                flow.addImage(spec -> spec.source(image).size(60, 33));
            }
            flow.build();
            blackhole.consume(document.toPdfBytes());
        }
    }

    /**
     * Runs the JMH harness over this benchmark.
     *
     * @param args JMH CLI arguments
     * @throws Exception if the JMH runner fails
     */
    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
