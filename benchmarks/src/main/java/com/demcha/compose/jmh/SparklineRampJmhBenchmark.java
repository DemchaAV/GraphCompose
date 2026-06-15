package com.demcha.compose.jmh;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
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
 * Strict JMH micro-benchmark: a "sparkline ramp" — a rich paragraph carrying
 * {@code N} inline sparklines — rendered to PDF, parameterized over N so the
 * per-sparkline inline-fragment cost (build + layout + vector draw) is visible.
 * Sparklines were otherwise only exercised once inside
 * {@code MixedShowcaseJmhBenchmark}, where a regression would dilute into the
 * surrounding charts and icons; this isolates and scales them.
 *
 * <pre>
 *   ./mvnw -f benchmarks/pom.xml clean package -DskipTests
 *   java -jar benchmarks/target/benchmarks.jar SparklineRamp
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
public class SparklineRampJmhBenchmark {

    private static final DocumentColor ACCENT = DocumentColor.rgb(20, 80, 95);

    @Param({"8", "32", "128"})
    public int sparklineCount;

    /**
     * Renders a paragraph of {@code sparklineCount} inline sparklines to PDF bytes.
     *
     * @param blackhole JMH sink
     * @throws Exception if rendering fails
     */
    @Benchmark
    public void renderSparklineRamp(Blackhole blackhole) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(28))
                .create()) {
            PageFlowBuilder flow = document.pageFlow().name("SparklineRamp").spacing(4);
            flow.addRich(r -> {
                for (int i = 0; i < sparklineCount; i++) {
                    r.plain("m ").sparkline(42, 9, ACCENT, 65.2, 69.8, 74.1, 81.3, 88.2);
                }
            });
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
