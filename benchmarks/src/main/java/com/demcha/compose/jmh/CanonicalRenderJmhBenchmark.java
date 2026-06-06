package com.demcha.compose.jmh;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Strict JMH micro-benchmark: end-to-end render of a representative
 * multi-section canonical document to PDF bytes.
 *
 * <p>Unlike the manual smoke / stress / endurance harness in this module, this
 * runs in a forked JVM with explicit warmup and measurement phases, so the
 * reported numbers are JIT-stable and comparable across runs. Forked runs need
 * the self-contained jar built by this module's shade plugin (an
 * {@code exec:java} run cannot fork — the child JVM loses the classpath):</p>
 *
 * <pre>
 *   ./mvnw -f benchmarks/pom.xml clean package -DskipTests
 *   java -jar benchmarks/target/benchmarks.jar CanonicalRender
 * </pre>
 *
 * <p>The {@code main} method below delegates to {@link org.openjdk.jmh.Main}.</p>
 *
 * @author Artem Demchyshyn
 */
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class CanonicalRenderJmhBenchmark {

    /**
     * Renders the canonical document to PDF bytes and consumes the result so
     * the JIT cannot elide the work.
     *
     * @param blackhole JMH sink that consumes the rendered bytes
     * @throws Exception if rendering fails
     */
    @Benchmark
    public void renderCanonicalDocument(Blackhole blackhole) throws Exception {
        blackhole.consume(renderDocument());
    }

    private static byte[] renderDocument() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(36))
                .create()) {
            document.pageFlow()
                    .name("BenchmarkCv")
                    .spacing(10)
                    .addParagraph("Jordan Rivera")
                    .addParagraph("Backend Java Developer")
                    .addSection("Summary", section -> section
                            .addParagraph("Platform engineer building resilient PDF and "
                                    + "document-generation workflows for reliable business output."))
                    .addSection("Skills", section -> section
                            .addParagraph("Java 21, PDFBox, Maven, REST APIs")
                            .addParagraph("Pagination, semantic layout composition, template design systems")
                            .addParagraph("Testing strategy, CI pipelines, developer enablement"))
                    .addSection("Experience", section -> section
                            .addParagraph("Senior Platform Engineer, Northwind Systems (2024-present)")
                            .addParagraph("Software Engineer, BrightLeaf Labs (2021-2024)"))
                    .addSection("Education", section -> section
                            .addParagraph("MSc Computer Science, University of Manchester (2021)")
                            .addParagraph("Oracle Java Certification (2023)"))
                    .build();
            return document.toPdfBytes();
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
