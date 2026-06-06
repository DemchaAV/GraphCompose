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
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Strict JMH benchmark: render a long, multi-section document that paginates
 * across several A4 pages — exercises the pagination / multi-page render path
 * that the small single-page {@link CanonicalRenderJmhBenchmark} does not.
 *
 * <p>{@code sectionCount} is a JMH parameter so the cost can be charted against
 * document length. Run through the shaded runner jar (see the module README).</p>
 *
 * @author Artem Demchyshyn
 */
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class PaginatedDocumentJmhBenchmark {

    /** Number of sections (drives the page count). */
    @Param({"40"})
    public int sectionCount;

    /**
     * Builds and renders a {@code sectionCount}-section document to PDF bytes.
     *
     * @param blackhole JMH sink that consumes the rendered bytes
     * @throws Exception if rendering fails
     */
    @Benchmark
    public void renderMultiPageDocument(Blackhole blackhole) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(36))
                .create()) {
            var flow = document.pageFlow().name("Paginated").spacing(8);
            for (int i = 0; i < sectionCount; i++) {
                final int index = i;
                flow.addSection("Section" + index, section -> section
                        .addParagraph("Section " + index)
                        .addParagraph("Body paragraph one for section " + index
                                + " with enough text to occupy a line or two on the page and exercise wrapping.")
                        .addParagraph("Body paragraph two for section " + index
                                + ", continuing the content so pagination has real work to do."));
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
